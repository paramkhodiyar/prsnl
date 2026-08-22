package com.prsnl.drawing.selection

import com.prsnl.document.model.Element
import com.prsnl.document.model.StrokePoint

class LassoEngine {

    fun findElementsInLasso(elements: List<Element>, lassoPoints: List<StrokePoint>): List<Element> {
        if (lassoPoints.size < 3) return emptyList()

        val polyX = lassoPoints.map { it.x }
        val polyY = lassoPoints.map { it.y }

        return elements.filter { element ->
            isPointInPolygon(element.boundingBox.centerX, element.boundingBox.centerY, polyX, polyY)
        }
    }

    private fun isPointInPolygon(x: Float, y: Float, px: List<Float>, py: List<Float>): Boolean {
        var isInside = false
        var j = px.size - 1
        for (i in px.indices) {
            if ((py[i] > y) != (py[j] > y) &&
                (x < (px[j] - px[i]) * (y - py[i]) / (py[j] - py[i]) + px[i])
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }
}
