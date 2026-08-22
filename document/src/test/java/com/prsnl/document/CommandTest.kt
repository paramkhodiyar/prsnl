package com.prsnl.document

import com.prsnl.document.model.Background
import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandTest {

    private val initialPage = Page(
        id = "page-1",
        notebookId = "notebook-1",
        index = 0,
        width = 1000f,
        height = 1000f,
        background = Background(
            type = Background.Type.BLANK,
            colorLight = 0xFFFFFFFF.toInt(),
            colorDark = 0xFF000000.toInt()
        )
    )

    private val stroke1 = Stroke(
        id = "s-1",
        zIndex = 0,
        boundingBox = RectData(0f, 0f, 50f, 50f),
        createdAt = 100L,
        points = listOf(StrokePoint(0f, 0f, 0.5f, timestampMs = 0L)),
        color = 0xFF000000.toInt(),
        baseWidth = 2f
    )

    @Test
    fun testAddElementAndUndo() {
        val addCmd = Command.AddElement(stroke1)
        val appliedPage = addCmd.apply(initialPage)

        assertEquals(1, appliedPage.elements.size)
        assertEquals("s-1", appliedPage.elements.first().id)

        val invertedPage = addCmd.invert(appliedPage)
        assertTrue(invertedPage.elements.isEmpty())
    }

    @Test
    fun testDeleteElementAndUndo() {
        val pageWithElement = initialPage.copy(elements = listOf(stroke1))
        val deleteCmd = Command.DeleteElement(stroke1)

        val deletedPage = deleteCmd.apply(pageWithElement)
        assertTrue(deletedPage.elements.isEmpty())

        val restoredPage = deleteCmd.invert(deletedPage)
        assertEquals(1, restoredPage.elements.size)
        assertEquals("s-1", restoredPage.elements.first().id)
    }

    @Test
    fun testReplaceElementAndUndo() {
        val pageWithStroke = initialPage.copy(elements = listOf(stroke1))
        val shape1 = Shape(
            id = "s-1", // same ID as stroke during hold-to-recognize replace
            zIndex = 0,
            boundingBox = RectData(0f, 0f, 50f, 50f),
            createdAt = 110L,
            type = Shape.Type.RECTANGLE,
            strokeColor = 0xFF000000.toInt(),
            strokeWidth = 2f,
            sourceStrokeId = "s-1"
        )

        val replaceCmd = Command.ReplaceElement(stroke1, shape1)
        val replacedPage = replaceCmd.apply(pageWithStroke)
        assertTrue(replacedPage.elements.first() is Shape)

        val restoredPage = replaceCmd.invert(replacedPage)
        assertTrue(restoredPage.elements.first() is Stroke)
    }
}
