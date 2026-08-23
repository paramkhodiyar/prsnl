package com.prsnl.drawing.selection

import com.prsnl.document.model.Element
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint

class LassoEngine {

    fun findElementsInLasso(elements: List<Element>, lassoPoints: List<StrokePoint>): List<Element> {
        if (lassoPoints.size < 3) return emptyList()

        val polyX = lassoPoints.map { it.x }
        val polyY = lassoPoints.map { it.y }

        return elements.filter { element ->
            when (element) {
                is Stroke -> {
                    val testedPoints = element.points
                    if (testedPoints.isEmpty()) {
                        isPointInPolygon(element.boundingBox.centerX, element.boundingBox.centerY, polyX, polyY)
                    } else {
                        val insideCount = testedPoints.count { point ->
                            isPointInPolygon(point.x, point.y, polyX, polyY)
                        }
                        insideCount >= 2 || insideCount >= (testedPoints.size * 0.35f).toInt().coerceAtLeast(1)
                    }
                }
                else -> {
                    val b = element.boundingBox
                    val probes = listOf(
                        b.centerX to b.centerY,
                        b.left to b.top,
                        b.right to b.top,
                        b.left to b.bottom,
                        b.right to b.bottom
                    )
                    probes.count { (x, y) -> isPointInPolygon(x, y, polyX, polyY) } >= 1
                }
            }
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
