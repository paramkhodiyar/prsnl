package com.prsnl.drawing.shape

import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ShapeRecognizerTest {

    private val recognizer = ShapeRecognizer()

    @Test
    fun testRecognizeCircle() {
        val points = mutableListOf<StrokePoint>()
        val centerX = 200f
        val centerY = 200f
        val radius = 80f

        for (i in 0..36) {
            val angle = Math.toRadians((i * 10).toDouble())
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            points.add(StrokePoint(x, y, 1.0f, timestampMs = i * 10L))
        }

        val stroke = Stroke(
            id = "circle-stroke",
            zIndex = 0,
            boundingBox = RectData(120f, 120f, 280f, 280f),
            createdAt = 1000L,
            points = points,
            color = 0xFF000000.toInt(),
            baseWidth = 4f
        )

        val shape = recognizer.recognize(stroke)
        assertNotNull(shape)
        assertEquals(Shape.Type.ELLIPSE, shape?.type)
    }

    @Test
    fun testRecognizeStraightLine() {
        val points = (0..20).map { i ->
            StrokePoint(100f + i * 10f, 100f + i * 5f, 1.0f, timestampMs = i * 10L)
        }

        val stroke = Stroke(
            id = "line-stroke",
            zIndex = 0,
            boundingBox = RectData(100f, 100f, 300f, 200f),
            createdAt = 1000L,
            points = points,
            color = 0xFF000000.toInt(),
            baseWidth = 4f
        )

        val shape = recognizer.recognize(stroke)
        assertNotNull(shape)
        assertEquals(Shape.Type.LINE, shape?.type)
    }

    @Test
    fun testMessyScribbleRejection() {
        val points = listOf(
            StrokePoint(10f, 10f, 1f, timestampMs = 0L),
            StrokePoint(50f, 90f, 1f, timestampMs = 10L),
            StrokePoint(120f, 30f, 1f, timestampMs = 20L),
            StrokePoint(20f, 150f, 1f, timestampMs = 30L)
        )

        val stroke = Stroke(
            id = "scribble-stroke",
            zIndex = 0,
            boundingBox = RectData(10f, 10f, 120f, 150f),
            createdAt = 1000L,
            points = points,
            color = 0xFF000000.toInt(),
            baseWidth = 4f
        )

        val shape = recognizer.recognize(stroke)
        assertNull(shape)
    }
}
