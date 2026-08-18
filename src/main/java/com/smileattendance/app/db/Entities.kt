package com.smileattendance.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A single enrolled person, identified by a face embedding vector captured at enrollment time. */
@Entity(tableName = "enrolled_users")
@TypeConverters(EmbeddingConverter::class)
data class EnrolledUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uniqueNumber: String,
    val embedding: FloatArray,
    val enrolledAtMillis: Long,
    val referencePhotoPath: String
) {
    override fun equals(other: Any?): Boolean = other is EnrolledUser && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

enum class AttendanceType { CHECK_IN, CHECK_OUT }

/** One attendance event — either the first (check-in) or second (check-out) scan of the day for a person. */
@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val userUniqueNumber: String,
    val timestampMillis: Long,
    val type: AttendanceType,
    val smileProbability: Float,
    val matchConfidence: Float,
    val photoPath: String
)

class AttendanceTypeConverter {
    @TypeConverter
    fun fromType(type: AttendanceType): String = type.name

    @TypeConverter
    fun toType(value: String): AttendanceType = AttendanceType.valueOf(value)
}

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
