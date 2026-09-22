package com.prsnl.ui.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.PdfViewUtils
import com.prsnl.document.model.Command
import com.prsnl.document.model.Element
import com.prsnl.document.model.ImageElement
import com.prsnl.document.model.Page
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.document.model.TextBox
import com.prsnl.drawing.eraser.EraserEngine
import com.prsnl.drawing.eraser.EraserMode
import com.prsnl.drawing.model.ActiveStroke
import com.prsnl.drawing.render.ShapeRenderer
import com.prsnl.drawing.render.StrokeRenderer
import com.prsnl.drawing.view.CanvasToolMode
import java.util.UUID

class PdfAnnotationOverlayView(context: Context) : View(context) {

    var pdfView: PDFView? = null
        set(value) {
            field = value
            invalidate()
        }

    var pages: List<Page> = emptyList()
        set(value) {
            field = value
            localPageElements.clear()
            value.forEachIndexed { index, page ->
                localPageElements[index] = page.elements.toMutableList()
            }
            invalidate()
        }

    var currentToolMode: CanvasToolMode = CanvasToolMode.PEN
        set(value) {
            field = value
            if (value != CanvasToolMode.STROKE_ERASER && value != CanvasToolMode.PIXEL_ERASER) {
                eraserCursorPoint = null
            }
            invalidate()
        }
    var selectedColor: Int = Color.BLACK
    var selectedWidth: Float = 4f
    var eraserRadius: Float = 36f
    var isFingerDrawingEnabled: Boolean = false
    var onCommandIssued: ((pageIndex: Int, Command) -> Unit)? = null

    private val localPageElements = mutableMapOf<Int, MutableList<Element>>()

    private val strokeRenderer = StrokeRenderer()
    private val shapeRenderer = ShapeRenderer()
    private val textPaint = Paint().apply {
        isAntiAlias = true
    }
    private val eraserEngine = EraserEngine()
    private var activeStroke: ActiveStroke? = null
    private var eraserCursorPoint: Pair<Float, Float>? = null
    private var targetPageIndex: Int = 0
    private var strokeStartTime: Long = 0L

