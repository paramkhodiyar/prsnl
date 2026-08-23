package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class Background(
    val type: Type,
    val lineSpacing: Float? = null,
    val colorLight: Int,
    val colorDark: Int,
    val pdfSourceRef: String? = null,
    val lineWeight: Float = 1.5f,
    val lineOpacity: Float = 0.14f,
    val lineColor: Int = 0xFF000000.toInt(),
    val marginWeight: Float = 2.5f,
    val marginColor: Int = 0xFFDC2626.toInt()
) {
    enum class Type {
        BLANK,
        RULED,
        MARGIN_RULED,
        GRID,
        ISOMETRIC,
        DOTTED,
        CORNELL,
        COLUMN_2,
        MUSIC,
        PDF
    }
}
