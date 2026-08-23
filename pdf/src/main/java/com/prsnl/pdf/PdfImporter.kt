package com.prsnl.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.prsnl.document.model.Background
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfImporter(private val context: Context) {

    fun importPdfToNotebook(pdfFile: File, notebookTitle: String = pdfFile.nameWithoutExtension): Pair<Notebook, List<Page>>? {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return null

        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            val notebookId = UUID.randomUUID().toString()
            val pagesList = mutableListOf<Page>()
            val now = System.currentTimeMillis()

            val storageDir = File(context.filesDir, "pdf_imports/$notebookId")
            if (!storageDir.exists()) storageDir.mkdirs()

            for (i in 0 until pdfRenderer.pageCount) {
                val pdfPage = pdfRenderer.openPage(i)
                val width = pdfPage.width * 2
                val height = pdfPage.height * 2

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfPage.close()

                val pageImgFile = File(storageDir, "page_${i + 1}.png")
                val fos = FileOutputStream(pageImgFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                fos.close()
                bitmap.recycle()

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

            pdfRenderer.close()
            fileDescriptor.close()

            val notebook = Notebook(
                id = notebookId,
                title = notebookTitle.ifBlank { "Imported PDF" },
                createdAt = now,
                updatedAt = now,
                coverColor = 0xFF4C6EF5.toInt(),
                folderName = "PDF Annotations",
                pages = pagesList.map { it.id }
            )

            return Pair(notebook, pagesList)
        } catch (e: Exception) {
            android.util.Log.e("PdfImporter", "Failed to import PDF file: ${pdfFile.name}", e)
            return null
        }
    }
}
