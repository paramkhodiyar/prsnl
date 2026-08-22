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
