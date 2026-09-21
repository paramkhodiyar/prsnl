package com.prsnl.pdf

import com.prsnl.document.model.Background
import com.prsnl.document.model.Page
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
}
