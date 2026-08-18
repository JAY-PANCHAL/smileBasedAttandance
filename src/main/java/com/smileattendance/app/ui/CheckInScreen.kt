@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smileattendance.app.ui

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smileattendance.app.camera.CameraPreview
import com.smileattendance.app.data.CheckInOutcome
import com.smileattendance.app.db.AttendanceType
import com.smileattendance.app.ml.SmileFaceAnalyzer
import com.smileattendance.app.ui.theme.Danger
import com.smileattendance.app.ui.theme.DangerContainer
import com.smileattendance.app.ui.theme.Success
import com.smileattendance.app.ui.theme.SuccessContainer
import com.smileattendance.app.ui.theme.Warning

@Composable
fun CheckInScreen(
    viewModel: AttendanceViewModel,
    onOpenAdminMenu: () -> Unit
) {
    var currentSmileProb by remember { mutableStateOf(0f) }
    var hasTriggered by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    val busy by viewModel.busy.collectAsState()
    val outcome by viewModel.lastOutcome.collectAsState()
    val livePreviewMatch by viewModel.livePreviewMatch.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearOutcome()
        viewModel.clearLivePreview()
    }

    // Kiosk mode: nobody is around to dismiss a result, so always resume scanning on its own.
    LaunchedEffect(outcome) {
        if (outcome != null) {
            delay(3000)
            hasTriggered = false
            viewModel.clearOutcome()
        }
    }

    // If a check-in attempt fails silently (e.g. a transient inference error) busy flips back to
    // false without ever producing an outcome — un-stick the trigger so the next frame can retry.
    LaunchedEffect(busy) {
        if (!busy && outcome == null) {
            hasTriggered = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check In", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenAdminMenu) {
                        Icon(Icons.Filled.Settings, contentDescription = "Admin menu")
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToRoundedCard()
                ) {
                    if (outcome == null) {
                        CameraPreview(lensFacing = lensFacing, onFaceResult = { result ->
                            if (result == null) {
                                currentSmileProb = 0f
                                viewModel.clearLivePreview()
                                return@CameraPreview
                            }
                            currentSmileProb = result.smileProbability
                            viewModel.previewRecognize(result.faceBitmap)
                            if (!hasTriggered && !busy && result.smileProbability >= SmileFaceAnalyzer.SMILE_THRESHOLD) {
                                hasTriggered = true
                                viewModel.checkIn(result.faceBitmap, result.smileProbability)
                            }
                        })
                        FaceGuideOverlay(smileProbability = currentSmileProb)
                        IconButton(
                            onClick = {
                                hasTriggered = false
                                viewModel.clearLivePreview()
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
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            OutcomeIcon(outcome!!)
                        }
                    }
                }

                if (busy) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                AnimatedContent(targetState = outcome, label = "outcome") { current ->
                    if (current == null) {
                        Column {
                            RecognizedPersonRow(livePreviewMatch)
                            Text("Smile to check in", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { currentSmileProb },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clipToRoundedCard(),
                                color = smileColor(currentSmileProb),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Smile confidence ${(currentSmileProb * 100).toInt()}% · need ${(SmileFaceAnalyzer.SMILE_THRESHOLD * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        OutcomeCard(current)
                    }
                }
            }
        }
    }
}

private fun Modifier.clipToRoundedCard(radius: androidx.compose.ui.unit.Dp = 20.dp) =
    this.clip(RoundedCornerShape(radius))

@Composable
private fun RecognizedPersonRow(match: Pair<com.smileattendance.app.db.EnrolledUser, Float>?) {
    if (match == null) return
    val (user, score) = match
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${user.name} · ID ${user.uniqueNumber}",
            style = MaterialTheme.typography.titleMedium,
            color = Success
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "(${(score * 100).toInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            color = Success.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FaceGuideOverlay(smileProbability: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 280.dp)
                .border(3.dp, smileColor(smileProbability), RoundedCornerShape(140.dp))
        )
    }
}

@Composable
private fun smileColor(probability: Float): Color = when {
    probability >= SmileFaceAnalyzer.SMILE_THRESHOLD -> Success
    probability >= 0.35f -> Warning
    else -> Color.White.copy(alpha = 0.8f)
}

@Composable
private fun OutcomeIcon(outcome: CheckInOutcome) {
    val (icon, tint) = when (outcome) {
        is CheckInOutcome.Success -> Icons.Filled.CheckCircle to Success
        is CheckInOutcome.NoMatch -> Icons.Filled.PersonOff to Danger
        CheckInOutcome.NoEnrolledUsers -> Icons.Filled.PersonOff to Danger
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(96.dp))
}

@Composable
private fun OutcomeCard(outcome: CheckInOutcome) {
    val (containerColor, contentColor, title, message) = when (outcome) {
        is CheckInOutcome.Success -> {
            val action = if (outcome.record.type == AttendanceType.CHECK_IN) "Checked in" else "Checked out"
            OutcomeStyle(
                SuccessContainer, Success,
                action,
                "${outcome.user.name} (ID ${outcome.user.uniqueNumber}) · match ${(outcome.record.matchConfidence * 100).toInt()}%"
            )
        }
        is CheckInOutcome.NoMatch -> OutcomeStyle(
            DangerContainer, Danger,
            "Face not recognized",
            "Best match was only ${(outcome.bestScore * 100).toInt()}%. Enroll first, or retry with better lighting."
        )
        CheckInOutcome.NoEnrolledUsers -> OutcomeStyle(
            DangerContainer, Danger,
            "No one enrolled yet",
            "Enroll at least one person before checking in."
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.9f))
            }
        }
    }
}

private data class OutcomeStyle(
    val containerColor: Color,
    val contentColor: Color,
    val title: String,
    val message: String
)
