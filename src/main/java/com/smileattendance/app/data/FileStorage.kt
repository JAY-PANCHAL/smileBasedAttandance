package com.smileattendance.app.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
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
     * Deletes attendance-punch photo files older than [retentionDays]; the attendance DB rows
     * (name, ID, timestamp) are tiny and kept forever regardless — only the photo files, which
     * are the actual storage cost, get cleaned up. Enrollment reference photos are never touched
     * since matching depends on them indefinitely.
     */
    fun cleanupOldAttendancePhotos(context: Context, retentionDays: Int = 90) {
        val dir = File(context.filesDir, "faces")
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("punch_") && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    /**
     * The server's `facePhoto` field is capped at 200KB. Downscales and compresses iteratively
     * until the encoded payload fits, rather than assuming a single quality setting always will.
     */
    fun bitmapToBase64Jpeg(bitmap: Bitmap, maxBytes: Int = 200_000): String? {
        var width = bitmap.width.coerceAtMost(320)
        var quality = 80
        repeat(6) {
            val scale = width.toFloat() / bitmap.width
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, width, (bitmap.height * scale).toInt().coerceAtLeast(1), true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes) {
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            if (quality > 40) quality -= 15 else width = (width * 0.8f).toInt().coerceAtLeast(64)
        }
        return null
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
