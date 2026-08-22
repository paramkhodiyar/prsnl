package com.prsnl.drawing.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.model.ActiveStroke

class StrokeRenderer {

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(8f)
    }

    private val pencilGrainPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(4f, 2f), 0f)
    }

    private val strokePath = Path()

    fun renderCommittedStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return

        when (stroke.tool) {
            Stroke.Tool.HIGHLIGHTER -> {
                strokePaint.color = stroke.color
                strokePaint.alpha = 110
                renderUnifiedPath(canvas, stroke.points, stroke.baseWidth, strokePaint)
            }
            Stroke.Tool.PENCIL -> {
                renderPencilStroke(canvas, stroke.points, stroke.color, stroke.baseWidth)
            }
            else -> { // PEN
                strokePaint.color = stroke.color
                strokePaint.alpha = 255
                renderUnifiedPath(canvas, stroke.points, stroke.baseWidth, strokePaint)
            }
        }
    }

    fun renderActiveStroke(canvas: Canvas, activeStroke: ActiveStroke) {
        val pointsSnapshot = activeStroke.points
        if (pointsSnapshot.isEmpty()) return

        when (activeStroke.tool) {
            Stroke.Tool.HIGHLIGHTER -> {
                strokePaint.color = activeStroke.color
                strokePaint.alpha = 110
                renderUnifiedPath(canvas, pointsSnapshot, activeStroke.baseWidth, strokePaint)
            }
            Stroke.Tool.PENCIL -> {
                renderPencilStroke(canvas, pointsSnapshot, activeStroke.color, activeStroke.baseWidth)
            }
            else -> { // PEN
                strokePaint.color = activeStroke.color
                strokePaint.alpha = 255
                renderUnifiedPath(canvas, pointsSnapshot, activeStroke.baseWidth, strokePaint)
            }
        }
    }

    private fun renderUnifiedPath(
        canvas: Canvas,
        points: List<StrokePoint>,
        baseWidth: Float,
        paint: Paint
    ) {
        if (points.isEmpty()) return

        if (points.size == 1) {
            val p = points[0]
            val w = baseWidth * (0.6f + 0.8f * p.pressure)
            paint.strokeWidth = w
            canvas.drawCircle(p.x, p.y, w / 2f, paint)
            return
        }

        // Build continuous smoothed Bezier path to eliminate overlapping end-cap blobs at corners
        strokePath.reset()
        strokePath.moveTo(points[0].x, points[0].y)

        var avgPressure = 0f
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            avgPressure += curr.pressure

            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            strokePath.quadTo(prev.x, prev.y, midX, midY)
        }
        val lastPoint = points.last()
        strokePath.lineTo(lastPoint.x, lastPoint.y)

        // Compute average stroke width across the continuous path
        val meanPressure = if (points.isNotEmpty()) (avgPressure / points.size).coerceIn(0.2f, 1.2f) else 1.0f
        paint.strokeWidth = baseWidth * (0.6f + 0.8f * meanPressure)

        // Draw unified path once to prevent bulky dot overlaps at sharp turns
        canvas.drawPath(strokePath, paint)
    }

    private fun renderPencilStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        color: Int,
        baseWidth: Float
    ) {
        if (points.isEmpty()) return

        pencilGrainPaint.color = color
        val avgP = points.map { it.pressure }.average().toFloat().coerceIn(0.2f, 1.2f)
        pencilGrainPaint.alpha = (160 * (0.6f + 0.5f * avgP)).toInt().coerceIn(80, 220)
        pencilGrainPaint.strokeWidth = baseWidth * (0.7f + 0.5f * avgP)

        strokePath.reset()
        strokePath.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            strokePath.quadTo(prev.x, prev.y, midX, midY)
        }
        strokePath.lineTo(points.last().x, points.last().y)

        canvas.drawPath(strokePath, pencilGrainPaint)
    }
}
