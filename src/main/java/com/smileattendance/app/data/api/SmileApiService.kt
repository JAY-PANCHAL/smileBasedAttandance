package com.smileattendance.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SmileApiService {

    @GET("api/health")
    suspend fun health(): Response<HealthResponse>

    @GET("api/version")
    suspend fun version(): Response<VersionResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<LoginResponse>

    @GET("api/device/employees")
    suspend fun getEmployees(
        @Header("X-Device-Code") deviceCode: String,
        @Header("X-Device-Key") deviceKey: String
    ): Response<DeviceEmployeesResponse>

    @POST("api/device/register-employee-full")
    suspend fun registerEmployee(
        @Header("X-Device-Code") deviceCode: String,
        @Header("X-Device-Key") deviceKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: RegisterEmployeeRequest
    ): Response<RegisterEmployeeResponse>

    @POST("api/device/register-face")
    suspend fun registerFace(
        @Header("X-Device-Code") deviceCode: String,
        @Header("X-Device-Key") deviceKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: RegisterFaceRequest
    ): Response<RegisterEmployeeResponse>

    @POST("api/device/sync-attendance")
    suspend fun syncAttendance(
        @Header("X-Device-Code") deviceCode: String,
        @Header("X-Device-Key") deviceKey: String,
        @Body request: SyncAttendanceRequest
    ): Response<SyncAttendanceResponse>
}
