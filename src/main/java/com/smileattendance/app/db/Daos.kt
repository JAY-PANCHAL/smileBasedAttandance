package com.smileattendance.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrolledUserDao {
    @Insert
    suspend fun insert(user: EnrolledUser): Long

    @Query("SELECT * FROM enrolled_users")
    fun observeAll(): Flow<List<EnrolledUser>>

    @Query("DELETE FROM enrolled_users WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insert(record: AttendanceRecord): Long

    @Query("SELECT * FROM attendance_records ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLastForUserSince(userId: Long, sinceMillis: Long): AttendanceRecord?
}