    private val eraserCursorFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x22EF4444.toInt()
    }
    private val eraserCursorOuterStroke = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0x66000000.toInt()
    }
    private val eraserCursorInnerStroke = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFFEF4444.toInt()
    }
    private val eraserCursorCenterPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xFFEF4444.toInt()
    }

    fun getPageRectOnScreen(pageIndex: Int): RectF? {
        val pv = pdfView ?: return null
        return PdfViewUtils.getPageRect(pv, pageIndex)
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
                    eraserCursorPoint = Pair(event.x, event.y)
                    handleErase(targetPageIndex, targetRect, event.x, event.y)
                    invalidate()
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
                    eraserCursorPoint = Pair(event.x, event.y)
                    if (targetRect != null) {
                        handleErase(targetPageIndex, targetRect, event.x, event.y)
                    }
                    invalidate()
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
                if (isEraserTool) {
                    eraserCursorPoint = null
                    invalidate()
                } else {
                    val stroke = activeStroke
                    val rect = getPageRectOnScreen(targetPageIndex)
                    val page = pages.getOrNull(targetPageIndex)

                    if (stroke != null && stroke.points.isNotEmpty() && rect != null && rect.width() > 0 && rect.height() > 0) {
                        val docWidth = page?.width ?: rect.width()
                        val docHeight = page?.height ?: rect.height()

                        val scaleX = docWidth / rect.width()
                        val scaleY = docHeight / rect.height()

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
                            zIndex = localPageElements[targetPageIndex]?.size ?: 0,
                            boundingBox = RectData(minX, minY, maxX, maxY),
                            createdAt = strokeStartTime,
                            points = docPoints,
                            color = stroke.color,
                            baseWidth = stroke.baseWidth * scaleX,
                            tool = stroke.tool
                        )

                        // Immediately retain locally to prevent any frame gap or disappearance
                        localPageElements.getOrPut(targetPageIndex) { mutableListOf() }.add(committedStroke)
                        onCommandIssued?.invoke(targetPageIndex, Command.AddElement(committedStroke))
                    }
                    activeStroke = null
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activeStroke = null
                eraserCursorPoint = null
                invalidate()
                return true
            }
        }
        return pv.dispatchTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val isEraserTool = (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER)
        if (isEraserTool) {
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                    eraserCursorPoint = Pair(event.x, event.y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    eraserCursorPoint = null
                    invalidate()
                    return true
                }
            }
        } else {
            if (eraserCursorPoint != null) {
                eraserCursorPoint = null
                invalidate()
            }
        }
        return super.onHoverEvent(event)
    }

    private fun handleErase(pageIndex: Int, rect: RectF, screenX: Float, screenY: Float) {
        val currentElements = localPageElements[pageIndex] ?: return
        if (currentElements.isEmpty() || rect.width() <= 0 || rect.height() <= 0) return

        val page = pages.getOrNull(pageIndex)
        val docWidth = page?.width ?: rect.width()
        val docHeight = page?.height ?: rect.height()

        val scaleX = docWidth / rect.width()
        val scaleY = docHeight / rect.height()
        val docX = (screenX - rect.left).coerceIn(0f, rect.width()) * scaleX
        val docY = (screenY - rect.top).coerceIn(0f, rect.height()) * scaleY

        val mode = if (currentToolMode == CanvasToolMode.STROKE_ERASER) EraserMode.STROKE_ERASER else EraserMode.PIXEL_ERASER
        val command = eraserEngine.eraseAt(currentElements, docX, docY, eraserRadius * scaleX, mode)
        if (command != null) {
            when (command) {
                is Command.DeleteElement -> {
                    currentElements.remove(command.element)
                }
                is Command.CompoundCommand -> {
                    val toDelete = command.commands.filterIsInstance<Command.DeleteElement>().map { it.element }
                    val toAdd = command.commands.filterIsInstance<Command.AddElement>().map { it.element }
                    currentElements.removeAll(toDelete)
                    currentElements.addAll(toAdd)
                }
                else -> {}
            }
            onCommandIssued?.invoke(pageIndex, command)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Render all committed annotations locked to their respective PDF pages
        val pv = pdfView
        if (pv != null) {
            val count = maxOf(pages.size, pv.pageCount)
            for (pageIndex in 0 until count) {
                val elements = localPageElements[pageIndex] ?: continue
                if (elements.isEmpty()) continue

                val rect = getPageRectOnScreen(pageIndex) ?: continue
                // Viewport culling: only draw pages currently visible on screen
                if (rect.bottom < 0f || rect.top > height.toFloat()) continue

                val page = pages.getOrNull(pageIndex)
                val docWidth = page?.width ?: rect.width()
                val docHeight = page?.height ?: rect.height()

                canvas.save()
                canvas.translate(rect.left, rect.top)
                val scaleX = rect.width() / docWidth
                val scaleY = rect.height() / docHeight
                canvas.scale(scaleX, scaleY)

                for (element in elements) {
                    when (element) {
                        is Stroke -> strokeRenderer.renderCommittedStroke(canvas, element)
                        is Shape -> shapeRenderer.renderShape(canvas, element)
                        is TextBox -> {
                            textPaint.color = element.color
                            textPaint.textSize = element.fontSize
                            canvas.drawText(element.content, element.boundingBox.left, element.boundingBox.top + element.fontSize, textPaint)
                        }
                        is ImageElement -> {
                            try {
                                val bitmap = BitmapFactory.decodeFile(element.assetPath)
                                if (bitmap != null) {
                                    val b = element.boundingBox
                                    canvas.drawBitmap(bitmap, null, RectF(b.left, b.top, b.right, b.bottom), null)
                                }
                            } catch (_: Exception) {}
                        }
                        else -> {}
                    }
                }
                canvas.restore()
            }
        }

        // 2. Render active in-flight stroke under stylus tip
        val stroke = activeStroke
        if (stroke != null) {
            strokeRenderer.renderActiveStroke(canvas, stroke)
        }

        // 3. Render active eraser cursor / reticle under stylus tip or during hover
        val isEraserTool = (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER)
        val cursorPt = eraserCursorPoint
        if (isEraserTool && cursorPt != null) {
            drawEraserCursor(canvas, cursorPt.first, cursorPt.second, eraserRadius)
        }
    }

    private fun drawEraserCursor(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, eraserCursorFillPaint)
        canvas.drawCircle(cx, cy, radius, eraserCursorOuterStroke)
        canvas.drawCircle(cx, cy, radius, eraserCursorInnerStroke)
        canvas.drawCircle(cx, cy, 2.5f, eraserCursorCenterPaint)

        // Precision crosshair tick marks extending from perimeter
        val tick = 4f
        canvas.drawLine(cx, cy - radius - tick, cx, cy - radius + 1f, eraserCursorInnerStroke)
        canvas.drawLine(cx, cy + radius - 1f, cx, cy + radius + tick, eraserCursorInnerStroke)
        canvas.drawLine(cx - radius - tick, cy, cx - radius + 1f, cy, eraserCursorInnerStroke)
        canvas.drawLine(cx + radius - 1f, cy, cx + radius + tick, cy, eraserCursorInnerStroke)
    }
}
