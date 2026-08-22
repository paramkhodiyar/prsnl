package com.prsnl.drawing.selection

import com.prsnl.document.model.Command
import com.prsnl.document.model.Element
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke

class SelectionHandler {

    var selectedElementId: String? = null
        private set

    private var initialBounds: RectData? = null

    fun selectAt(elements: List<Element>, x: Float, y: Float): Element? {
        val hit = elements.reversed().firstOrNull { element ->
            element.boundingBox.contains(x, y)
        }
        selectedElementId = hit?.id
        initialBounds = hit?.boundingBox
        return hit
    }

    fun clearSelection() {
        selectedElementId = null
        initialBounds = null
    }

    fun createRecolorCommand(element: Element, newColor: Int): Command.ReplaceElement? {
        val updatedElement: Element = when (element) {
            is Shape -> element.copy(strokeColor = newColor)
            is Stroke -> element.copy(color = newColor)
            else -> return null
        }
        return Command.ReplaceElement(oldElement = element, newElement = updatedElement)
    }

    fun createRotateCommand(element: Element, deltaAngleDegrees: Float): Command.ReplaceElement? {
        if (element !is Shape) return null
        val updatedShape = element.copy(rotation = (element.rotation + deltaAngleDegrees) % 360f)
        return Command.ReplaceElement(oldElement = element, newElement = updatedShape)
    }

    fun createMoveCommand(element: Element, dx: Float, dy: Float): Command.MoveElement? {
        val start = initialBounds ?: element.boundingBox
        val target = RectData(
            left = start.left + dx,
            top = start.top + dy,
            right = start.right + dx,
            bottom = start.bottom + dy
        )
        return Command.MoveElement(
            elementId = element.id,
            fromBounds = start,
            toBounds = target
        )
    }

    fun createResizeCommand(element: Element, newWidth: Float, newHeight: Float): Command.ResizeElement? {
        val start = initialBounds ?: element.boundingBox
        val target = RectData(
            left = start.left,
            top = start.top,
            right = start.left + newWidth.coerceAtLeast(10f),
            bottom = start.top + newHeight.coerceAtLeast(10f)
        )
        return Command.ResizeElement(
            elementId = element.id,
            fromBounds = start,
            toBounds = target
        )
    }
}
