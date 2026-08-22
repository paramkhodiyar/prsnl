package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class Background(
    val type: Type,
    val lineSpacing: Float? = null,
    val colorLight: Int,
    val colorDark: Int,
    val pdfSourceRef: String? = null
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
