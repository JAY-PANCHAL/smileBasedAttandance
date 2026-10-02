package com.smileattendance.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smileattendance.app.ml.FaceAnalysisResult
import com.smileattendance.app.ml.SmileFaceAnalyzer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay

/**
 * Live camera preview that continuously runs face+smile detection on incoming frames
 * and reports results via [onFaceResult]. Analysis runs on a dedicated background thread
 * with STRATEGY_KEEP_ONLY_LATEST so slow frames are dropped rather than queued.
 *
 * [lensFacing] can be [CameraSelector.LENS_FACING_FRONT] or [CameraSelector.LENS_FACING_BACK];
 * changing it rebinds the camera use cases to the new lens.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    onFaceResult: (FaceAnalysisResult?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { SmileFaceAnalyzer(onFaceResult) }
    val previewView = remember { PreviewView(context) }

    val boundUseCases = remember { mutableListOf<UseCase>() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analyzer.close()
        }
    }

    // The activity isn't recreated on rotation, so the use cases must be told the new display
    // rotation — otherwise ML Kit receives frames rotated 90°/180° and finds no face.
    DisposableEffect(Unit) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                val rotation = previewView.display?.takeIf { it.displayId == displayId }?.rotation ?: return
                boundUseCases.forEach { useCase ->
                    when (useCase) {
                        is Preview -> useCase.targetRotation = rotation
                        is ImageAnalysis -> useCase.targetRotation = rotation
                    }
                }
            }
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }

    LaunchedEffect(lensFacing) {
        // A kiosk stand runs this for days at a time — a transient bind failure (camera briefly
        // busy right after boot, etc.) shouldn't leave the check-in screen with a dead camera forever.
        var attempt = 0
        while (attempt < MAX_BIND_ATTEMPTS) {
            try {
                val cameraProvider = getCameraProvider(context)

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            analyzer.analyze(imageProxy)
                        }
                    }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                previewView.display?.rotation?.let { rotation ->
                    preview.targetRotation = rotation
                    imageAnalysis.targetRotation = rotation
                }
                boundUseCases.clear()
                boundUseCases.addAll(listOf(preview, imageAnalysis))
                break
            } catch (e: Exception) {
                attempt++
                Log.e(TAG, "Camera bind attempt $attempt failed", e)
                if (attempt < MAX_BIND_ATTEMPTS) delay(BIND_RETRY_DELAY_MS)
            }
        }
    }

    AndroidView(modifier = modifier.fillMaxSize(), factory = { previewView })
}

private const val TAG = "CameraPreview"
private const val MAX_BIND_ATTEMPTS = 3
private const val BIND_RETRY_DELAY_MS = 1500L

private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))
    }
