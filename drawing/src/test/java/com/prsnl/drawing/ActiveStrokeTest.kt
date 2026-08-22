package com.prsnl.drawing

import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.model.ActiveStroke
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveStrokeTest {

    @Test
    fun testActiveStrokeBoundingBoxAndConversion() {
        val activeStroke = ActiveStroke(
            id = "active-1",
            color = 0xFF000000.toInt(),
            baseWidth = 4f,
            tool = Stroke.Tool.PEN
        )

        activeStroke.addPoint(StrokePoint(10f, 10f, 0.5f, timestampMs = 0L))
        activeStroke.addPoint(StrokePoint(100f, 200f, 0.8f, timestampMs = 10L))

        val bbox = activeStroke.getBoundingBox(padding = 0f)
        assertEquals(10f, bbox.left, 0.01f)
        assertEquals(10f, bbox.top, 0.01f)
        assertEquals(100f, bbox.right, 0.01f)
        assertEquals(200f, bbox.bottom, 0.01f)

        val committed = activeStroke.toCommittedStroke(zIndex = 0, createdAt = 500L)
        assertEquals("active-1", committed.id)
        assertEquals(2, committed.points.size)
        assertEquals(0, committed.zIndex)
    }
}
