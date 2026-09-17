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

/**
 * v3 -> v4: the app moved from fully local (device-only "enrolled users") to backend-integrated
 * (employees identified by a server-issued empCode, synced from the Smile Please API). Every
 * pre-migration row was enrolled purely locally and has no empCode — there's no server record to
 * link it to, so it can't be carried forward. Both tables are recreated with the new schema, and
 * pre-pivot local test data is dropped intentionally rather than migrated. This also drops the
 * check-in/check-out `type` column, since the server has no such concept — every scan is now
 * just a timestamped punch.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS enrolled_users")
        db.execSQL(
            """
            CREATE TABLE enrolled_users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                empCode INTEGER NOT NULL,
                name TEXT NOT NULL,
                hrid TEXT NOT NULL,
                embedding BLOB NOT NULL,
                enrolledAtMillis INTEGER NOT NULL,
                referencePhotoPath TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_enrolled_users_empCode ON enrolled_users(empCode)")

        db.execSQL("DROP TABLE IF EXISTS attendance_records")
        db.execSQL(
            """
            CREATE TABLE attendance_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                empCode INTEGER NOT NULL,
                userName TEXT NOT NULL,
                hrid TEXT NOT NULL,
                timestampMillis INTEGER NOT NULL,
                smileProbability REAL NOT NULL,
                matchConfidence REAL NOT NULL,
                photoPath TEXT NOT NULL,
                syncedToServer INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}
