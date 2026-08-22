package com.prsnl.storage

import com.prsnl.document.model.RectData
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PageFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndLoadPageElements() {
        val storage = PageFileStorage(tempFolder.root)

        val stroke = Stroke(
            id = "stroke-test-1",
            zIndex = 0,
            boundingBox = RectData(0f, 0f, 100f, 100f),
            createdAt = 1000L,
            points = listOf(StrokePoint(0f, 0f, 0.5f, timestampMs = 0L)),
            color = 0xFF112233.toInt(),
            baseWidth = 3f
        )

        val pageId = "test-page-1"
        val relativePath = storage.savePageElements(pageId, listOf(stroke))

        assertEquals("pages/test-page-1.json", relativePath)

        val loadedElements = storage.loadPageElements(relativePath)
        assertEquals(1, loadedElements.size)
        assertEquals("stroke-test-1", loadedElements.first().id)
        assertEquals(stroke, loadedElements.first())

        val deleted = storage.deletePageElements(relativePath)
        assertTrue(deleted)
        assertTrue(storage.loadPageElements(relativePath).isEmpty())
    }
}
