package com.prsnl.drawing.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.prsnl.document.model.Shape

class ShapeRenderer {

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val path = Path()

    fun renderShape(canvas: Canvas, shape: Shape) {
        strokePaint.color = shape.strokeColor
        strokePaint.strokeWidth = shape.strokeWidth

        val bounds = shape.boundingBox
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f

        // Fill if configured
        val fill = shape.fillColor
        if (fill != null) {
            fillPaint.color = fill
            when (shape.type) {
                Shape.Type.RECTANGLE -> canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fillPaint)
                Shape.Type.ROUNDED_RECTANGLE -> canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, 24f, 24f, fillPaint)
                Shape.Type.ELLIPSE -> canvas.drawOval(bounds.left, bounds.top, bounds.right, bounds.bottom, fillPaint)
                Shape.Type.TRIANGLE -> {
                    path.reset()
                    path.moveTo(centerX, bounds.top)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.lineTo(bounds.right, bounds.bottom)
                    path.close()
                    canvas.drawPath(path, fillPaint)
                }
                Shape.Type.PARALLELOGRAM -> {
                    val skew = (bounds.right - bounds.left) * 0.25f
                    path.reset()
                    path.moveTo(bounds.left + skew, bounds.top)
                    path.lineTo(bounds.right, bounds.top)
                    path.lineTo(bounds.right - skew, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.close()
                    canvas.drawPath(path, fillPaint)
                }
                Shape.Type.DIAMOND -> {
                    path.reset()
                    path.moveTo(centerX, bounds.top)
                    path.lineTo(bounds.right, centerY)
                    path.lineTo(centerX, bounds.bottom)
                    path.lineTo(bounds.left, centerY)
                    path.close()
                    canvas.drawPath(path, fillPaint)
                }
                else -> {}
            }
        }

