package com.smileattendance.app.data.api

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LoginOutcome {
    data class Success(val session: SupervisorSession) : LoginOutcome()
    data class Failure(val message: String) : LoginOutcome()
}

class SupervisorAuthRepository(context: Context, private val credentialsStore: DeviceCredentialsStore) {

    private val sessionStore = SupervisorSessionStore(context)

    private val _session = MutableStateFlow(sessionStore.get())
    val session: StateFlow<SupervisorSession?> = _session.asStateFlow()

    suspend fun login(userName: String, password: String): LoginOutcome {
        val credentials = credentialsStore.get()
            ?: return LoginOutcome.Failure("This device isn't set up yet — pair it first.")

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.login(LoginRequest(userName, password, rememberMe = true))
            val body = response.body()
            if (response.isSuccessful && body?.success == 1 && body.user != null && body.accessToken != null) {
                sessionStore.save(body)
                val saved = sessionStore.get()!!
                _session.value = saved
                LoginOutcome.Success(saved)
            } else {
                LoginOutcome.Failure(body?.error ?: "Sign-in failed (${response.code()}).")
            }
        } catch (e: Exception) {
            LoginOutcome.Failure(networkErrorMessage(e))
        }
    }

    /** Called before an enrollment call; refreshes a stale-but-refreshable session transparently. Returns null if the supervisor must sign in again. */
    suspend fun ensureUsableSession(): SupervisorSession? {
        val current = _session.value ?: return null
        if (current.isAccessTokenUsable()) return current

        val refreshToken = current.refreshToken ?: return null
        val credentials = credentialsStore.get() ?: return null

        return try {
            val api = NetworkModule.api(credentials.baseUrl)
            val response = api.refresh(RefreshRequest(refreshToken))
            val body = response.body()
            if (response.isSuccessful && body?.success == 1 && body.accessToken != null && body.user != null) {
                sessionStore.save(body)
                val saved = sessionStore.get()
                _session.value = saved
                saved
            } else {
                signOut()
                null
            }
        } catch (e: Exception) {
            // A network hiccup during refresh shouldn't sign the supervisor out — just fail this one call.
            null
        }
    }

    fun signOut() {
        sessionStore.clear()
        _session.value = null
    }

    companion object {
        fun networkErrorMessage(e: Exception): String = when (e) {
            is java.net.UnknownHostException -> "Can't reach the server — check the device's network connection."
            is java.net.SocketTimeoutException -> "The server took too long to respond. Try again."
            is java.net.ConnectException -> "Can't connect to the server at this address."
            else -> e.message ?: "Something went wrong talking to the server."
        }
    }
}
