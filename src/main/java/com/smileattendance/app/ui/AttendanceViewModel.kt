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
import com.smileattendance.app.db.AttendanceRecord
import com.smileattendance.app.db.EnrolledUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application)

    val users: StateFlow<List<EnrolledUser>> = repository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<List<AttendanceRecord>> = repository.observeAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastOutcome = MutableStateFlow<CheckInOutcome?>(null)
    val lastOutcome: StateFlow<CheckInOutcome?> = _lastOutcome.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _livePreviewMatch = MutableStateFlow<Pair<EnrolledUser, Float>?>(null)
    val livePreviewMatch: StateFlow<Pair<EnrolledUser, Float>?> = _livePreviewMatch.asStateFlow()
    private var previewInFlight = false

    fun enroll(name: String, uniqueNumber: String, faceBitmap: Bitmap, onDone: (EnrolledUser) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val user = repository.enroll(name, uniqueNumber, faceBitmap)
                onDone(user)
            } catch (e: Exception) {
                Log.e(TAG, "enroll failed", e)
            } finally {
                _busy.value = false
            }
        }
    }

    /** Runs on every smiling frame from an unattended kiosk camera — must never crash the process. */
    fun checkIn(faceBitmap: Bitmap, smileProbability: Float) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val outcome = repository.checkIn(faceBitmap, smileProbability)
                _lastOutcome.value = outcome
            } catch (e: Exception) {
                Log.e(TAG, "checkIn failed — will retry on next frame", e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearOutcome() {
        _lastOutcome.value = null
    }

    /** Identifies whoever's face is currently framed, for live "Name · ID" feedback while checking in. Skips overlapping calls so frames don't queue up behind slow inference. */
    fun previewRecognize(faceBitmap: Bitmap) {
        if (previewInFlight || _busy.value) return
        previewInFlight = true
        viewModelScope.launch {
            try {
                _livePreviewMatch.value = repository.recognize(faceBitmap)
            } catch (e: Exception) {
                Log.e(TAG, "previewRecognize failed", e)
            } finally {
                previewInFlight = false
            }
        }
    }

    fun clearLivePreview() {
        _livePreviewMatch.value = null
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
    }
}
