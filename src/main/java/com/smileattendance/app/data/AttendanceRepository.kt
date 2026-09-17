package com.smileattendance.app.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.smileattendance.app.data.api.DeviceCredentialsStore
import com.smileattendance.app.data.api.NetworkModule
import com.smileattendance.app.data.api.PunchRequest
import com.smileattendance.app.data.api.RegisterEmployeeRequest
import com.smileattendance.app.data.api.RegisterFaceRequest
import com.smileattendance.app.data.api.SupervisorAuthRepository
import com.smileattendance.app.data.api.SyncAttendanceRequest
import com.smileattendance.app.db.AppDatabase
import com.smileattendance.app.db.AttendanceRecord
import com.smileattendance.app.db.EnrolledUser
import com.smileattendance.app.ml.EmbeddingCodec
import com.smileattendance.app.ml.FaceEmbedder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Result of matching a live face against the enrolled roster. */
sealed class FaceMatchResult {
    data class Matched(val user: EnrolledUser, val score: Float, val margin: Float) : FaceMatchResult()

    /** Best match cleared the similarity threshold, but didn't beat the runner-up by enough to be confident which person it is. */
    data class Ambiguous(val bestScore: Float, val secondScore: Float) : FaceMatchResult()

    data class NoMatch(val bestScore: Float) : FaceMatchResult()
    object NoEnrolledUsers : FaceMatchResult()
}

sealed class CheckInOutcome {
    data class Success(val user: EnrolledUser, val record: AttendanceRecord) : CheckInOutcome()
    data class NoMatch(val bestScore: Float) : CheckInOutcome()
    data class Ambiguous(val bestScore: Float, val secondScore: Float) : CheckInOutcome()
    object NoEnrolledUsers : CheckInOutcome()
}

sealed class EnrollOutcome {
    data class Success(val empCode: Int) : EnrollOutcome()
    object NeedsSupervisor : EnrollOutcome()
    object Forbidden : EnrollOutcome()
    object AlreadyExists : EnrollOutcome()
    data class Error(val message: String) : EnrollOutcome()
}

sealed class EmployeeSyncOutcome {
    data class Success(val employeeCount: Int) : EmployeeSyncOutcome()
    data class Failure(val message: String) : EmployeeSyncOutcome()
}

