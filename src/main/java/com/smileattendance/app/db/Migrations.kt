package com.smileattendance.app.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 -> v3: added [AttendanceRecord.type] to distinguish check-in from check-out.
 * Existing rows predate that distinction, so they're backfilled as CHECK_IN — they were all
 * recorded under the old one-event-per-scan model, which is what CHECK_IN represents here.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN type TEXT NOT NULL DEFAULT 'CHECK_IN'")
    }
}
