package com.smileattendance.app.data

import android.content.Context
import android.graphics.Bitmap
import com.smileattendance.app.db.AppDatabase
import com.smileattendance.app.db.AttendanceRecord
import com.smileattendance.app.db.AttendanceType
import com.smileattendance.app.db.EnrolledUser
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

sealed class CheckInOutcome {
    data class Success(
        val user: EnrolledUser,
        val record: AttendanceRecord
    ) : CheckInOutcome()

    data class NoMatch(val bestScore: Float) : CheckInOutcome()
    object NoEnrolledUsers : CheckInOutcome()
}

class AttendanceRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val embedder = FaceEmbedder(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * At 800+ scans/day, re-querying and deserializing every enrolled embedding from SQLite on
     * every camera frame (recognize() runs continuously for the live preview, not just on
     * check-in) would mean constant disk I/O. Instead we keep one reactive in-memory copy —
     * 2,000 employees is only ~1.5MB of embeddings — and every lookup just reads this list.
     * Room's Flow re-emits automatically whenever enrolled_users changes, so this stays current
     * without any manual invalidation.
     */
    private val cachedUsers: StateFlow<List<EnrolledUser>> = db.enrolledUserDao().observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    init {
        // Runs once per app start; deletes old check-in/checkout photo thumbnails only —
        // the attendance history rows themselves are kept forever.
        repositoryScope.launch { FileStorage.cleanupOldAttendancePhotos(context) }
    }

    fun observeUsers(): Flow<List<EnrolledUser>> = db.enrolledUserDao().observeAll()
    fun observeAttendance(): Flow<List<AttendanceRecord>> = db.attendanceDao().observeAll()

    suspend fun enroll(name: String, uniqueNumber: String, faceBitmap: Bitmap): EnrolledUser {
        val embedding = embedder.embed(faceBitmap)
        val photoPath = FileStorage.saveEnrollmentPhoto(context, faceBitmap, "enroll_${name.replace(" ", "_")}")
        val user = EnrolledUser(
            name = name,
            uniqueNumber = uniqueNumber,
            embedding = embedding,
            enrolledAtMillis = System.currentTimeMillis(),
            referencePhotoPath = photoPath
        )
        val id = db.enrolledUserDao().insert(user)
        return user.copy(id = id)
    }

    /** Identifies the face against enrolled users without recording attendance. Used for live preview while framing a shot. */
    suspend fun recognize(faceBitmap: Bitmap): Pair<EnrolledUser, Float>? {
        val allUsers = cachedUsers.value
        if (allUsers.isEmpty()) return null

        val queryEmbedding = embedder.embed(faceBitmap)
        val best = allUsers
            .map { it to FaceEmbedder.cosineSimilarity(queryEmbedding, it.embedding) }
            .maxByOrNull { it.second } ?: return null

        return if (best.second >= FaceEmbedder.MATCH_THRESHOLD) best else null
    }

    /**
     * Embed the smiling face, match against enrolled users, and record a check-in or check-out.
     * Every scan simply flips the person's last state — no time restriction — so someone can
     * check in and out as many times a day as they actually walk through the gate.
     */
    suspend fun checkIn(faceBitmap: Bitmap, smileProbability: Float): CheckInOutcome {
        val allUsers = cachedUsers.value
        if (allUsers.isEmpty()) return CheckInOutcome.NoEnrolledUsers

        val queryEmbedding = embedder.embed(faceBitmap)
        val ranked = allUsers
            .map { it to FaceEmbedder.cosineSimilarity(queryEmbedding, it.embedding) }
            .sortedByDescending { it.second }

        val (bestUser, bestScore) = ranked.first()
        if (bestScore < FaceEmbedder.MATCH_THRESHOLD) {
            return CheckInOutcome.NoMatch(bestScore)
        }

        val lastRecord = db.attendanceDao().getLastForUser(bestUser.id)
        val type = if (lastRecord == null || lastRecord.type == AttendanceType.CHECK_OUT) {
            AttendanceType.CHECK_IN
        } else {
            AttendanceType.CHECK_OUT
        }

        val prefix = if (type == AttendanceType.CHECK_IN) "checkin" else "checkout"
        val photoPath = FileStorage.saveAttendancePhoto(context, faceBitmap, "${prefix}_${bestUser.name.replace(" ", "_")}")

        val record = AttendanceRecord(
            userId = bestUser.id,
            userName = bestUser.name,
            userUniqueNumber = bestUser.uniqueNumber,
            timestampMillis = System.currentTimeMillis(),
            type = type,
            smileProbability = smileProbability,
            matchConfidence = bestScore,
            photoPath = photoPath
        )

        db.attendanceDao().insert(record)
        return CheckInOutcome.Success(bestUser, record)
    }

    fun close() {
        embedder.close()
        repositoryScope.cancel()
    }
}
