package com.smileattendance.app.ml

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes a face embedding for the `faceData` field the server stores and later hands back —
 * possibly to a different device than the one that enrolled the person. Unlike the purely local
 * Room blob storage (which can use native byte order since it never leaves the device), this
 * fixes little-endian explicitly so any Android device decodes the same bytes the same way.
 */
object EmbeddingCodec {

    fun toBase64(embedding: FloatArray): String {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach { buffer.putFloat(it) }
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    fun fromBase64(encoded: String): FloatArray {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(bytes.size / 4)
        for (i in out.indices) out[i] = buffer.float
        return out
    }
}
