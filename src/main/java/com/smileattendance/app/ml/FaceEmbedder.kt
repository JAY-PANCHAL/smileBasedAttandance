package com.smileattendance.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Generates a 192-d face embedding on-device using a bundled MobileFaceNet TFLite model
 * (input 112x112x3, output 192 floats). Embeddings are L2-normalized so cosine similarity
 * reduces to a dot product.
 *
 * This particular model was exported with a fixed batch size of 2 (input [2,112,112,3],
 * output [2,192]) rather than 1 — confirmed via the model's tensor metadata. We fill both
 * batch slots with the same image and only read back the first result.
 */
class FaceEmbedder(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 112
    private val embeddingSize = 192
    private val batchSize = 2

    init {
        val model = loadModelFile(context, "mobile_face_net.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(model, options)
    }

    private fun loadModelFile(context: Context, assetName: String): ByteBuffer {
        val fd = context.assets.openFd(assetName)
        FileInputStream(fd.fileDescriptor).use { stream ->
            val channel = stream.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    /** [faceBitmap] should already be a tightly cropped, upright face (from ML Kit's bounding box). */
    fun embed(faceBitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(faceBitmap, inputSize, inputSize, true)
        val input = bitmapToInputBuffer(resized)
        val output = Array(batchSize) { FloatArray(embeddingSize) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(batchSize * 4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        repeat(batchSize) {
            for (pixel in pixels) {
                // Normalize to [-1, 1], matching the model's training preprocessing.
                buffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 127.5f)
                buffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 127.5f)
                buffer.putFloat(((pixel and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return buffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in vector) sumSq += v * v
        val norm = sqrt(sumSq).coerceAtLeast(1e-8f)
        return FloatArray(vector.size) { vector[it] / norm }
    }

    fun close() = interpreter.close()

    companion object {
        /** Cosine similarity between two L2-normalized embeddings; range [-1, 1], higher = more similar. */
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot
        }

        /** Empirically reasonable threshold for MobileFaceNet cosine similarity on 112x112 crops. */
        const val MATCH_THRESHOLD = 0.6f
    }
}

/** Rotates a bitmap by the given degrees (needed since CameraX frames can arrive rotated). */
fun Bitmap.rotated(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
