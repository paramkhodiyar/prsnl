package com.prsnl.pdf

import com.prsnl.document.model.Background
import com.prsnl.document.model.Page
import com.prsnl.document.model.Shape
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPdfExporterGeneratesValidPdfFile() {
        val page = Page(
            id = "p1",
            notebookId = "nb1",
            index = 0,
            width = 1200f,
            height = 1697f,
            background = Background(
                type = Background.Type.MARGIN_RULED,
                lineSpacing = 40f,
                colorLight = 0xFFFAF8F5.toInt(),
                colorDark = 0xFF1C1C1E.toInt()
            ),
            elements = emptyList()
        )

        val pdfFile = tempFolder.newFile("test_output.pdf")
        val exporter = PdfExporter()
        val success = exporter.exportPagesToPdf(listOf(page), pdfFile)

        assertTrue(success)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0L)
    }

    @Test
    fun testPdfExporterAnnotatedPdfPage() {
        val dummyPdf = tempFolder.newFile("sample_source.pdf")
        dummyPdf.writeText("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n")

        val stroke = com.prsnl.document.model.Stroke(
            id = "s1",
            zIndex = 0,
            boundingBox = com.prsnl.document.model.RectData(100f, 100f, 150f, 150f),
            createdAt = System.currentTimeMillis(),
            points = listOf(
                com.prsnl.document.model.StrokePoint(100f, 100f, 0.5f, timestampMs = 100L),
                com.prsnl.document.model.StrokePoint(150f, 150f, 0.6f, timestampMs = 110L)
            ),
            color = 0xFFDC2626.toInt(),
            baseWidth = 4f,
            tool = com.prsnl.document.model.Stroke.Tool.PEN
        )

        val page = Page(
            id = "pdf_page_1",
            notebookId = "nb_pdf",
            index = 0,
            width = 1200f,
            height = 1697f,
            background = Background(
                type = Background.Type.PDF,
                colorLight = 0xFFFFFFFF.toInt(),
                colorDark = 0xFF1C1C1E.toInt(),
                pdfSourceRef = dummyPdf.absolutePath
            ),
            elements = listOf(stroke)
        )

        val outputFile = tempFolder.newFile("annotated_export.pdf")
        val exporter = PdfExporter()
        val success = exporter.exportPagesToPdf(listOf(page), outputFile)

        assertTrue(success)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
    }

    @Test
    fun testPdfExporterWithShapesAndImages() {
        val dummyImg = tempFolder.newFile("sample_img.png")
        dummyImg.writeBytes(byteArrayOf(1, 2, 3, 4))

        val graphShape = Shape(
            id = "graph1",
            zIndex = 0,
            boundingBox = com.prsnl.document.model.RectData(100f, 100f, 400f, 400f),
            createdAt = System.currentTimeMillis(),
            type = Shape.Type.AXIS_2D,
            strokeColor = 0xFF1D4ED8.toInt(),
            strokeWidth = 3f
        )

        val triangleShape = Shape(
            id = "tri1",
            zIndex = 1,
            boundingBox = com.prsnl.document.model.RectData(450f, 100f, 650f, 300f),
            createdAt = System.currentTimeMillis(),
            type = Shape.Type.TRIANGLE,
            strokeColor = 0xFFDC2626.toInt(),
            strokeWidth = 2f,
            fillColor = 0x33DC2626.toInt()
        )

        val imageElement = com.prsnl.document.model.ImageElement(
            id = "img1",
            zIndex = 2,
            boundingBox = com.prsnl.document.model.RectData(100f, 500f, 300f, 700f),
            createdAt = System.currentTimeMillis(),
            assetPath = dummyImg.absolutePath
        )

        val page = Page(
            id = "page_shapes",
            notebookId = "nb_shapes",
            index = 0,
            width = 1200f,
            height = 1697f,
            background = Background(
                type = Background.Type.GRID,
                lineSpacing = 40f,
                colorLight = 0xFFFAF8F5.toInt(),
                colorDark = 0xFF1C1C1E.toInt()
            ),
            elements = listOf(graphShape, triangleShape, imageElement)
        )

        val outputFile = tempFolder.newFile("shapes_export.pdf")
        val exporter = PdfExporter()
        val success = exporter.exportPagesToPdf(listOf(page), outputFile)

        assertTrue(success)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
    }
}
