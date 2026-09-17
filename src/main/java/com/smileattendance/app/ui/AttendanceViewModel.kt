package com.smileattendance.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smileattendance.app.data.AttendanceRepository
import com.smileattendance.app.data.CheckInOutcome
import com.smileattendance.app.data.EmployeeSyncOutcome
import com.smileattendance.app.data.EnrollOutcome
import com.smileattendance.app.data.FaceMatchResult
import com.smileattendance.app.data.api.DeviceCredentials
import com.smileattendance.app.data.api.DeviceCredentialsStore
import com.smileattendance.app.data.api.LoginOutcome
import com.smileattendance.app.data.api.NetworkModule
import com.smileattendance.app.data.api.SupervisorAuthRepository
import com.smileattendance.app.data.api.SupervisorSession
import com.smileattendance.app.db.AttendanceRecord
import com.smileattendance.app.db.EnrolledUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialsStore = DeviceCredentialsStore(application)
    private val authRepository = SupervisorAuthRepository(application, credentialsStore)
    private val repository = AttendanceRepository(application, credentialsStore, authRepository)

    val users: StateFlow<List<EnrolledUser>> = repository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<List<AttendanceRecord>> = repository.observeAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supervisorSession: StateFlow<SupervisorSession?> = authRepository.session

    private val _deviceCredentials = MutableStateFlow(credentialsStore.get())
    val deviceCredentials: StateFlow<DeviceCredentials?> = _deviceCredentials.asStateFlow()

    private val _lastOutcome = MutableStateFlow<CheckInOutcome?>(null)
    val lastOutcome: StateFlow<CheckInOutcome?> = _lastOutcome.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _livePreviewMatch = MutableStateFlow<FaceMatchResult?>(null)
    val livePreviewMatch: StateFlow<FaceMatchResult?> = _livePreviewMatch.asStateFlow()
    private var previewInFlight = false

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Multi-frame consensus: the live preview alone is never enough to commit a punch — the same
    // person has to be the clear match across several consecutive good-quality frames first. This
    // is what actually stops a single ambiguous or motion-blurred frame from accepting a match.
    private var consensusEmpCode: Int? = null
    private var consensusCount: Int = 0

    init {
        if (_deviceCredentials.value != null) {
            viewModelScope.launch { repository.syncEmployeesFromServer() }
        }
    }

    // ---- Device pairing ----

    suspend fun testAndSavePairing(baseUrl: String, deviceCode: String, deviceKey: String): String? {
        val trimmedUrl = baseUrl.trim().trimEnd('/')
        return try {
            val api = NetworkModule.api(trimmedUrl)
            val response = api.getEmployees(deviceCode.trim(), deviceKey.trim())
            when {
                response.isSuccessful && response.body()?.success == 1 -> {
                    credentialsStore.save(DeviceCredentials(trimmedUrl, deviceCode.trim(), deviceKey.trim()))
                    _deviceCredentials.value = credentialsStore.get()
                    viewModelScope.launch { repository.syncEmployeesFromServer() }
                    null
                }
                response.code() == 401 -> "Device code or key is wrong — check Device Manager."
                else -> response.body()?.error ?: "Couldn't reach the server (${response.code()})."
            }
        } catch (e: Exception) {
            SupervisorAuthRepository.networkErrorMessage(e)
        }
    }

    fun unpairDevice() {
        credentialsStore.clear()
        authRepository.signOut()
        _deviceCredentials.value = null
    }

    // ---- Supervisor auth ----

    fun signIn(userName: String, password: String, onResult: (LoginOutcome) -> Unit) {
        viewModelScope.launch {
            onResult(authRepository.login(userName, password))
        }
    }

    fun signOut() = authRepository.signOut()

    // ---- Employee sync ----

    fun syncEmployees() {
        if (_syncing.value) return
        _syncing.value = true
        viewModelScope.launch {
            when (val outcome = repository.syncEmployeesFromServer()) {
                is EmployeeSyncOutcome.Success -> _syncMessage.value = "Synced ${outcome.employeeCount} employee(s)."
                is EmployeeSyncOutcome.Failure -> _syncMessage.value = outcome.message
            }
            val pendingSynced = repository.trySyncPendingAttendance()
            if (pendingSynced > 0) {
                _syncMessage.value = (_syncMessage.value ?: "") + " Uploaded $pendingSynced punch(es)."
            }
            _syncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // ---- Enrollment ----

    fun enroll(name: String, hrid: String, faceBitmap: Bitmap, onDone: (EnrollOutcome) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                onDone(repository.enrollNewEmployee(name, hrid, faceBitmap))
            } catch (e: Exception) {
                Log.e(TAG, "enroll failed", e)
                onDone(EnrollOutcome.Error(e.message ?: "Unexpected error"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun reEnroll(empCode: Int, name: String, hrid: String, faceBitmap: Bitmap, onDone: (EnrollOutcome) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                onDone(repository.reEnrollFace(empCode, name, hrid, faceBitmap))
            } catch (e: Exception) {
                Log.e(TAG, "re-enroll failed", e)
                onDone(EnrollOutcome.Error(e.message ?: "Unexpected error"))
            } finally {
                _busy.value = false
            }
        }
    }

    // ---- Check-in ----

    /** Runs on every smiling frame from an unattended kiosk camera — must never crash the process. */
    fun checkIn(faceBitmap: Bitmap, smileProbability: Float) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val outcome = repository.checkIn(faceBitmap, smileProbability)
                _lastOutcome.value = outcome
                resetConsensus()
            } catch (e: Exception) {
                Log.e(TAG, "checkIn failed — will retry on next frame", e)
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Identifies whoever's face is currently framed, for live "Name · ID" feedback and to build
     * the consensus needed before a check-in is allowed to trigger. Skips overlapping calls so
     * frames don't queue up behind slow inference.
     */
    fun previewRecognize(faceBitmap: Bitmap) {
        if (previewInFlight || _busy.value) return
        previewInFlight = true
        viewModelScope.launch {
            try {
                val result = repository.recognize(faceBitmap)
                _livePreviewMatch.value = result
                updateConsensus(result)
            } catch (e: Exception) {
                Log.e(TAG, "previewRecognize failed", e)
            } finally {
                previewInFlight = false
            }
        }
    }

    /** True once the same person has been the clear, unambiguous match across enough consecutive frames to safely commit a punch. */
    fun isReadyToCommit(empCode: Int): Boolean =
        consensusEmpCode == empCode && consensusCount >= CONSENSUS_FRAMES_REQUIRED

    private fun updateConsensus(result: FaceMatchResult) {
        val matchedEmpCode = (result as? FaceMatchResult.Matched)?.user?.empCode
        if (matchedEmpCode != null && matchedEmpCode == consensusEmpCode) {
            consensusCount++
        } else {
            consensusEmpCode = matchedEmpCode
            consensusCount = if (matchedEmpCode != null) 1 else 0
        }
    }

    private fun resetConsensus() {
        consensusEmpCode = null
        consensusCount = 0
    }

    fun clearOutcome() {
        _lastOutcome.value = null
    }

    fun clearLivePreview() {
        _livePreviewMatch.value = null
        resetConsensus()
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AttendanceViewModel(application) as T
        }
    }

    companion object {
        private const val TAG = "AttendanceViewModel"

        /** How many consecutive good-quality frames must agree on the same person before a check-in is allowed to fire. */
        const val CONSENSUS_FRAMES_REQUIRED = 2
    }
}
