package com.rehaby.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rehaby.app.R
import kotlin.math.min

class RingProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress: Int = 0

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
        color = ContextCompat.getColor(context, R.color.card_background)
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
        color = ContextCompat.getColor(context, R.color.primary)
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    fun setProgress(p: Int) {
        progress = p.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val diameter = min(width, height).toFloat()
        val pad = fgPaint.strokeWidth / 2f
        rect.set(pad, pad, diameter - pad, diameter - pad)

        canvas.drawArc(rect, 0f, 360f, false, bgPaint)

        val sweep = 360f * progress / 100f
        canvas.drawArc(rect, -90f, sweep, false, fgPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
