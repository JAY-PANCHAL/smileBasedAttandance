package com.smileattendance.app.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Saves captured face crops to app-private storage (not shared, not on external storage). */
object FileStorage {
    fun savePhoto(context: Context, bitmap: Bitmap, prefix: String): String {
        val dir = File(context.filesDir, "faces").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}
