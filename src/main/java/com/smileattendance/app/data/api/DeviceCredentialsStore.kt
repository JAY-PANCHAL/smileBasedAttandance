package com.smileattendance.app.data.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class DeviceCredentials(
    val baseUrl: String,
    val deviceCode: String,
    val deviceKey: String
)

/**
 * The device code + key are this tablet's entire identity to the backend — anyone with them
 * could pull the employee roster or file punches as this device, so they're stored encrypted
 * rather than in plain SharedPreferences.
 */
class DeviceCredentialsStore(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "device_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(): DeviceCredentials? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null) ?: return null
        val deviceCode = prefs.getString(KEY_DEVICE_CODE, null) ?: return null
        val deviceKey = prefs.getString(KEY_DEVICE_KEY, null) ?: return null
        return DeviceCredentials(baseUrl, deviceCode, deviceKey)
    }

    fun save(credentials: DeviceCredentials) {
        prefs.edit()
            .putString(KEY_BASE_URL, credentials.baseUrl)
            .putString(KEY_DEVICE_CODE, credentials.deviceCode)
            .putString(KEY_DEVICE_KEY, credentials.deviceKey)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_DEVICE_CODE = "device_code"
        const val KEY_DEVICE_KEY = "device_key"
    }
}
