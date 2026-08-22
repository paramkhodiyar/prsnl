package com.prsnl.document

import com.prsnl.document.model.Background
import com.prsnl.document.model.ImageElement
import com.prsnl.document.model.Page
import com.prsnl.document.model.PdfAnnotationRef
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.document.model.TextBox
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializationRoundTripTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testFullPageSerializationRoundTrip() {
        val stroke = Stroke(
            id = "stroke-1",
            zIndex = 0,
            boundingBox = RectData(10f, 10f, 100f, 100f),
            createdAt = 1000L,
            points = listOf(
                StrokePoint(10f, 10f, 0.5f, timestampMs = 0L),
                StrokePoint(50f, 50f, 0.8f, timestampMs = 10L),
                StrokePoint(100f, 100f, 0.3f, timestampMs = 20L)
            ),
            color = 0xFF000000.toInt(),
            baseWidth = 4f,
            tool = Stroke.Tool.PEN,
            brushStyle = Stroke.BrushStyle.ROUND
        )

        val shape = Shape(
            id = "shape-1",
            zIndex = 1,
            boundingBox = RectData(120f, 120f, 300f, 300f),
            createdAt = 1010L,
            type = Shape.Type.RECTANGLE,
            strokeColor = 0xFF0000FF.toInt(),
            strokeWidth = 2f,
            fillColor = 0x220000FF.toInt()
        )

        val textBox = TextBox(
            id = "text-1",
            zIndex = 2,
            boundingBox = RectData(50f, 320f, 250f, 370f),
            createdAt = 1020L,
            content = "Hello prsnl notebook!",
            fontSize = 18f,
            color = 0xFF333333.toInt()
        )

        val image = ImageElement(
            id = "img-1",
            zIndex = 3,
            boundingBox = RectData(0f, 0f, 500f, 500f),
            createdAt = 1030L,
            assetPath = "images/sample.jpg"
        )

        val pdfRef = PdfAnnotationRef(
            id = "pdf-ref-1",
            zIndex = 4,
            boundingBox = RectData(0f, 0f, 100f, 100f),
            createdAt = 1040L,
            targetElementId = "stroke-1"
        )

        val page = Page(
            id = "page-uuid-1",
            notebookId = "notebook-uuid-1",
            index = 0,
            width = 1200f,
            height = 1600f,
            background = Background(
                type = Background.Type.RULED,
                lineSpacing = 40f,
                colorLight = 0xFFFFFFFF.toInt(),
                colorDark = 0xFF121212.toInt()
            ),
            elements = listOf(stroke, shape, textBox, image, pdfRef),
            schemaVersion = 1
        )

        val jsonString = json.encodeToString(page)
        val deserializedPage = json.decodeFromString<Page>(jsonString)

        assertEquals(page.id, deserializedPage.id)
        assertEquals(page.notebookId, deserializedPage.notebookId)
        assertEquals(page.elements.size, deserializedPage.elements.size)
        assertEquals(page, deserializedPage)
    }
}
