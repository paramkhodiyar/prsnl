package com.prsnl.document.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("elementType")
sealed class Element {
    abstract val id: String
    abstract val zIndex: Int
    abstract val boundingBox: RectData
    abstract val createdAt: Long
}

@Serializable
data class Stroke(
    override val id: String,
    override val zIndex: Int,
    override val boundingBox: RectData,
    override val createdAt: Long,
    val points: List<StrokePoint>,
    val color: Int,
    val baseWidth: Float,
    val tool: Tool = Tool.PEN,
    val brushStyle: BrushStyle = BrushStyle.ROUND
) : Element() {
    enum class Tool { PEN, HIGHLIGHTER, PENCIL }
    enum class BrushStyle { ROUND, FLAT, MARKER }
}

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float, // 0.0 - 1.0
    val tiltX: Float? = null,
    val tiltY: Float? = null,
    val timestampMs: Long
)

@Serializable
data class Shape(
    override val id: String,
    override val zIndex: Int,
    override val boundingBox: RectData,
    override val createdAt: Long,
    val type: Type,
    val rotation: Float = 0f,
    val strokeColor: Int,
    val strokeWidth: Float,
    val fillColor: Int? = null,
    val sourceStrokeId: String? = null
) : Element() {
    enum class Type {
        LINE,
        ARROW,
        ARROW_DOUBLE,
        CORNER,
        CORNER_ARROW_SINGLE,
        CORNER_ARROW_DOUBLE,
        RECTANGLE,
        ROUNDED_RECTANGLE,
        ELLIPSE,
        PARALLELOGRAM,
        TRIANGLE,
        DIAMOND,
        AXIS_2D,
        QUADRANT_4,
        AXIS_3D
    }
}

@Serializable
data class TextBox(
    override val id: String,
    override val zIndex: Int,
    override val boundingBox: RectData,
    override val createdAt: Long,
    val content: String,
    val fontSize: Float,
    val color: Int,
    val alignment: Alignment = Alignment.START
) : Element() {
    enum class Alignment { START, CENTER, END }
}

@Serializable
data class ImageElement(
    override val id: String,
    override val zIndex: Int,
    override val boundingBox: RectData,
    override val createdAt: Long,
    val assetPath: String,
    val rotation: Float = 0f
) : Element()

@Serializable
data class PdfAnnotationRef(
    override val id: String,
    override val zIndex: Int,
    override val boundingBox: RectData,
    override val createdAt: Long,
    val targetElementId: String
) : Element()
