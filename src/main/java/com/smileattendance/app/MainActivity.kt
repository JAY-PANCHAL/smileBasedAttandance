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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
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
import com.smileattendance.app.ui.SplashScreen
import com.smileattendance.app.ui.theme.SmileAttendanceTheme

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

                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        granted = result.values.all { it }
                    }

                    LaunchedEffect(showBrandedSplash) {
                        if (!showBrandedSplash && !granted) launcher.launch(requiredPermissions)
                    }

                    when {
                        showBrandedSplash -> SplashScreen(onFinished = { showBrandedSplash = false })
                        granted -> AppNavHost(viewModel = viewModel)
                        else -> PermissionRationale(onRequest = { launcher.launch(requiredPermissions) })
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
