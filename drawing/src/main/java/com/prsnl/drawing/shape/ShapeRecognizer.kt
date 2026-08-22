package com.prsnl.drawing.shape

import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class ShapeRecognizer(
    private val config: ShapeRecognitionConfig = ShapeRecognitionConfig()
) {

    fun recognize(stroke: Stroke): Shape? {
        val points = stroke.points
        if (points.size < 5) return null

        val first = points.first()
        val last = points.last()
        val endDistance = hypot(last.x - first.x, last.y - first.y)
        val isClosed = endDistance < config.closeShapeThreshold

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val width = maxX - minX
        val height = maxY - minY

        // Require minimum dimensions for valid shape recognition
        if (width < 20f && height < 20f) return null

        val boundingBox = RectData(minX, minY, maxX, maxY)

        if (isClosed) {
            val corners = detectCorners(points)
            if (corners == 3) {
                return Shape(
                    id = UUID.randomUUID().toString(),
                    zIndex = stroke.zIndex,
                    boundingBox = boundingBox,
                    createdAt = System.currentTimeMillis(),
                    type = Shape.Type.TRIANGLE,
                    strokeColor = stroke.color,
                    strokeWidth = stroke.baseWidth,
                    sourceStrokeId = stroke.id
                )
            } else if (corners == 4) {
                return Shape(
                    id = UUID.randomUUID().toString(),
                    zIndex = stroke.zIndex,
                    boundingBox = boundingBox,
                    createdAt = System.currentTimeMillis(),
                    type = Shape.Type.RECTANGLE,
                    strokeColor = stroke.color,
                    strokeWidth = stroke.baseWidth,
                    sourceStrokeId = stroke.id
                )
            } else { // Ellipse / Circle candidate
                val isCircleOrEllipse = checkEllipseFit(points, boundingBox)
                if (isCircleOrEllipse) {
                    return Shape(
                        id = UUID.randomUUID().toString(),
                        zIndex = stroke.zIndex,
                        boundingBox = boundingBox,
                        createdAt = System.currentTimeMillis(),
                        type = Shape.Type.ELLIPSE,
                        strokeColor = stroke.color,
                        strokeWidth = stroke.baseWidth,
                        sourceStrokeId = stroke.id
                    )
                }
            }
        } else {
            // Open stroke candidate: Straight Line or Arrow
            if (checkStraightLine(points)) {
                val isArrow = checkArrowhead(points)
                return Shape(
                    id = UUID.randomUUID().toString(),
                    zIndex = stroke.zIndex,
                    boundingBox = boundingBox,
                    createdAt = System.currentTimeMillis(),
                    type = if (isArrow) Shape.Type.ARROW else Shape.Type.LINE,
                    strokeColor = stroke.color,
                    strokeWidth = stroke.baseWidth,
                    sourceStrokeId = stroke.id
                )
            }
        }

        return null
    }

    private fun detectCorners(points: List<StrokePoint>): Int {
        var cornerCount = 0
        val step = (points.size / 12).coerceAtLeast(2)
        for (i in step until points.size - step step step) {
            val p1 = points[i - step]
            val p2 = points[i]
            val p3 = points[i + step]

            val angle1 = atan2(p2.y - p1.y, p2.x - p1.x)
            val angle2 = atan2(p3.y - p2.y, p3.x - p2.x)
            var diff = abs(Math.toDegrees((angle2 - angle1).toDouble())).toFloat()
            if (diff > 180f) diff = 360f - diff

            if (diff > config.cornerAngleThreshold) {
                cornerCount++
            }
        }
        return cornerCount.coerceIn(0, 4)
    }

    private fun checkEllipseFit(points: List<StrokePoint>, box: RectData): Boolean {
        val centerX = (box.left + box.right) / 2f
        val centerY = (box.top + box.bottom) / 2f
        val rx = (box.right - box.left) / 2f
        val ry = (box.bottom - box.top) / 2f

        if (rx <= 0f || ry <= 0f) return false

        var totalDev = 0f
        for (p in points) {
            val dx = (p.x - centerX) / rx
            val dy = (p.y - centerY) / ry
            val dev = abs(dx * dx + dy * dy - 1.0f)
            totalDev += dev
        }
        val avgDev = (totalDev / points.size) * 100f
        return avgDev < config.ellipseDeviationThreshold
    }

    private fun checkStraightLine(points: List<StrokePoint>): Boolean {
        val first = points.first()
        val last = points.last()
        val totalDist = hypot(last.x - first.x, last.y - first.y)
        if (totalDist < 30f) return false

        var maxDev = 0f
        for (p in points) {
            // Distance from point to line equation: Ax + By + C = 0
            val numerator = abs((last.y - first.y) * p.x - (last.x - first.x) * p.y + last.x * first.y - last.y * first.x)
            val dev = numerator / totalDist
            if (dev > maxDev) maxDev = dev
        }
        return maxDev < config.lineResidualThreshold
    }

    private fun checkArrowhead(points: List<StrokePoint>): Boolean {
        if (points.size < 12) return false
        val end = points.last()
        val prev = points[points.size - 4]
        // Check for sharp direction reversal at the end of the stroke (arrowhead fins)
        val mainAngle = atan2(end.y - points.first().y, end.x - points.first().x)
        val endAngle = atan2(end.y - prev.y, end.x - prev.x)
        var diff = abs(Math.toDegrees((endAngle - mainAngle).toDouble())).toFloat()
        if (diff > 180f) diff = 360f - diff
        return diff > 60f
    }
}
