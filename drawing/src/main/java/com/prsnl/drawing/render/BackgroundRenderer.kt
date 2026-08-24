package com.prsnl.drawing.render

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.prsnl.document.model.Background
import java.io.File

class BackgroundRenderer {

    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val marginPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFFDC2626.toInt() // Red Margin Line
    }

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 22f
        color = 0xFF71717A.toInt()
    }

    private val pdfBitmapCache = object : android.util.LruCache<String, android.graphics.Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun renderBackground(
        canvas: Canvas,
        background: Background,
        width: Float,
        height: Float,
        pageIndex: Int = 0,
        isDarkMode: Boolean = false
    ) {
        val bgColor = if (isDarkMode) background.colorDark else background.colorLight
        canvas.drawColor(bgColor)

        linePaint.strokeWidth = background.lineWeight.coerceIn(0.25f, 8f)
        linePaint.color = if (isDarkMode) {
            Color.argb((255 * background.lineOpacity.coerceIn(0f, 1f)).toInt(), 255, 255, 255)
        } else {
            Color.argb(
                (255 * background.lineOpacity.coerceIn(0f, 1f)).toInt(),
                Color.red(background.lineColor),
                Color.green(background.lineColor),
                Color.blue(background.lineColor)
            )
        }
        dotPaint.color = linePaint.color
        marginPaint.strokeWidth = background.marginWeight.coerceIn(0.25f, 10f)
        marginPaint.color = background.marginColor
        textPaint.color = if (isDarkMode) 0x66FFFFFF else 0x66000000

        val spacing = (background.lineSpacing ?: 40f).coerceAtLeast(20f)
        val minX = -20000f
        val maxX = 20000f
        val minY = -20000f
        val maxY = 20000f

        when (background.type) {
            Background.Type.BLANK -> {
                // Plain paper
            }
            Background.Type.PDF -> {
                val pdfRef = background.pdfSourceRef
                if (!pdfRef.isNullOrBlank()) {
                    var bitmap = pdfBitmapCache.get(pdfRef)
                    if (bitmap == null || bitmap.isRecycled) {
                        val bgFile = File(pdfRef)
                        if (bgFile.exists()) {
                            try {
                                bitmap = decodePdfPageBitmap(bgFile.absolutePath, width, height)
                                if (bitmap != null) {
                                    pdfBitmapCache.put(pdfRef, bitmap)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BackgroundRenderer", "Error decoding PDF bitmap at $pdfRef", e)
                            }
                        } else {
                            android.util.Log.e("BackgroundRenderer", "PDF background file missing: $pdfRef")
                        }
                    }
                    if (bitmap != null && !bitmap.isRecycled) {
                        canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width, height), null)
                    } else {
                        drawPdfUnavailable(canvas, width, height)
                    }
                } else {
                    android.util.Log.e("BackgroundRenderer", "PDF background missing pdfSourceRef")
                    drawPdfUnavailable(canvas, width, height)
                }
            }
            Background.Type.RULED -> {
                var y = spacing + 40f
                while (y < maxY) {
                    canvas.drawLine(minX, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.MARGIN_RULED -> {
                val topMarginY = 120f
                val bottomMarginY = height - 100f

                canvas.drawLine(100f, minY, 100f, maxY, marginPaint) // Red Left Margin
                canvas.drawLine(minX, topMarginY, maxX, topMarginY, linePaint)
                canvas.drawLine(minX, bottomMarginY, maxX, bottomMarginY, linePaint)

                // Date Section Header
                val dateText = "Date: ____ / ____ / 20__"
                canvas.drawText(dateText, width - 260f, 80f, textPaint)

                var y = topMarginY + spacing
                while (y < bottomMarginY) {
                    canvas.drawLine(minX, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.GRID -> {
                var x = minX
                while (x < maxX) {
                    canvas.drawLine(x, minY, x, maxY, linePaint)
                    x += spacing
                }
                var y = minY
                while (y < maxY) {
                    canvas.drawLine(minX, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.ISOMETRIC -> {
                var x = minX
                while (x < maxX) {
                    canvas.drawLine(x, minY, x + 5000f, maxY, linePaint)
                    canvas.drawLine(x, minY, x - 5000f, maxY, linePaint)
                    x += spacing * 1.5f
                }
            }
            Background.Type.DOTTED -> {
                var x = minX
                while (x < maxX) {
                    var y = minY
                    while (y < maxY) {
                        canvas.drawCircle(x, y, 2.5f, dotPaint)
                        y += spacing
                    }
                    x += spacing
                }
            }
            Background.Type.CORNELL -> {
                val cueX = 260f
                val summaryY = height - 220f
                canvas.drawLine(cueX, minY, cueX, summaryY, marginPaint)
                canvas.drawLine(minX, summaryY, maxX, summaryY, marginPaint)

                // Labels
                canvas.drawText("CUES / KEYWORDS", 40f, 60f, textPaint)
                canvas.drawText("NOTES", cueX + 30f, 60f, textPaint)
                canvas.drawText("SUMMARY", 40f, summaryY + 40f, textPaint)

                var y = 80f
                while (y < summaryY) {
                    canvas.drawLine(minX, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.COLUMN_2 -> {
                val midX = width / 2f
                canvas.drawLine(midX, minY, midX, maxY, marginPaint)
                var y = spacing + 40f
                while (y < maxY) {
                    canvas.drawLine(minX, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.MUSIC -> {
                var y = 140f
                while (y < height - 140f) {
                    for (i in 0..4) {
                        canvas.drawLine(80f, y + i * 16f, width - 80f, y + i * 16f, linePaint)
                    }
                    y += 140f
                }
            }
        }

        // Page Number Footer
        val pageNumText = "Page ${pageIndex + 1}"
        canvas.drawText(pageNumText, width - 140f, height - 40f, textPaint)
    }

    private fun decodePdfPageBitmap(path: String, targetWidth: Float, targetHeight: Float): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDecodeWidth = targetWidth.coerceAtLeast(1f)
        val maxDecodeHeight = targetHeight.coerceAtLeast(1f)
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxDecodeWidth * 1.5f ||
            bounds.outHeight / sampleSize > maxDecodeHeight * 1.5f
        ) {
            sampleSize *= 2
        }

        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
        )
    }

    private fun drawPdfUnavailable(canvas: Canvas, width: Float, height: Float) {
        canvas.drawColor(0xFFFAF8F5.toInt())
        val cardPaint = Paint().apply {
            color = 0xFF475569.toInt()
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PDF Page Markup Canvas", width / 2f, height / 2f, cardPaint)
    }
}
