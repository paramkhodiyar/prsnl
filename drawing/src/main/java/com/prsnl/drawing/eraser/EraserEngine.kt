package com.prsnl.drawing.eraser

import com.prsnl.document.model.Command
import com.prsnl.document.model.Element
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import java.util.UUID
import kotlin.math.hypot

enum class EraserMode {
    STROKE_ERASER,
    PIXEL_ERASER
}

class EraserEngine {

    fun eraseAt(
        elements: List<Element>,
        touchX: Float,
        touchY: Float,
        eraserRadius: Float,
        mode: EraserMode
    ): Command? {
        val targetElement = elements.reversed().firstOrNull { element ->
            when (element) {
                is Stroke -> isStrokeHit(element, touchX, touchY, eraserRadius)
                is Shape -> isShapeHit(element, touchX, touchY, eraserRadius)
                else -> false
            }
        } ?: return null

        return when (targetElement) {
            is Shape -> Command.DeleteElement(targetElement)
            is Stroke -> {
                when (mode) {
                    EraserMode.STROKE_ERASER -> Command.DeleteElement(targetElement)
                    EraserMode.PIXEL_ERASER -> splitStrokeAtPoint(targetElement, touchX, touchY, eraserRadius)
                }
            }
            else -> null
        }
    }

    fun isShapeHit(shape: Shape, touchX: Float, touchY: Float, radius: Float): Boolean {
        return shape.boundingBox.expanded(radius).contains(touchX, touchY)
    }

    fun isStrokeHit(stroke: Stroke, touchX: Float, touchY: Float, radius: Float): Boolean {
        val totalRadius = radius + stroke.baseWidth / 2f
        if (!stroke.boundingBox.expanded(totalRadius).contains(touchX, touchY)) {
            return false
        }
        return stroke.points.any { p ->
            hypot(p.x - touchX, p.y - touchY) <= totalRadius
        }
    }

    fun splitStrokeAtPoint(stroke: Stroke, touchX: Float, touchY: Float, radius: Float): Command {
        val totalRadius = radius + stroke.baseWidth / 2f
        val remainingSegments = mutableListOf<MutableList<StrokePoint>>()
        var currentSegment = mutableListOf<StrokePoint>()

        for (point in stroke.points) {
            val distance = hypot(point.x - touchX, point.y - touchY)
            if (distance <= totalRadius) {
                if (currentSegment.isNotEmpty()) {
                    remainingSegments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            } else {
                currentSegment.add(point)
            }
        }
        if (currentSegment.isNotEmpty()) {
            remainingSegments.add(currentSegment)
        }

        val commands = mutableListOf<Command>()
        commands.add(Command.DeleteElement(stroke))

        remainingSegments.forEachIndexed { index, points ->
            if (points.isNotEmpty()) {
                val newStrokeId = UUID.randomUUID().toString()
                val minX = points.minOf { it.x } - stroke.baseWidth
                val minY = points.minOf { it.y } - stroke.baseWidth
                val maxX = points.maxOf { it.x } + stroke.baseWidth
                val maxY = points.maxOf { it.y } + stroke.baseWidth

                val newStroke = stroke.copy(
                    id = newStrokeId,
                    boundingBox = RectData(minX, minY, maxX, maxY),
                    points = points,
                    createdAt = System.currentTimeMillis()
                )
                commands.add(Command.AddElement(newStroke))
            }
        }

        return if (commands.size == 1) {
            commands[0]
        } else {
            Command.CompoundCommand(commands)
        }
    }

    private fun RectData.expanded(padding: Float): RectData {
        return RectData(
            left = left - padding,
            top = top - padding,
            right = right + padding,
            bottom = bottom + padding
        )
    }
}