class AttendanceRepository(
    private val context: Context,
    private val credentialsStore: DeviceCredentialsStore,
    private val authRepository: SupervisorAuthRepository
) {

    private val db = AppDatabase.get(context)
    private val embedder = FaceEmbedder(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val punchTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /**
     * At 800+ scans/day, re-querying and deserializing every enrolled embedding from SQLite on
     * every camera frame (recognize() runs continuously for the live preview, not just on
     * check-in) would mean constant disk I/O. Instead we keep one reactive in-memory copy,
     * updated automatically whenever the local employee cache changes (i.e. after a sync).
     */
    private val cachedUsers: StateFlow<List<EnrolledUser>> = db.enrolledUserDao().observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    init {
        repositoryScope.launch { FileStorage.cleanupOldAttendancePhotos(context) }
        repositoryScope.launch { trySyncPendingAttendance() }
    }

    fun observeUsers(): Flow<List<EnrolledUser>> = db.enrolledUserDao().observeAll()
    fun observeAttendance(): Flow<List<AttendanceRecord>> = db.attendanceDao().observeAll()

    // ---- Employee roster sync ----

    /** Pulls the current employee roster (with face templates) from the server and mirrors it locally for offline matching. */
    suspend fun syncEmployeesFromServer(): EmployeeSyncOutcome {
        val credentials = credentialsStore.get()
            ?: return EmployeeSyncOutcome.Failure("This device isn't paired yet.")

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.getEmployees(credentials.deviceCode, credentials.deviceKey)
            val body = response.body()
            if (!response.isSuccessful || body?.success != 1 || body.employees == null) {
                return EmployeeSyncOutcome.Failure(body?.error ?: "Sync failed (${response.code()}).")
            }

            val serverEmployees = body.employees.filter { !it.faceData.isNullOrBlank() }
            val dao = db.enrolledUserDao()

            for (employee in serverEmployees) {
                val embedding = try {
                    EmbeddingCodec.fromBase64(employee.faceData!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Bad faceData for empCode ${employee.empCode}, skipping", e)
                    continue
                }
                val existing = dao.getByEmpCode(employee.empCode)
                if (existing == null) {
                    dao.insert(
                        EnrolledUser(
                            empCode = employee.empCode,
                            name = employee.empName,
                            hrid = employee.hrid,
                            embedding = embedding,
                            enrolledAtMillis = System.currentTimeMillis(),
                            referencePhotoPath = ""
                        )
                    )
                } else {
                    dao.update(
                        existing.copy(
                            name = employee.empName,
                            hrid = employee.hrid,
                            embedding = embedding
                        )
                    )
                }
            }

            // Anyone no longer returned by the server (locked, unassigned, deleted) stops being recognized here.
            dao.deleteMissing(serverEmployees.map { it.empCode })

            EmployeeSyncOutcome.Success(serverEmployees.size)
        } catch (e: Exception) {
            EmployeeSyncOutcome.Failure(SupervisorAuthRepository.networkErrorMessage(e))
        }
    }

    // ---- Enrollment (requires a signed-in supervisor with the Employee Management right) ----

    suspend fun enrollNewEmployee(name: String, hrid: String, faceBitmap: Bitmap): EnrollOutcome {
        val credentials = credentialsStore.get() ?: return EnrollOutcome.Error("This device isn't paired yet.")
        val session = authRepository.ensureUsableSession() ?: return EnrollOutcome.NeedsSupervisor

        val embedding = embedder.embed(faceBitmap)
        val photoBase64 = FileStorage.bitmapToBase64Jpeg(faceBitmap)

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.registerEmployee(
                credentials.deviceCode,
                credentials.deviceKey,
                "Bearer ${session.accessToken}",
                RegisterEmployeeRequest(
                    empName = name,
                    hrid = hrid,
                    faceModel = FACE_MODEL,
                    faceData = EmbeddingCodec.toBase64(embedding),
                    facePhoto = photoBase64
                )
            )
            when {
                response.code() == 401 -> EnrollOutcome.NeedsSupervisor
                response.code() == 403 -> EnrollOutcome.Forbidden
                response.code() == 409 -> EnrollOutcome.AlreadyExists
                response.isSuccessful && response.body()?.success == 1 && response.body()?.empCode != null -> {
                    val empCode = response.body()!!.empCode!!
                    cacheEnrolledUserLocally(empCode, name, hrid, embedding, faceBitmap)
                    EnrollOutcome.Success(empCode)
                }
                else -> EnrollOutcome.Error(response.body()?.error ?: "Enrollment failed (${response.code()}).")
            }
        } catch (e: Exception) {
            EnrollOutcome.Error(SupervisorAuthRepository.networkErrorMessage(e))
        }
    }

    /** Replaces a person's stored face — for when recognition keeps failing for them, or a model upgrade. */
    suspend fun reEnrollFace(empCode: Int, name: String, hrid: String, faceBitmap: Bitmap): EnrollOutcome {
        val credentials = credentialsStore.get() ?: return EnrollOutcome.Error("This device isn't paired yet.")
        val session = authRepository.ensureUsableSession() ?: return EnrollOutcome.NeedsSupervisor

        val embedding = embedder.embed(faceBitmap)
        val photoBase64 = FileStorage.bitmapToBase64Jpeg(faceBitmap)

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.registerFace(
                credentials.deviceCode,
                credentials.deviceKey,
                "Bearer ${session.accessToken}",
                RegisterFaceRequest(empCode, FACE_MODEL, EmbeddingCodec.toBase64(embedding), photoBase64)
            )
            when {
                response.code() == 401 -> EnrollOutcome.NeedsSupervisor
                response.code() == 403 -> EnrollOutcome.Forbidden
                response.isSuccessful && response.body()?.success == 1 -> {
                    cacheEnrolledUserLocally(empCode, name, hrid, embedding, faceBitmap)
                    EnrollOutcome.Success(empCode)
                }
                else -> EnrollOutcome.Error(response.body()?.error ?: "Re-enrollment failed (${response.code()}).")
            }
        } catch (e: Exception) {
            EnrollOutcome.Error(SupervisorAuthRepository.networkErrorMessage(e))
        }
    }

    private suspend fun cacheEnrolledUserLocally(empCode: Int, name: String, hrid: String, embedding: FloatArray, faceBitmap: Bitmap) {
        val photoPath = FileStorage.saveEnrollmentPhoto(context, faceBitmap, "enroll_${name.replace(" ", "_")}")
        val dao = db.enrolledUserDao()
        val existing = dao.getByEmpCode(empCode)
        if (existing == null) {
            dao.insert(
                EnrolledUser(
                    empCode = empCode,
                    name = name,
                    hrid = hrid,
                    embedding = embedding,
                    enrolledAtMillis = System.currentTimeMillis(),
                    referencePhotoPath = photoPath
                )
            )
        } else {
            dao.update(existing.copy(name = name, hrid = hrid, embedding = embedding, referencePhotoPath = photoPath))
        }
    }

    // ---- Recognition ----

    /** Identifies the face against the cached roster without recording attendance — used for live "who is this" preview. */
    suspend fun recognize(faceBitmap: Bitmap): FaceMatchResult {
        val queryEmbedding = embedder.embed(faceBitmap)
        return strictMatch(queryEmbedding, cachedUsers.value)
    }

    /**
     * Records a punch for an already-identified person. The caller (ViewModel) is responsible
     * for requiring multi-frame consensus before calling this — this function trusts that the
     * match it's given is the final decision, not a single ambiguous frame.
     */
    suspend fun checkIn(faceBitmap: Bitmap, smileProbability: Float): CheckInOutcome {
        val queryEmbedding = embedder.embed(faceBitmap)
        val match = strictMatch(queryEmbedding, cachedUsers.value)

        return when (match) {
            FaceMatchResult.NoEnrolledUsers -> CheckInOutcome.NoEnrolledUsers
            is FaceMatchResult.NoMatch -> CheckInOutcome.NoMatch(match.bestScore)
            is FaceMatchResult.Ambiguous -> CheckInOutcome.Ambiguous(match.bestScore, match.secondScore)
            is FaceMatchResult.Matched -> {
                val photoPath = FileStorage.saveAttendancePhoto(context, faceBitmap, "punch_${match.user.name.replace(" ", "_")}")
                val record = AttendanceRecord(
                    empCode = match.user.empCode,
                    userName = match.user.name,
                    hrid = match.user.hrid,
                    timestampMillis = System.currentTimeMillis(),
                    smileProbability = smileProbability,
                    matchConfidence = match.score,
                    photoPath = photoPath,
                    syncedToServer = false
                )
                db.attendanceDao().insert(record)
                repositoryScope.launch { trySyncPendingAttendance() }
                CheckInOutcome.Success(match.user, record)
            }
        }
    }

    private fun strictMatch(queryEmbedding: FloatArray, allUsers: List<EnrolledUser>): FaceMatchResult {
        if (allUsers.isEmpty()) return FaceMatchResult.NoEnrolledUsers

        val ranked = allUsers
            .map { it to FaceEmbedder.cosineSimilarity(queryEmbedding, it.embedding) }
            .sortedByDescending { it.second }

        val (bestUser, bestScore) = ranked[0]
        if (bestScore < FaceEmbedder.MATCH_THRESHOLD) return FaceMatchResult.NoMatch(bestScore)

        val secondScore = ranked.getOrNull(1)?.second
        if (secondScore != null && bestScore - secondScore < FaceEmbedder.MIN_MATCH_MARGIN) {
            return FaceMatchResult.Ambiguous(bestScore, secondScore)
        }

        return FaceMatchResult.Matched(bestUser, bestScore, bestScore - (secondScore ?: -1f))
    }

    // ---- Attendance sync ----

    /** Uploads whatever punches haven't reached the server yet. Safe to call repeatedly — already-synced punches are skipped, and a re-sent punch is deduped server-side. */
    suspend fun trySyncPendingAttendance(): Int {
        val credentials = credentialsStore.get() ?: return 0
        val dao = db.attendanceDao()
        val unsynced = dao.getUnsynced()
        if (unsynced.isEmpty()) return 0

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.syncAttendance(
                credentials.deviceCode,
                credentials.deviceKey,
                SyncAttendanceRequest(
                    unsynced.map { record ->
                        PunchRequest(
                            empCode = record.empCode,
                            punchedOn = punchTimeFormat.format(record.timestampMillis),
                            smileScore = record.smileProbability
                        )
                    }
                )
            )
            val results = response.body()?.results ?: return 0
            var syncedCount = 0
            for ((record, result) in unsynced.zip(results)) {
                if (result.success == 1) {
                    dao.markSynced(record.id)
                    syncedCount++
                } else {
                    Log.w(TAG, "Punch for empCode ${record.empCode} rejected by server: ${result.error}")
                }
            }
            syncedCount
        } catch (e: Exception) {
            Log.w(TAG, "Attendance sync failed, will retry later", e)
            0
        }
    }

    fun close() {
        embedder.close()
        repositoryScope.cancel()
    }

    companion object {
        private const val TAG = "AttendanceRepository"
        const val FACE_MODEL = "mobilefacenet-v1"
    }
}
