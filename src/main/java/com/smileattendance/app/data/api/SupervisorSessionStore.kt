package com.smileattendance.app.data.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class SupervisorSession(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAtMillis: Long,
    val userName: String,
    val role: String
) {
    /** Refresh a little before the real expiry so a call never starts on a token that dies mid-flight. */
    fun isAccessTokenUsable(): Boolean =
        System.currentTimeMillis() < accessTokenExpiresAtMillis - EXPIRY_SAFETY_MARGIN_MILLIS

    /** Enrollment is restricted to roles that can hold the Employee Management right — everyone else never sees the option. */
    fun canEnroll(): Boolean = role == "SuperAdmin" || role == "Admin"

    private companion object {
        const val EXPIRY_SAFETY_MARGIN_MILLIS = 30_000L
    }
}

/** Supervisor tokens are short-lived credentials for a real person — encrypted at rest, same as the device key. */
class SupervisorSessionStore(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "supervisor_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(): SupervisorSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val userName = prefs.getString(KEY_USER_NAME, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        return SupervisorSession(
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            accessTokenExpiresAtMillis = expiresAt,
            userName = userName,
            role = role
        )
    }

    fun save(response: LoginResponse) {
        val user = requireNotNull(response.user) { "Cannot store a session without a user" }
        val expiresAt = System.currentTimeMillis() + (response.expiresInMinutes ?: 15) * 60_000L
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken ?: prefs.getString(KEY_REFRESH_TOKEN, null))
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USER_NAME, user.userName)
            .putString(KEY_ROLE, user.role)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_NAME = "user_name"
        const val KEY_ROLE = "role"
    }
}
