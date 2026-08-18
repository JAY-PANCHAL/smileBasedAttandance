package com.smileattendance.app.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Saves captured face crops to app-private storage (not shared, not on external storage). */
object FileStorage {

    private const val ATTENDANCE_PHOTO_MAX_DIMENSION = 200
    private const val ATTENDANCE_PHOTO_QUALITY = 70

    /** Enrollment reference photos are kept at full quality — this is what every future match compares against. */
    fun saveEnrollmentPhoto(context: Context, bitmap: Bitmap, prefix: String): String =
        save(context, bitmap, prefix, quality = 90)

    /**
     * Check-in/check-out photos are pure audit trail — a small thumbnail is plenty, and at
     * 800+ scans/day keeping these small is what keeps total storage bounded over months of use.
     */
    fun saveAttendancePhoto(context: Context, bitmap: Bitmap, prefix: String): String {
        val scale = ATTENDANCE_PHOTO_MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
        val thumbnail = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        return save(context, thumbnail, prefix, quality = ATTENDANCE_PHOTO_QUALITY)
    }

    /**
     * Deletes check-in/check-out photo files older than [retentionDays]; the attendance DB rows
     * (name, ID, timestamp, type) are tiny and kept forever regardless — only the photo files,
     * which are the actual storage cost, get cleaned up. Enrollment reference photos are never
     * touched since matching depends on them indefinitely.
     */
    fun cleanupOldAttendancePhotos(context: Context, retentionDays: Int = 90) {
        val dir = File(context.filesDir, "faces")
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { file ->
            val isAttendancePhoto = file.name.startsWith("checkin_") || file.name.startsWith("checkout_")
            if (isAttendancePhoto && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private fun save(context: Context, bitmap: Bitmap, prefix: String, quality: Int): String {
        val dir = File(context.filesDir, "faces").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file.absolutePath
    }
}
