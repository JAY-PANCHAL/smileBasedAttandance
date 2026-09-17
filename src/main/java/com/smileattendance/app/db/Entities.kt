package com.smileattendance.app.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A person enrolled on this device. [empCode] is the server-issued employee code — the real
 * identity; everything else here is a local cache for offline matching and display.
 */
@Entity(tableName = "enrolled_users", indices = [Index(value = ["empCode"], unique = true)])
@TypeConverters(EmbeddingConverter::class)
data class EnrolledUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empCode: Int,
    val name: String,
    val hrid: String,
    val embedding: FloatArray,
    val enrolledAtMillis: Long,
    val referencePhotoPath: String
) {
    override fun equals(other: Any?): Boolean = other is EnrolledUser && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * One attendance punch. The server has no check-in/check-out concept — this is just a
 * timestamped, smile-verified sighting of a person, queued locally until [syncedToServer].
 */
@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empCode: Int,
    val userName: String,
    val hrid: String,
    val timestampMillis: Long,
    val smileProbability: Float,
    val matchConfidence: Float,
    val photoPath: String,
    val syncedToServer: Boolean = false
)

/** Room can't persist FloatArray natively; store as a packed byte blob. */
class EmbeddingConverter {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.nativeOrder())
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val out = FloatArray(bytes.size / 4)
        for (i in out.indices) out[i] = buffer.float
        return out
    }
}
