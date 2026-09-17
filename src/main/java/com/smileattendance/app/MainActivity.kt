package com.smileattendance.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.smileattendance.app.ui.AppNavHost
import com.smileattendance.app.ui.AttendanceViewModel
import com.smileattendance.app.ui.DevicePairingScreen
import com.smileattendance.app.ui.SplashScreen
import com.smileattendance.app.ui.SupervisorSignInScreen
import com.smileattendance.app.ui.theme.SmileAttendanceTheme
import com.smileattendance.app.ui.theme.Success
import com.smileattendance.app.ui.theme.SuccessContainer
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels {
        AttendanceViewModel.Factory(application)
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupKioskDisplay()
        setContent {
            SmileAttendanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showBrandedSplash by remember { mutableStateOf(true) }
                    var granted by remember { mutableStateOf(hasAllPermissions()) }
                    val deviceCredentials by viewModel.deviceCredentials.collectAsState()
                    val supervisorSession by viewModel.supervisorSession.collectAsState()
                    // After pairing, setup continues straight into supervisor sign-in — once that
                    // succeeds (or the admin backs out and re-pairs), normal kiosk operation begins.
                    var awaitingPostPairSignIn by remember { mutableStateOf(false) }
                    var showRecoveryBanner by remember { mutableStateOf(false) }

                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        granted = result.values.all { it }
                    }

                    LaunchedEffect(showBrandedSplash) {
                        if (!showBrandedSplash && !granted) launcher.launch(requiredPermissions)
                    }

                    // If the previous run crashed, the crash handler leaves a flag before the
                    // process dies — whoever's at the kiosk sees a brief, honest "we recovered"
                    // message instead of the screen just silently reappearing.
                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences(SmileAttendanceApp.PREFS_NAME, MODE_PRIVATE)
                        if (prefs.getBoolean(SmileAttendanceApp.KEY_RECOVERED_FROM_CRASH, false)) {
                            prefs.edit().putBoolean(SmileAttendanceApp.KEY_RECOVERED_FROM_CRASH, false).apply()
                            showRecoveryBanner = true
                            delay(4000)
                            showRecoveryBanner = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            showBrandedSplash -> SplashScreen(onFinished = { showBrandedSplash = false })
                            !granted -> PermissionRationale(onRequest = { launcher.launch(requiredPermissions) })
                            deviceCredentials == null -> DevicePairingScreen(
                                viewModel = viewModel,
                                onPaired = { awaitingPostPairSignIn = true }
                            )
                            awaitingPostPairSignIn && supervisorSession?.canEnroll() != true -> SupervisorSignInScreen(
                                viewModel = viewModel,
                                onSignedIn = { awaitingPostPairSignIn = false },
                                onBack = {
                                    awaitingPostPairSignIn = false
                                    viewModel.unpairDevice()
                                }
                            )
                            else -> AppNavHost(viewModel = viewModel)
                        }

                        AnimatedVisibility(
                            visible = showRecoveryBanner,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SuccessContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success)
                                    Text(
                                        "Recovered from an unexpected issue — continuing normally",
                                        color = Success,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /** This device sits mounted at a gate running the check-in screen unattended — the display must never sleep or lock. */
    private fun setupKioskDisplay() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Filled.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Camera access needed",
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "We use this only to verify your face for attendance.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRequest) {
            Text("Grant Permissions")
        }
    }
}
