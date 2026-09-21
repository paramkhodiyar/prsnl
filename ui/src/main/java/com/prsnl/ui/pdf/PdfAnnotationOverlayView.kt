package com.prsnl.ui.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.github.barteksc.pdfviewer.PDFView
import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.eraser.EraserEngine
import com.prsnl.drawing.eraser.EraserMode
import com.prsnl.drawing.model.ActiveStroke
import com.prsnl.drawing.render.StrokeRenderer
import com.prsnl.drawing.view.CanvasToolMode
import java.util.UUID

class PdfAnnotationOverlayView(context: Context) : View(context) {

    var pdfView: PDFView? = null
    var pages: List<Page> = emptyList()
    var currentToolMode: CanvasToolMode = CanvasToolMode.PEN
    var selectedColor: Int = Color.BLACK
    var selectedWidth: Float = 4f
    var isFingerDrawingEnabled: Boolean = false
    var onCommandIssued: ((pageIndex: Int, Command) -> Unit)? = null

    private val strokeRenderer = StrokeRenderer()
    private val eraserEngine = EraserEngine()
    private var activeStroke: ActiveStroke? = null
    private var targetPageIndex: Int = 0
    private var strokeStartTime: Long = 0L

    fun getPageRectOnScreen(pageIndex: Int): RectF? {
        val pv = pdfView ?: return null
        return com.github.barteksc.pdfviewer.PdfViewUtils.getPageRect(pv, pageIndex)
    }

    private fun findPageAt(screenX: Float, screenY: Float): Pair<Int, RectF>? {
        val pv = pdfView ?: return null
        val count = pv.pageCount
        if (count == 0) return null
        for (i in 0 until count) {
            val rect = getPageRectOnScreen(i) ?: continue
            if (screenY >= rect.top && screenY <= rect.bottom + pv.spacingPx &&
                screenX >= rect.left && screenX <= rect.right) {
                return Pair(i, rect)
            }
        }
        for (i in 0 until count) {
            val rect = getPageRectOnScreen(i) ?: continue
            if (screenY >= rect.top && screenY <= rect.bottom + pv.spacingPx) {
                return Pair(i, rect)
            }
        }
        val current = pv.currentPage.coerceIn(0, count - 1)
        val rect = getPageRectOnScreen(current)
        return if (rect != null) Pair(current, rect) else null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pv = pdfView ?: return super.onTouchEvent(event)
        val toolType = event.getToolType(0)
        val isStylus = (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER)
        val isEraserTool = (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER)

        // If user is in SELECT / Hand mode, or drawing with finger is disabled, let PDFView scroll & pinch-to-zoom
        if (currentToolMode == CanvasToolMode.SELECT || (!isStylus && !isFingerDrawingEnabled)) {
            return pv.dispatchTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val pageInfo = findPageAt(event.x, event.y)
                if (pageInfo == null) {
                    return pv.dispatchTouchEvent(event)
                }
                targetPageIndex = pageInfo.first
                val targetRect = pageInfo.second
                strokeStartTime = System.currentTimeMillis()

                if (isEraserTool) {
                    handleErase(targetPageIndex, targetRect, event.x, event.y)
                } else {
                    val tool = when (currentToolMode) {
                        CanvasToolMode.HIGHLIGHTER -> Stroke.Tool.HIGHLIGHTER
                        CanvasToolMode.PENCIL -> Stroke.Tool.PENCIL
                        else -> Stroke.Tool.PEN
                    }
                    val stroke = ActiveStroke(
                        id = UUID.randomUUID().toString(),
                        color = selectedColor,
                        baseWidth = selectedWidth,
                        tool = tool
                    )
                    stroke.addPoint(
                        StrokePoint(
                            x = event.x,
                            y = event.y,
                            pressure = event.pressure.coerceIn(0.1f, 1f),
                            timestampMs = strokeStartTime
                        )
                    )
                    activeStroke = stroke
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val targetRect = getPageRectOnScreen(targetPageIndex)
                if (isEraserTool) {
                    if (targetRect != null) {
                        handleErase(targetPageIndex, targetRect, event.x, event.y)
                    }
                } else {
                    val stroke = activeStroke ?: return true
                    for (i in 0 until event.historySize) {
                        stroke.addPoint(
                            StrokePoint(
                                x = event.getHistoricalX(i),
                                y = event.getHistoricalY(i),
                                pressure = event.getHistoricalPressure(i).coerceIn(0.1f, 1f),
                                timestampMs = event.getHistoricalEventTime(i)
                            )
                        )
                    }
                    stroke.addPoint(
                        StrokePoint(
                            x = event.x,
                            y = event.y,
                            pressure = event.pressure.coerceIn(0.1f, 1f),
                            timestampMs = event.eventTime
                        )
                    )
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isEraserTool) {
                    val stroke = activeStroke
                    val rect = getPageRectOnScreen(targetPageIndex)
                    val page = pages.getOrNull(targetPageIndex)

                    if (stroke != null && stroke.points.isNotEmpty() && rect != null && page != null && rect.width() > 0 && rect.height() > 0) {
                        val scaleX = page.width / rect.width()
                        val scaleY = page.height / rect.height()

                        val docPoints = stroke.points.map { pt ->
                            val relX = (pt.x - rect.left).coerceIn(0f, rect.width())
                            val relY = (pt.y - rect.top).coerceIn(0f, rect.height())
                            pt.copy(
                                x = relX * scaleX,
                                y = relY * scaleY
                            )
                        }

                        val minX = docPoints.minOf { it.x }
                        val minY = docPoints.minOf { it.y }
                        val maxX = docPoints.maxOf { it.x }
                        val maxY = docPoints.maxOf { it.y }

                        val committedStroke = Stroke(
                            id = stroke.id,
                            zIndex = page.elements.size,
                            boundingBox = RectData(minX, minY, maxX, maxY),
                            createdAt = strokeStartTime,
                            points = docPoints,
                            color = stroke.color,
                            baseWidth = stroke.baseWidth * scaleX,
                            tool = stroke.tool
                        )

                        onCommandIssued?.invoke(targetPageIndex, Command.AddElement(committedStroke))
                    }
                    activeStroke = null
                    invalidate()
                    pv.invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activeStroke = null
                invalidate()
                return true
            }
        }
        return pv.dispatchTouchEvent(event)
    }

    private fun handleErase(pageIndex: Int, rect: RectF, screenX: Float, screenY: Float) {
        val pv = pdfView ?: return
        val page = pages.getOrNull(pageIndex) ?: return
        if (page.elements.isEmpty() || rect.width() <= 0 || rect.height() <= 0) return

        val scaleX = page.width / rect.width()
        val scaleY = page.height / rect.height()
        val docX = (screenX - rect.left).coerceIn(0f, rect.width()) * scaleX
        val docY = (screenY - rect.top).coerceIn(0f, rect.height()) * scaleY

        val mode = if (currentToolMode == CanvasToolMode.STROKE_ERASER) EraserMode.STROKE_ERASER else EraserMode.PIXEL_ERASER
        val command = eraserEngine.eraseAt(page.elements, docX, docY, 32f * scaleX, mode)
        if (command != null) {
            onCommandIssued?.invoke(pageIndex, command)
            pv.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = activeStroke ?: return
        // Render only the active in-flight stroke under the stylus tip while drawing
        strokeRenderer.renderActiveStroke(canvas, stroke)
    }
}
