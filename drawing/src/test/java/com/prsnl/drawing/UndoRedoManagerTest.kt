package com.prsnl.drawing

import com.prsnl.document.model.Background
import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.command.UndoRedoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoManagerTest {

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
        id = "stroke-1",
        zIndex = 0,
        boundingBox = RectData(0f, 0f, 10f, 10f),
        createdAt = 100L,
        points = listOf(StrokePoint(0f, 0f, 0.5f, timestampMs = 0L)),
        color = 0xFF000000.toInt(),
        baseWidth = 2f
    )

    @Test
    fun testUndoRedoSequence() {
        val manager = UndoRedoManager(initialPage)

        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        // Execute Add
        val page1 = manager.execute(Command.AddElement(stroke1))
        assertEquals(1, page1.elements.size)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        // Undo
        val undonePage = manager.undo()
        assertTrue(undonePage != null)
        assertEquals(0, undonePage?.elements?.size)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)

        // Redo
        val redonePage = manager.redo()
        assertTrue(redonePage != null)
        assertEquals(1, redonePage?.elements?.size)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
    }
}
