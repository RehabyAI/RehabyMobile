package com.rehaby.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class SkeletonOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var imageWidth = 1
    private var imageHeight = 1
    private var incorrectJoints: Set<Int> = emptySet()
    private var mirrorX: Boolean = false

    private val correctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF88")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val incorrectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4444")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val jointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val connections = listOf(
        11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
        11 to 23, 12 to 24, 23 to 24,
        23 to 25, 25 to 27, 24 to 26, 26 to 28
    )

    fun update(
        result: PoseLandmarkerResult,
        imgW: Int,
        imgH: Int,
        badJoints: Set<Int> = emptySet(),
        mirrorOverlayX: Boolean = false
    ) {
        results = result
        imageWidth = imgW
        imageHeight = imgH
        incorrectJoints = badJoints
        mirrorX = mirrorOverlayX
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val landmarks = results?.landmarks()?.firstOrNull() ?: return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        fun nx(x: Float) = if (mirrorX) 1f - x else x

        fun lx(i: Int) = nx(landmarks[i].x()) * imageWidth * scaleX
        fun ly(i: Int) = landmarks[i].y() * imageHeight * scaleY

        for ((a, b) in connections) {
            if (a >= landmarks.size || b >= landmarks.size) continue
            val paint = if (a in incorrectJoints || b in incorrectJoints) incorrectPaint else correctPaint
            canvas.drawLine(lx(a), ly(a), lx(b), ly(b), paint)
        }

        val jointIndices = connections.flatMap { listOf(it.first, it.second) }.toSet()
        for (i in jointIndices) {
            if (i >= landmarks.size) continue
            jointPaint.color = if (i in incorrectJoints) Color.parseColor("#FF4444") else Color.parseColor("#00FF88")
            canvas.drawCircle(lx(i), ly(i), 10f, jointPaint)
        }
    }
}
