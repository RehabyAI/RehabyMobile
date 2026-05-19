package com.rehaby.app.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageEncoding {

    fun toJpegBase64(source: Bitmap, maxDimension: Int = 720, quality: Int = 82): String {
        val scaled = scaleDownIfNeeded(source, maxDimension)
        try {
            ByteArrayOutputStream().use { baos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            }
        } finally {
            if (scaled != source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }
}
