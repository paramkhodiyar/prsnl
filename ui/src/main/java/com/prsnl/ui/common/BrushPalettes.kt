package com.prsnl.ui.common

import com.prsnl.drawing.view.CanvasToolMode

object BrushPalettes {

    // Curated Translucent Neon & Pastel Highlighter Shades (Alpha 0x88)
    val HIGHLIGHTER_COLORS = listOf(
        0x88FFEE00.toInt(), // Neon Yellow
        0x8839FF14.toInt(), // Neon Lime Green
        0x88FF4081.toInt(), // Neon Pink / Rose
        0x8800E5FF.toInt(), // Neon Cyan / Electric Blue
        0x88FF9100.toInt(), // Neon Orange
        0x88C084FC.toInt(), // Pastel Lavender
        0x88FCA5A5.toInt(), // Pastel Peach
        0x8886EFAC.toInt()  // Pastel Mint
    )

    // Opaque Inks for Pens
    val PEN_COLORS = listOf(
        0xFF1E1E1E.toInt(), // Classic Black
        0xFF1D4ED8.toInt(), // Royal Blue
        0xFFDC2626.toInt(), // Crimson Red
        0xFF15803D.toInt(), // Emerald / Forest Green
        0xFFD97706.toInt(), // Amber / Leather
        0xFF7E22CE.toInt()  // Deep Purple
    )

    // Sketch & Graphite Shades for Pencil
    val PENCIL_COLORS = listOf(
        0xFF2D2B28.toInt(), // Charcoal Black
        0xFF525252.toInt(), // 2B Graphite
        0xFF737373.toInt(), // HB Graphite
        0xFFA3A3A3.toInt(), // 2H Light Graphite
        0xFF78350F.toInt(), // Sepia Sketch
        0xFF1E3A5F.toInt()  // Navy Blueprint
    )

    fun getPaletteForTool(toolMode: CanvasToolMode): List<Int> {
        return when (toolMode) {
            CanvasToolMode.HIGHLIGHTER -> HIGHLIGHTER_COLORS
            CanvasToolMode.PENCIL -> PENCIL_COLORS
            else -> PEN_COLORS
        }
    }

    fun getDefaultColorForTool(toolMode: CanvasToolMode): Int {
        return when (toolMode) {
            CanvasToolMode.HIGHLIGHTER -> HIGHLIGHTER_COLORS.first()
            CanvasToolMode.PENCIL -> PENCIL_COLORS.first()
            else -> PEN_COLORS.first()
        }
    }

    fun isColorInPalette(color: Int, toolMode: CanvasToolMode): Boolean {
        return getPaletteForTool(toolMode).contains(color)
    }
}
