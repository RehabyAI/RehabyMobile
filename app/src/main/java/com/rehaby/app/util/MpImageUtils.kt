package com.rehaby.app.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage

object MpImageUtils {

    /**
     * Full camera pipeline bitmap: RGBA from [ImageProxy], rotation, optional mirror.
     * Caller must recycle when done (after building [MPImage] / copies for API).
     */
    fun buildPipelineBitmap(image: ImageProxy, useFrontCamera: Boolean): Bitmap? {
        val bitmap = rawImageProxyToBitmap(image) ?: return null
        val rotation = image.imageInfo.rotationDegrees
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        var rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        if (useFrontCamera) {
            val mirrored = mirrorBitmap(rotated)
            if (mirrored != rotated) {
                rotated.recycle()
            }
            rotated = mirrored
        }
        return rotated
    }

    fun imageProxyToMpImage(image: ImageProxy, useFrontCamera: Boolean): MPImage? {
        val bmp = buildPipelineBitmap(image, useFrontCamera) ?: return null
        return BitmapImageBuilder(bmp).build()
    }

    private fun mirrorBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun rawImageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer.duplicate()
            buffer.rewind()
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val width = image.width
            val height = image.height
            if (pixelStride <= 0) return null

            val rowPadding = rowStride - pixelStride * width
            val bitmapWidth = width + rowPadding / pixelStride

            val bitmap = Bitmap.createBitmap(
                bitmapWidth,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            if (rowPadding == 0) {
                bitmap
            } else {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                bitmap.recycle()
                cropped
            }
        } catch (_: Exception) {
            null
        }
    }
}
