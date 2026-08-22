package com.prsnl.drawing

import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.drawing.selection.SelectionHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SelectionHandlerTest {

    @Test
    fun testSelectAtHitTesting() {
        val handler = SelectionHandler()

        val shape = Shape(
            id = "shape-target-1",
            zIndex = 0,
            boundingBox = RectData(50f, 50f, 200f, 200f),
            createdAt = 1000L,
            type = Shape.Type.RECTANGLE,
            strokeColor = 0xFF000000.toInt(),
            strokeWidth = 2f
        )

        val selected = handler.selectAt(listOf(shape), 100f, 100f)
        assertNotNull(selected)
        assertEquals("shape-target-1", selected?.id)

        val missed = handler.selectAt(listOf(shape), 300f, 300f)
        assertNull(missed)
    }
}
