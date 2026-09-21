package com.prsnl.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.prsnl.document.model.Background
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

class PdfImporter(private val context: Context) {
    private val maxRenderedPageDimension = 2400

    fun importPdfToNotebook(
        pdfFile: File,
        notebookTitle: String = pdfFile.nameWithoutExtension,
        targetFolderName: String = "General"
    ): Pair<Notebook, List<Page>>? {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return null

        val notebookId = UUID.randomUUID().toString()
        val pagesList = mutableListOf<Page>()
        val now = System.currentTimeMillis()

        val storageDir = File(context.filesDir, "pdf_imports/$notebookId")
        if (!storageDir.exists()) storageDir.mkdirs()

        // 1. Persist original PDF document file directly to storage
        val savedPdfFile = File(storageDir, "document.pdf")
        pdfFile.copyTo(savedPdfFile, overwrite = true)

        var pageCount = 1
        var defaultWidth = 1200f
        var defaultHeight = 1697f

        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = ParcelFileDescriptor.open(savedPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            pageCount = pdfRenderer.pageCount.coerceAtLeast(1)

            for (i in 0 until pageCount) {
                var width = defaultWidth
                var height = defaultHeight
                try {
                    val pdfPage = pdfRenderer.openPage(i)
                    width = pdfPage.width.toFloat().coerceAtLeast(100f)
                    height = pdfPage.height.toFloat().coerceAtLeast(100f)
                    pdfPage.close()
                } catch (_: Exception) {}

                val pageId = UUID.randomUUID().toString()
                val page = Page(
                    id = pageId,
                    notebookId = notebookId,
                    index = i,
                    width = width,
                    height = height,
                    background = Background(
                        type = Background.Type.PDF,
                        colorLight = 0xFFFFFFFF.toInt(),
                        colorDark = 0xFF1C1C1E.toInt(),
                        pdfSourceRef = savedPdfFile.absolutePath
                    ),
                    elements = emptyList()
                )
                pagesList.add(page)
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfImporter", "Failed to query native PdfRenderer for page dimensions; using document defaults", e)
            val pageId = UUID.randomUUID().toString()
            val page = Page(
                id = pageId,
                notebookId = notebookId,
                index = 0,
                width = defaultWidth,
                height = defaultHeight,
                background = Background(
                    type = Background.Type.PDF,
                    colorLight = 0xFFFFFFFF.toInt(),
                    colorDark = 0xFF1C1C1E.toInt(),
                    pdfSourceRef = savedPdfFile.absolutePath
                ),
                elements = emptyList()
            )
            pagesList.add(page)
        } finally {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (_: Exception) {}
        }

        val notebook = Notebook(
            id = notebookId,
            title = notebookTitle.ifBlank { "Imported PDF" },
            createdAt = now,
            updatedAt = now,
            coverColor = 0xFF4C6EF5.toInt(),
            coverStyle = "PDF",
            folderName = targetFolderName.ifBlank { "General" },
            pages = pagesList.map { it.id }
        )

        return Pair(notebook, pagesList)
    }
}

