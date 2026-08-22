package com.prsnl.drawing.render

import android.graphics.Canvas
import android.graphics.Paint
import com.prsnl.document.model.Background

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

        linePaint.color = if (isDarkMode) 0x33FFFFFF else 0x22000000
        dotPaint.color = linePaint.color
        textPaint.color = if (isDarkMode) 0x66FFFFFF else 0x66000000

        val spacing = (background.lineSpacing ?: 40f).coerceAtLeast(20f)
        val maxX = maxOf(width * 4f, 4000f)
        val maxY = maxOf(height * 4f, 5000f)

        when (background.type) {
            Background.Type.BLANK -> {
                // Plain paper
            }
            Background.Type.RULED -> {
                var y = spacing + 40f
                while (y < maxY) {
                    canvas.drawLine(-1000f, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.MARGIN_RULED -> {
                // Top header margin & bottom footer margin
                val topMarginY = 120f
                val bottomMarginY = height - 100f

                canvas.drawLine(100f, -1000f, 100f, maxY, marginPaint) // Left red margin
                canvas.drawLine(-1000f, topMarginY, maxX, topMarginY, linePaint)
                canvas.drawLine(-1000f, bottomMarginY, maxX, bottomMarginY, linePaint)

                // Date Box in Top Header Right
                val dateText = "Date: ____ / ____ / 20__"
                canvas.drawText(dateText, width - 260f, 80f, textPaint)

                var y = topMarginY + spacing
                while (y < bottomMarginY) {
                    canvas.drawLine(-1000f, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.GRID -> {
                var x = -1000f
                while (x < maxX) {
                    canvas.drawLine(x, -1000f, x, maxY, linePaint)
                    x += spacing
                }
                var y = -1000f
                while (y < maxY) {
                    canvas.drawLine(-1000f, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.ISOMETRIC -> {
                var x = -1000f
                while (x < maxX) {
                    canvas.drawLine(x, -1000f, x + 3000f, maxY, linePaint)
                    canvas.drawLine(x, -1000f, x - 3000f, maxY, linePaint)
                    x += spacing * 1.5f
                }
            }
            Background.Type.DOTTED -> {
                var x = spacing
                while (x < maxX) {
                    var y = spacing
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
                canvas.drawLine(cueX, -1000f, cueX, summaryY, marginPaint)
                canvas.drawLine(-1000f, summaryY, maxX, summaryY, marginPaint)

                // Labels
                canvas.drawText("CUES / KEYWORDS", 40f, 60f, textPaint)
                canvas.drawText("NOTES", cueX + 30f, 60f, textPaint)
                canvas.drawText("SUMMARY", 40f, summaryY + 40f, textPaint)

                var y = 80f
                while (y < summaryY) {
                    canvas.drawLine(-1000f, y, maxX, y, linePaint)
                    y += spacing
                }
            }
            Background.Type.COLUMN_2 -> {
                val midX = width / 2f
                canvas.drawLine(midX, -1000f, midX, maxY, marginPaint)
                var y = spacing + 40f
                while (y < maxY) {
                    canvas.drawLine(-1000f, y, maxX, y, linePaint)
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
            else -> {}
        }

        // Render Page Number Footer in ALL Formats/Templates
        val pageNumText = "Page ${pageIndex + 1}"
        canvas.drawText(pageNumText, width - 140f, height - 40f, textPaint)
    }
}
