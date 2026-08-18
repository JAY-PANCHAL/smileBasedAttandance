package com.smileattendance.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EnrolledUser::class, AttendanceRecord::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(EmbeddingConverter::class, AttendanceTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enrolledUserDao(): EnrolledUserDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smile-attendance.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