        // Draw Vector Outline
        when (shape.type) {
            Shape.Type.LINE -> {
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.ARROW -> { // Single Arrow
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.ARROW_DOUBLE -> { // Double Arrow
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.right, bounds.bottom, bounds.left, bounds.top, strokePaint)
            }
            Shape.Type.CORNER -> { // Corner L-Shape
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint)
                canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.CORNER_ARROW_SINGLE -> { // Single Corner Arrow
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint)
                canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.right, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.CORNER_ARROW_DOUBLE -> { // Double Corner Arrow
                canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint)
                canvas.drawLine(bounds.right, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.right, bounds.top, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.right, bounds.top, bounds.left, bounds.top, strokePaint)
            }
            Shape.Type.RECTANGLE -> {
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.ROUNDED_RECTANGLE -> {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, 24f, 24f, strokePaint)
            }
            Shape.Type.ELLIPSE -> {
                canvas.drawOval(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.PARALLELOGRAM -> {
                val skew = (bounds.right - bounds.left) * 0.25f
                path.reset()
                path.moveTo(bounds.left + skew, bounds.top)
                path.lineTo(bounds.right, bounds.top)
                path.lineTo(bounds.right - skew, bounds.bottom)
                path.lineTo(bounds.left, bounds.bottom)
                path.close()
                canvas.drawPath(path, strokePaint)
            }
            Shape.Type.TRIANGLE -> {
                path.reset()
                path.moveTo(centerX, bounds.top)
                path.lineTo(bounds.left, bounds.bottom)
                path.lineTo(bounds.right, bounds.bottom)
                path.close()
                canvas.drawPath(path, strokePaint)
            }
            Shape.Type.DIAMOND -> {
                path.reset()
                path.moveTo(centerX, bounds.top)
                path.lineTo(bounds.right, centerY)
                path.lineTo(centerX, bounds.bottom)
                path.lineTo(bounds.left, centerY)
                path.close()
                canvas.drawPath(path, strokePaint)
            }
            Shape.Type.AXIS_2D -> { // 2-Axis Graph (Quadrant I)
                canvas.drawLine(bounds.left, bounds.bottom, bounds.left, bounds.top, strokePaint)
                canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.bottom, bounds.left, bounds.top, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint)
            }
            Shape.Type.QUADRANT_4 -> { // 4 Quadrants Graph (Full Cartesian Grid)
                canvas.drawLine(bounds.left, centerY, bounds.right, centerY, strokePaint)
                canvas.drawLine(centerX, bounds.top, centerX, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.left, centerY, bounds.right, centerY, strokePaint)
                drawArrowHead(canvas, bounds.right, centerY, bounds.left, centerY, strokePaint)
                drawArrowHead(canvas, centerX, bounds.bottom, centerX, bounds.top, strokePaint)
                drawArrowHead(canvas, centerX, bounds.top, centerX, bounds.bottom, strokePaint)
            }
            Shape.Type.AXIS_3D -> { // 3D 3-Axis Graph
                val originX = bounds.left + 40f
                canvas.drawLine(originX, centerY, originX, bounds.top, strokePaint)
                canvas.drawLine(originX, centerY, bounds.right, centerY, strokePaint)
                canvas.drawLine(originX, centerY, bounds.left, bounds.bottom, strokePaint)
                drawArrowHead(canvas, originX, centerY, originX, bounds.top, strokePaint)
                drawArrowHead(canvas, originX, centerY, bounds.right, centerY, strokePaint)
                drawArrowHead(canvas, originX, centerY, bounds.left, bounds.bottom, strokePaint)
            }
            Shape.Type.NUMBER_LINE -> {
                // Horizontal line with bidirectional arrowheads and evenly spaced tick marks
                val midY = centerY
                canvas.drawLine(bounds.left, midY, bounds.right, midY, strokePaint)
                drawArrowHead(canvas, bounds.right, midY, bounds.left, midY, strokePaint)
                drawArrowHead(canvas, bounds.left, midY, bounds.right, midY, strokePaint)

                // Draw 7-9 equidistant tick marks
                val numTicks = 9
                val step = (bounds.right - bounds.left) / (numTicks + 1)
                for (i in 1..numTicks) {
                    val tx = bounds.left + i * step
                    val isCenter = (i == (numTicks + 1) / 2)
                    val tickHalfHeight = if (isCenter) 14f else 8f
                    canvas.drawLine(tx, midY - tickHalfHeight, tx, midY + tickHalfHeight, strokePaint)
                }
            }
            Shape.Type.POLAR_GRID -> {
                // Concentric circles with cross axes and 45-degree diagonal rays
                val radius = Math.min(bounds.width, bounds.height) / 2f
                val rings = 3
                for (r in 1..rings) {
                    val ringRadius = radius * (r / rings.toFloat())
                    canvas.drawCircle(centerX, centerY, ringRadius, strokePaint)
                }
                // Horizontal and vertical polar axes
                canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, strokePaint)
                canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, strokePaint)
                // 45-degree diagonal guide lines
                val diagOffset = (radius * 0.7071f)
                canvas.drawLine(centerX - diagOffset, centerY - diagOffset, centerX + diagOffset, centerY + diagOffset, strokePaint)
                canvas.drawLine(centerX - diagOffset, centerY + diagOffset, centerX + diagOffset, centerY - diagOffset, strokePaint)
            }
            Shape.Type.GRAPH_PAPER_BG -> {
                // Bounded grid area with 5x5 subgrid
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, strokePaint)
                val gridCols = 5
                val gridRows = 5
                val colStep = bounds.width / gridCols.toFloat()
                val rowStep = bounds.height / gridRows.toFloat()
                val gridPaint = Paint(strokePaint).apply {
                    strokeWidth = (strokePaint.strokeWidth * 0.6f).coerceAtLeast(1f)
                    alpha = (strokePaint.alpha * 0.75f).toInt()
                }
                for (c in 1 until gridCols) {
                    val gx = bounds.left + c * colStep
                    canvas.drawLine(gx, bounds.top, gx, bounds.bottom, gridPaint)
                }
                for (r in 1 until gridRows) {
                    val gy = bounds.top + r * rowStep
                    canvas.drawLine(bounds.left, gy, bounds.right, gy, gridPaint)
                }
            }
            Shape.Type.BAR_CHART_TEMPLATE -> {
                // L-Axis frame
                canvas.drawLine(bounds.left, bounds.bottom, bounds.left, bounds.top, strokePaint)
                canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.bottom, bounds.left, bounds.top, strokePaint)
                drawArrowHead(canvas, bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint)

                // 3 Bar templates with dashed/light outlines
                val barPaint = Paint(strokePaint).apply {
                    strokeWidth = (strokePaint.strokeWidth * 0.8f).coerceAtLeast(1f)
                }
                val availableWidth = bounds.right - bounds.left - 20f
                val barWidth = availableWidth / 5f
                val heights = listOf(0.4f, 0.75f, 0.55f)
                heights.forEachIndexed { idx, hFraction ->
                    val barLeft = bounds.left + 15f + idx * (barWidth * 1.5f)
                    val barTop = bounds.bottom - (bounds.height - 30f) * hFraction
                    canvas.drawRect(barLeft, barTop, barLeft + barWidth, bounds.bottom, barPaint)
                }
            }
            Shape.Type.PIE_CHART_TEMPLATE -> {
                // Circle with radial pie slices
                val radius = Math.min(bounds.width, bounds.height) / 2f
                canvas.drawCircle(centerX, centerY, radius, strokePaint)
                // Center point
                canvas.drawCircle(centerX, centerY, 3f, strokePaint)
                // 4 sector dividers (0 deg, 70 deg, 160 deg, 260 deg)
                val angles = listOf(0.0, 70.0, 160.0, 260.0)
                for (angleDeg in angles) {
                    val rad = Math.toRadians(angleDeg)
                    val endX = (centerX + radius * Math.cos(rad)).toFloat()
                    val endY = (centerY + radius * Math.sin(rad)).toFloat()
                    canvas.drawLine(centerX, centerY, endX, endY, strokePaint)
                }
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, paint: Paint) {
        val angle = Math.atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
        val arrowLen = 18f
        val arrowAngle = Math.toRadians(25.0)

        val x1 = (toX - arrowLen * Math.cos(angle - arrowAngle)).toFloat()
        val y1 = (toY - arrowLen * Math.sin(angle - arrowAngle)).toFloat()
        val x2 = (toX - arrowLen * Math.cos(angle + arrowAngle)).toFloat()
        val y2 = (toY - arrowLen * Math.sin(angle + arrowAngle)).toFloat()

        canvas.drawLine(toX, toY, x1, y1, paint)
        canvas.drawLine(toX, toY, x2, y2, paint)
    }
}
