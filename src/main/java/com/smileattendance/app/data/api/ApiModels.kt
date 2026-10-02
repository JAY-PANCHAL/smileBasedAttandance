package com.smileattendance.app.data.api

import com.google.gson.annotations.SerializedName

// ---- Connectivity ----

data class HealthResponse(val success: Int, val status: String?)
data class VersionResponse(val success: Int, val version: String?)

// ---- Supervisor auth ----

data class LoginRequest(
    val userName: String,
    val password: String,
    val rememberMe: Boolean
)

data class RefreshRequest(val refreshToken: String)

data class SupervisorUser(
    val code: Int,
    val userName: String,
    val userShort: String?,
    val role: String,
    val userType: Int,
    val lastLoginOn: String?,
    val lastLoginIp: String?
)

data class LoginResponse(
    val success: Int,
    val error: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresInMinutes: Int?,
    val user: SupervisorUser?,
    val menus: List<String>?
)

// ---- Device: employees ----

data class DeviceEmployee(
    val empCode: Int,
    val empName: String,
    val hrid: String,
    val faceModel: String?,
    val faceData: String?
)

data class DeviceEmployeesResponse(
    val success: Int,
    val error: String?,
    val deviceCode: String?,
    val employees: List<DeviceEmployee>?
)

// ---- Device: enroll / re-enroll ----

data class RegisterEmployeeRequest(
    val empName: String,
    val hrid: String,
    val faceModel: String,
    val faceData: String,
    val facePhoto: String?
)

data class RegisterFaceRequest(
    val empCode: Int,
    val faceModel: String,
    val faceData: String,
    val facePhoto: String?
)

data class RegisterEmployeeResponse(
    val success: Int,
    val error: String?,
    val empCode: Int?
)

// ---- Device: attendance sync ----

data class PunchRequest(
    val empCode: Int,
    val punchedOn: String,
    val smileScore: Float?
)

data class SyncAttendanceRequest(val punches: List<PunchRequest>)

data class PunchResult(
    val empCode: Int,
    val punchedOn: String,
    val success: Int,
    val error: String?
)

data class SyncAttendanceResponse(
    val success: Int,
    @SerializedName("error") val topLevelError: String?,
    val results: List<PunchResult>?
)

/** Generic envelope for a plain `{ success, error }` failure body when a call fails on the whole request, not per-item. */
data class GenericErrorResponse(val success: Int, val error: String?)
