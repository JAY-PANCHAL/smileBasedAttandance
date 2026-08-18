@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smileattendance.app.ui

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smileattendance.app.camera.CameraPreview
import com.smileattendance.app.ml.FaceAnalysisResult
import com.smileattendance.app.ui.theme.Success

@Composable
fun EnrollScreen(
    viewModel: AttendanceViewModel,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var uniqueNumber by remember { mutableStateOf("") }
    var latestFace by remember { mutableStateOf<FaceAnalysisResult?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    val busy by viewModel.busy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enroll New Person", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
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
                            Text("Face detected", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uniqueNumber,
                    onValueChange = { uniqueNumber = it },
                    label = { Text("Unique ID number") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val face = latestFace ?: return@Button
                        if (name.isBlank() || uniqueNumber.isBlank()) return@Button
                        viewModel.enroll(name.trim(), uniqueNumber.trim(), face.faceBitmap) { onDone() }
                    },
                    enabled = !busy && latestFace != null && name.isNotBlank() && uniqueNumber.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (busy) "Enrolling..." else "Capture & Enroll")
                }
            }
        }
    }
}
