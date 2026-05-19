package com.rehaby.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rehaby.app.R

/**
 * Lightweight line chart: X = date order, Y = score 0–100.
 */
class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9CA3AF.toInt()
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = ContextCompat.getColor(context, R.color.primary)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x334F46E5
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 11f * density
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private var entries: List<Pair<String, Float>> = emptyList()

    fun setTrendPoints(points: List<Pair<String, Double>>) {
        entries = points
            .sortedBy { it.first }
            .map { it.first to it.second.toFloat().coerceIn(0f, 100f) }
        visibility = if (entries.isEmpty()) GONE else VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val padL = 44f * density
        val padR = 16f * density
        val padT = 16f * density
        val padB = 36f * density
        val w = width.toFloat()
        val h = height.toFloat()
        val chartL = padL
        val chartR = w - padR
        val chartT = padT
        val chartB = h - padB
        val chartW = chartR - chartL
        val chartH = chartB - chartT

        val yMin = 0f
        val yMax = 100f

        fun xAt(i: Int): Float {
            val n = entries.size
            if (n <= 1) return chartL + chartW / 2f
            return chartL + chartW * i / (n - 1).toFloat()
        }

        fun yAt(score: Float): Float {
            val t = (score - yMin) / (yMax - yMin)
            return chartB - t * chartH
        }

        for (g in 0..4) {
            val frac = g / 4f
            val y = chartB - frac * chartH
            canvas.drawLine(chartL, y, chartR, y, gridPaint)
            val label = (yMin + (yMax - yMin) * frac).toInt().toString()
            canvas.drawText(label, 4f * density, y + 4f * density, labelPaint)
        }

        canvas.drawLine(chartL, chartT, chartL, chartB, axisPaint)
        canvas.drawLine(chartL, chartB, chartR, chartB, axisPaint)

        if (entries.size == 1) {
            val (d, s) = entries[0]
            canvas.drawCircle(xAt(0), yAt(s), 6f * density, dotPaint)
            drawXLabel(canvas, shortDate(d), xAt(0), chartB + labelPaint.textSize + 4f * density)
            return
        }

        val path = Path()
        val fillPath = Path()
        entries.forEachIndexed { i, pair ->
            val x = xAt(i)
            val y = yAt(pair.second)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartB)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xAt(entries.lastIndex), chartB)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        entries.forEachIndexed { i, pair ->
            canvas.drawCircle(xAt(i), yAt(pair.second), 4f * density, dotPaint)
        }

        val labelIndices = when (entries.size) {
            in 1..2 -> entries.indices.toList()
            else -> listOf(0, entries.size / 2, entries.lastIndex).distinct().sorted()
        }
        for (idx in labelIndices) {
            drawXLabel(
                canvas,
                shortDate(entries[idx].first),
                xAt(idx),
                chartB + labelPaint.textSize + 4f * density
            )
        }
    }

    private fun drawXLabel(canvas: Canvas, text: String, x: Float, y: Float) {
        val tw = labelPaint.measureText(text)
        canvas.drawText(text, x - tw / 2f, y, labelPaint)
    }

    private fun shortDate(iso: String): String {
        val parts = iso.split("-")
        if (parts.size != 3) return iso
        return "${parts[1]}/${parts[2]}"
    }
}
