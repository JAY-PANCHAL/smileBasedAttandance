package com.smileattendance.app.camera

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analyzer.close()
        }
    }

    LaunchedEffect(lensFacing) {
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

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(modifier = modifier.fillMaxSize(), factory = { previewView })
}

private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))
    }
