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
                        val imageArea = inputImage.width * inputImage.height
                        if (largest == null || largest.smilingProbability == null || !isGoodQuality(largest, imageArea)) {
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

    /**
     * A face embedding computed from a turned head or a distant, tiny face is unreliable input
     * to the matcher — this is often the real root cause of a "confident" wrong match, not the
     * matching math itself. Reject those frames before they ever reach the embedder.
     */
    private fun isGoodQuality(face: Face, imageArea: Int): Boolean {
        val faceArea = face.boundingBox.width() * face.boundingBox.height()
        val areaRatio = faceArea.toFloat() / imageArea.toFloat()
        if (areaRatio < MIN_FACE_AREA_RATIO) return false

        // headEulerAngleY = left/right turn, headEulerAngleZ = tilt. Both are always populated
        // by ML Kit's face detector regardless of classification/landmark mode.
        if (kotlin.math.abs(face.headEulerAngleY) > MAX_HEAD_YAW_DEGREES) return false
        if (kotlin.math.abs(face.headEulerAngleZ) > MAX_HEAD_ROLL_DEGREES) return false

        return true
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
        const val SMILE_THRESHOLD = 0.60f

        /** A face must fill at least this fraction of the frame area — too small/far means a low-quality embedding. */
        const val MIN_FACE_AREA_RATIO = 0.06f

        /** Reject frames where the head is turned or tilted more than this — a side profile embeds unreliably. */
        const val MAX_HEAD_YAW_DEGREES = 18f
        const val MAX_HEAD_ROLL_DEGREES = 18f
    }
}
