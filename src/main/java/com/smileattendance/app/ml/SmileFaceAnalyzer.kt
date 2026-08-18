package com.smileattendance.app.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

data class FaceAnalysisResult(
    val face: Face,
    val smileProbability: Float,
    val faceBitmap: Bitmap
)

/**
 * Wraps ML Kit's on-device face detector configured for smile classification.
 * Feed it CameraX frames; it reports back the largest detected face plus its smile score
 * and a cropped bitmap ready for embedding.
 */
class SmileFaceAnalyzer(
    private val onResult: (FaceAnalysisResult?) -> Unit
) {
    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // needed for smilingProbability
            .setMinFaceSize(0.3f)
            .enableTracking()
            .build()
    )

    @ExperimentalGetImage
    fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    // This runs on a bad-frame kiosk camera for hours unattended — one malformed
                    // frame (odd YUV stride, corrupt buffer) must not take the whole app down.
                    try {
                        val largest = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        if (largest == null || largest.smilingProbability == null) {
                            onResult(null)
                        } else {
                            val fullBitmap = imageProxy.toBitmap().rotated(rotationDegrees.toFloat())
                            val crop = cropToFace(fullBitmap, largest.boundingBox, fullBitmap.width, fullBitmap.height)
                            onResult(crop?.let { FaceAnalysisResult(largest, largest.smilingProbability!!, it) })
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame post-processing failed, skipping frame", e)
                        onResult(null)
                    }
                }
                .addOnFailureListener { onResult(null) }
                .addOnCompleteListener { imageProxy.close() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit frame for detection, skipping frame", e)
            onResult(null)
            imageProxy.close()
        }
    }

    private fun cropToFace(bitmap: Bitmap, box: Rect, imgWidth: Int, imgHeight: Int): Bitmap? {
        val left = box.left.coerceIn(0, imgWidth - 1)
        val top = box.top.coerceIn(0, imgHeight - 1)
        val right = box.right.coerceIn(left + 1, imgWidth)
        val bottom = box.bottom.coerceIn(top + 1, imgHeight)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun close() = detector.close()

    companion object {
        private const val TAG = "SmileFaceAnalyzer"

        /** How confident ML Kit must be that the person is smiling before we treat it as a valid trigger. */
        const val SMILE_THRESHOLD = 0.75f
    }
}
