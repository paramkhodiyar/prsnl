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

        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            val pageCount = pdfRenderer.pageCount
            if (pageCount == 0) {
                return createEmergencyFallbackNotebook(notebookId, notebookTitle, targetFolderName, storageDir)
            }

            for (i in 0 until pageCount) {
                val pageImgFile = File(storageDir, "page_${i + 1}.png")
                var width = 1200
                var height = 1697

                try {
                    val pdfPage = pdfRenderer.openPage(i)
                    val pageWidth = pdfPage.width.coerceAtLeast(1)
                    val pageHeight = pdfPage.height.coerceAtLeast(1)
                    val scale = min(
                        maxRenderedPageDimension / pageWidth.toFloat(),
                        maxRenderedPageDimension / pageHeight.toFloat()
                    ).coerceAtMost(2f).coerceAtLeast(1f)

                    width = (pageWidth * scale).roundToInt().coerceAtLeast(1)
                    height = (pageHeight * scale).roundToInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    FileOutputStream(pageImgFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    android.util.Log.w("PdfImporter", "Failed to render PDF page ${i + 1}, using fallback canvas", e)
                    createFallbackPageImage(pageImgFile, width, height, i + 1)
                }

                val pageId = UUID.randomUUID().toString()
                val page = Page(
                    id = pageId,
                    notebookId = notebookId,
                    index = i,
                    width = width.toFloat(),
                    height = height.toFloat(),
                    background = Background(
                        type = Background.Type.PDF,
                        colorLight = 0xFFFFFFFF.toInt(),
                        colorDark = 0xFF1C1C1E.toInt(),
                        pdfSourceRef = pageImgFile.absolutePath
                    ),
                    elements = emptyList()
                )
                pagesList.add(page)
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfImporter", "Native PdfRenderer failed to open PDF file: ${pdfFile.name}. Creating failsafe document.", e)
            return createEmergencyFallbackNotebook(notebookId, notebookTitle, targetFolderName, storageDir)
        } finally {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (_: Exception) {}
        }

        if (pagesList.isEmpty()) {
            return createEmergencyFallbackNotebook(notebookId, notebookTitle, targetFolderName, storageDir)
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

    private fun createEmergencyFallbackNotebook(
        notebookId: String,
        title: String,
        folderName: String,
        storageDir: File
    ): Pair<Notebook, List<Page>> {
        val now = System.currentTimeMillis()
        val pageImgFile = File(storageDir, "page_1.png")
        createFallbackPageImage(pageImgFile, 1200, 1697, 1)

        val pageId = UUID.randomUUID().toString()
        val page = Page(
            id = pageId,
            notebookId = notebookId,
            index = 0,
            width = 1200f,
            height = 1697f,
            background = Background(
                type = Background.Type.MARGIN_RULED,
                colorLight = 0xFFFAF8F5.toInt(),
                colorDark = 0xFF1C1C1E.toInt()
            ),
            elements = emptyList()
        )

        val notebook = Notebook(
            id = notebookId,
            title = title.ifBlank { "PDF Note" },
            createdAt = now,
            updatedAt = now,
            coverColor = 0xFF4C6EF5.toInt(),
            coverStyle = "PDF",
            folderName = folderName.ifBlank { "General" },
            pages = listOf(pageId)
        )

        return Pair(notebook, listOf(page))
    }

    private fun createFallbackPageImage(file: File, width: Int, height: Int, pageNum: Int) {
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                color = 0xFF475569.toInt()
                textSize = 32f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("PDF Page $pageNum Markup Canvas", width / 2f, height / 2f, paint)

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos)
            }
            bitmap.recycle()
        } catch (_: Exception) {}
    }
}
