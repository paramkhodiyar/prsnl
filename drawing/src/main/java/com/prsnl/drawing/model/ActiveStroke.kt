package com.prsnl.drawing.model

import android.graphics.Path
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint

data class ActiveStroke(
    val id: String,
    val color: Int,
    val baseWidth: Float,
    val tool: Stroke.Tool = Stroke.Tool.PEN,
    val brushStyle: Stroke.BrushStyle = Stroke.BrushStyle.ROUND
) {
    private val _points = mutableListOf<StrokePoint>()
    val points: List<StrokePoint>
        get() = synchronized(_points) { _points.toList() }

    val renderPath: Path = Path()
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE

    fun addPoint(point: StrokePoint) {
        synchronized(_points) {
            _points.add(point)
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }
    }

    fun getBoundingBox(padding: Float = baseWidth): RectData {
        synchronized(_points) {
            if (_points.isEmpty()) return RectData(0f, 0f, 0f, 0f)
            return RectData(
                left = minX - padding,
                top = minY - padding,
                right = maxX + padding,
                bottom = maxY + padding
            )
        }
    }

    fun toCommittedStroke(zIndex: Int, createdAt: Long): Stroke {
        val pointsSnapshot = synchronized(_points) { _points.toList() }
        return Stroke(
            id = id,
            zIndex = zIndex,
            boundingBox = getBoundingBox(),
            createdAt = createdAt,
            points = pointsSnapshot,
            color = color,
            baseWidth = baseWidth,
            tool = tool,
            brushStyle = brushStyle
        )
    }
}
