package com.rehaby.app.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object AngleCalculator {
    /**
     * Calculate the angle at point B, formed by the line A→B and C→B.
     * Returns degrees between 0 and 180.
     */
    fun angleBetween(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Float {
        val abX = a.x() - b.x()
        val abY = a.y() - b.y()
        val cbX = c.x() - b.x()
        val cbY = c.y() - b.y()

        val dot = abX * cbX + abY * cbY
        val magAB = sqrt(abX * abX + abY * abY)
        val magCB = sqrt(cbX * cbX + cbY * cbY)

        if (magAB == 0f || magCB == 0f) return 0f

        val cosAngle = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }
}
