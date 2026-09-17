package com.smileattendance.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrolledUserDao {
    @Insert
    suspend fun insert(user: EnrolledUser): Long

    @Update
    suspend fun update(user: EnrolledUser)

    @Query("SELECT * FROM enrolled_users")
    fun observeAll(): Flow<List<EnrolledUser>>

    @Query("SELECT * FROM enrolled_users WHERE empCode = :empCode LIMIT 1")
    suspend fun getByEmpCode(empCode: Int): EnrolledUser?

    @Query("DELETE FROM enrolled_users WHERE empCode NOT IN (:empCodes)")
    suspend fun deleteMissing(empCodes: List<Int>)

    @Query("DELETE FROM enrolled_users WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insert(record: AttendanceRecord): Long

    @Query("SELECT * FROM attendance_records ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE empCode = :empCode ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLastForEmployee(empCode: Int): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE syncedToServer = 0 ORDER BY timestampMillis ASC")
    suspend fun getUnsynced(): List<AttendanceRecord>

    @Query("UPDATE attendance_records SET syncedToServer = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
