package com.smileattendance.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.smileattendance.app.db.AttendanceRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes attendance records to a CSV file (opens directly in Excel/Sheets) and returns a shareable content URI. */
object AttendanceExporter {

    fun exportToCsv(context: Context, records: List<AttendanceRecord>): Uri {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "attendance_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("Name,ID Number,Type,Date,Time,Smile Confidence %,Match Confidence %\n")
            for (record in records) {
                val date = Date(record.timestampMillis)
                writer.append(csvField(record.userName)).append(',')
                writer.append(csvField(record.userUniqueNumber)).append(',')
                writer.append(record.type.name).append(',')
                writer.append(dateFormat.format(date)).append(',')
                writer.append(timeFormat.format(date)).append(',')
                writer.append((record.smileProbability * 100).toInt().toString()).append(',')
                writer.append((record.matchConfidence * 100).toInt().toString()).append('\n')
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun csvField(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
