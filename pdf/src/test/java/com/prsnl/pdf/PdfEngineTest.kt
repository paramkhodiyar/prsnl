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
}
