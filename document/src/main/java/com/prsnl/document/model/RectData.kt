package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class RectData(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val center: Pair<Float, Float> get() = Pair(centerX, centerY)

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    companion object {
        fun fromBounds(left: Float, top: Float, right: Float, bottom: Float): RectData {
            return RectData(left, top, right, bottom)
        }
    }
}
