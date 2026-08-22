package com.prsnl.drawing

import com.prsnl.document.model.Command
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.eraser.EraserEngine
import com.prsnl.drawing.eraser.EraserMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EraserEngineTest {

    private val stroke1 = Stroke(
        id = "stroke-line-1",
        zIndex = 0,
        boundingBox = RectData(0f, 0f, 100f, 10f),
        createdAt = 100L,
        points = listOf(
            StrokePoint(0f, 0f, 0.5f, timestampMs = 0L),
            StrokePoint(25f, 0f, 0.5f, timestampMs = 5L),
            StrokePoint(50f, 0f, 0.5f, timestampMs = 10L), // Erase target at x=50
            StrokePoint(75f, 0f, 0.5f, timestampMs = 15L),
            StrokePoint(100f, 0f, 0.5f, timestampMs = 20L)
        ),
        color = 0xFF000000.toInt(),
        baseWidth = 2f
    )

    @Test
    fun testStrokeEraserHit() {
        val engine = EraserEngine()
        val command = engine.eraseAt(
            elements = listOf(stroke1),
            touchX = 50f,
            touchY = 0f,
            eraserRadius = 10f,
            mode = EraserMode.STROKE_ERASER
        )

        assertTrue(command is Command.DeleteElement)
        assertEquals("stroke-line-1", (command as Command.DeleteElement).element.id)
    }

    @Test
    fun testPixelEraserSplitsStrokeIntoTwoSegments() {
        val engine = EraserEngine()
        val command = engine.eraseAt(
            elements = listOf(stroke1),
            touchX = 50f,
            touchY = 0f,
            eraserRadius = 5f,
            mode = EraserMode.PIXEL_ERASER
        )

        assertTrue(command is Command.CompoundCommand)
        val compound = command as Command.CompoundCommand

        // Should contain 1 DeleteElement + 2 AddElement commands for the two remaining stroke pieces
        assertEquals(3, compound.commands.size)
        assertTrue(compound.commands[0] is Command.DeleteElement)
        assertTrue(compound.commands[1] is Command.AddElement)
        assertTrue(compound.commands[2] is Command.AddElement)

        val leftSegment = (compound.commands[1] as Command.AddElement).element as Stroke
        val rightSegment = (compound.commands[2] as Command.AddElement).element as Stroke

        assertEquals(2, leftSegment.points.size) // x=0, x=25
        assertEquals(2, rightSegment.points.size) // x=75, x=100
    }
}
