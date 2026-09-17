@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smileattendance.app.ui

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smileattendance.app.camera.CameraPreview
import com.smileattendance.app.data.EnrollOutcome
import com.smileattendance.app.db.EnrolledUser
import com.smileattendance.app.ml.FaceAnalysisResult
import com.smileattendance.app.ui.theme.Danger
import com.smileattendance.app.ui.theme.DangerContainer
import com.smileattendance.app.ui.theme.Success
import com.smileattendance.app.ui.theme.Warning
import com.smileattendance.app.ui.theme.WarningContainer

/** [existingUser] non-null means this is a re-enroll (replacing a face) rather than registering someone new. */
@Composable
fun EnrollScreen(
    viewModel: AttendanceViewModel,
    existingUser: EnrolledUser?,
    onDone: () -> Unit,
    onNeedSignIn: () -> Unit
) {
    val session by viewModel.supervisorSession.collectAsState()

    if (session?.canEnroll() != true) {
        EnrollmentSignInGate(onDone = onDone, onSignIn = onNeedSignIn)
        return
    }

    var name by remember { mutableStateOf(existingUser?.name ?: "") }
    var hrid by remember { mutableStateOf(existingUser?.hrid ?: "") }
    var latestFace by remember { mutableStateOf<FaceAnalysisResult?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val busy by viewModel.busy.collectAsState()
    val isReEnroll = existingUser != null
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReEnroll) "Re-enroll Face" else "Enroll New Person", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
                    CameraPreview(lensFacing = lensFacing, onFaceResult = { latestFace = it })

                    IconButton(
                        onClick = {
                            latestFace = null
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 280.dp)
                                .border(
                                    3.dp,
                                    if (latestFace != null) Success else Color.White.copy(alpha = 0.8f),
                                    RoundedCornerShape(140.dp)
                                )
                        )
                    }

                    if (latestFace != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Success.copy(alpha = 0.9f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Face detected — frontal, well-lit", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    enabled = !isReEnroll,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = hrid,
                    onValueChange = { hrid = it },
                    label = { Text("HR ID") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    singleLine = true,
                    enabled = !isReEnroll,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (errorMessage!!.startsWith("Already")) WarningContainer else DangerContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(
                            errorMessage!!,
                            color = if (errorMessage!!.startsWith("Already")) Warning else Danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val face = latestFace ?: return@Button
                        if (name.isBlank() || hrid.isBlank()) return@Button
                        errorMessage = null
                        val onOutcome: (EnrollOutcome) -> Unit = { outcome ->
                            when (outcome) {
                                is EnrollOutcome.Success -> onDone()
                                EnrollOutcome.NeedsSupervisor -> onNeedSignIn()
                                EnrollOutcome.Forbidden -> errorMessage = "Your role can't enroll employees. Ask an administrator."
                                EnrollOutcome.AlreadyExists -> errorMessage = "Already registered — that HR ID exists. Use Re-enroll instead if their face changed."
                                is EnrollOutcome.Error -> errorMessage = outcome.message
                            }
                        }
                        if (isReEnroll) {
                            viewModel.reEnroll(existingUser!!.empCode, name.trim(), hrid.trim(), face.faceBitmap, onOutcome)
                        } else {
                            viewModel.enroll(name.trim(), hrid.trim(), face.faceBitmap, onOutcome)
                        }
                    },
                    enabled = !busy && latestFace != null && name.isNotBlank() && hrid.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            busy -> "Saving..."
                            isReEnroll -> "Capture & Replace Face"
                            else -> "Capture & Enroll"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrollmentSignInGate(onDone: () -> Unit, onSignIn: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enroll", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Sign in required",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Text(
                "A supervisor must sign in before the camera opens for enrollment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            Button(
                onClick = onSignIn,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text("Sign In", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}
