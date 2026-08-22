package com.prsnl.drawing

import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.shape.ShapeRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeRecognizerTest {

    private val recognizer = ShapeRecognizer()

    @Test
    fun testStraightLineRecognition() {
        val points = (0..10).map { i ->
            StrokePoint(i * 10f, 0f, 0.5f, timestampMs = i * 5L)
        }
        val lineStroke = Stroke(
            id = "line-1",
            zIndex = 0,
            boundingBox = RectData(0f, 0f, 100f, 0f),
            createdAt = 1000L,
            points = points,
            color = 0xFF000000.toInt(),
            baseWidth = 2f
        )

        val shape = recognizer.recognize(lineStroke)
        assertNotNull(shape)
        assertEquals(Shape.Type.LINE, shape?.type)
    }

    @Test
    fun testMessyScribbleRejection() {
        val messyPoints = listOf(
            StrokePoint(0f, 0f, 0.5f, timestampMs = 0L),
            StrokePoint(50f, 120f, 0.5f, timestampMs = 5L),
            StrokePoint(10f, 20f, 0.5f, timestampMs = 10L),
            StrokePoint(200f, 10f, 0.5f, timestampMs = 15L),
            StrokePoint(5f, 300f, 0.5f, timestampMs = 20L),
            StrokePoint(90f, 40f, 0.5f, timestampMs = 25L)
        )
        val messyStroke = Stroke(
            id = "messy-1",
            zIndex = 0,
            boundingBox = RectData(0f, 0f, 200f, 300f),
            createdAt = 1000L,
            points = messyPoints,
            color = 0xFF000000.toInt(),
            baseWidth = 2f
        )

        val shape = recognizer.recognize(messyStroke)
        assertNull(shape)
    }
}
