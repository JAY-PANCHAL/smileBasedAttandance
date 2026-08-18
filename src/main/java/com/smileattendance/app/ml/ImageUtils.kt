package com.smileattendance.app.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * CameraX 1.3's ImageProxy has no stable public toBitmap(); convert YUV_420_888 -> NV21 -> JPEG -> Bitmap.
 * Not the fastest path, but simple and correct, and this only runs on the (already throttled) analyzer frames.
 */
fun ImageProxy.toBitmap(): Bitmap {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val jpegBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val ySize = yPlane.buffer.remaining()
    val uSize = uPlane.buffer.remaining()
    val vSize = vPlane.buffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yPlane.buffer.get(nv21, 0, ySize)

    val vBuffer = vPlane.buffer
    val uBuffer = uPlane.buffer
    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    // Interleave V and U into the NV21 layout (VU VU VU...), respecting stride.
    var offset = ySize
    val chromaHeight = image.height / 2
    val chromaWidth = image.width / 2
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val vIndex = row * vRowStride + col * vPixelStride
            val uIndex = row * uRowStride + col * uPixelStride
            if (offset + 1 < nv21.size && vIndex < vSize && uIndex < uSize) {
                nv21[offset++] = vBuffer.get(vIndex)
                nv21[offset++] = uBuffer.get(uIndex)
            }
        }
    }
    return nv21
}
