package com.prsnl.drawing.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.prsnl.document.model.Background
import com.prsnl.document.model.Command
import com.prsnl.document.model.Element
import com.prsnl.document.model.ImageElement
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.document.model.TextBox
import com.prsnl.drawing.eraser.EraserEngine
import com.prsnl.drawing.eraser.EraserMode
import com.prsnl.drawing.filter.InputFilter
import com.prsnl.drawing.model.ActiveStroke
import com.prsnl.drawing.render.BackgroundRenderer
import com.prsnl.drawing.render.ShapeRenderer
import com.prsnl.drawing.render.StrokeRenderer
import com.prsnl.drawing.selection.LassoEngine
import com.prsnl.drawing.selection.SelectionHandler
import com.prsnl.drawing.shape.ShapeRecognizer
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

enum class CanvasToolMode {
    PEN,
    PENCIL,
    HIGHLIGHTER,
    STROKE_ERASER,
    PIXEL_ERASER,
    SHAPE_PICKER,
    SELECT,
    LASSO,
    TEXT
}

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentToolMode: CanvasToolMode = CanvasToolMode.PEN
    var currentColor: Int = Color.BLACK
    var currentBaseWidth: Float = 6f
    var eraserRadius: Float = 32f
    var currentInsertedShapeType: Shape.Type = Shape.Type.RECTANGLE

    var currentBackground: Background = Background(
        type = Background.Type.RULED,
        lineSpacing = 40f,
        colorLight = 0xFFFAF8F5.toInt(),
        colorDark = 0xFF1C1C1E.toInt()
    )

    var pageIndex: Int = 0
    var documentWidth: Float = 1200f
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }
    var documentHeight: Float = 1697f
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    var isFingerDrawingEnabled: Boolean = false
    var isPressureSensitivityEnabled: Boolean = true

    var onCommandIssued: ((Command) -> Unit)? = null
    var onAutoStylusSwitch: (() -> Unit)? = null
    var onInsertTextBoxRequested: ((x: Float, y: Float) -> Unit)? = null
    var onInteractionStarted: (() -> Unit)? = null
    var onSelectionChanged: ((Boolean) -> Unit)? = null

    private val strokeRenderer = StrokeRenderer()
    private val shapeRenderer = ShapeRenderer()
    private val backgroundRenderer = BackgroundRenderer()
    private val eraserEngine = EraserEngine()
    private val selectionHandler = SelectionHandler()
    private val shapeRecognizer = ShapeRecognizer()
    private val lassoEngine = LassoEngine()
    private val inputFilter = InputFilter()

    private val localElementsList = mutableListOf<Element>()
    private var activeStroke: ActiveStroke? = null
    private var eraserTouchPoint: Pair<Float, Float>? = null

    // Lasso & Selection State
    private val lassoPoints = mutableListOf<StrokePoint>()
    private val lassoPath = Path()
    private var dashPhase = 0f
    // All elements selected by lasso (rendered with marching ants)
    private val lassoSelectedElements = mutableListOf<Element>()

    // Transform State for SELECT mode
    private var isDraggingTopRotationHandle = false
    private var initialRotationAngle = 0f
    private var isDraggingSelectedElement = false
    private var isDraggingCornerHandle = false
    private var draggingCornerIndex = -1   // 0=TL, 1=TR, 2=BL, 3=BR
    private var selectedElement: Element? = null
    private var transformStartElement: Element? = null
    private var transformLastElement: Element? = null
    private var selectDragLastX = 0f
    private var selectDragLastY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isStylusStrokeActive = false

    var committedElements: List<Element>
        get() = synchronized(localElementsList) { localElementsList.toList() }
        set(value) {
            synchronized(localElementsList) {
                localElementsList.clear()
                localElementsList.addAll(value)
            }
            invalidate()
        }

    private var panOffsetX = 0f
    private var panOffsetY = 0f
    private var zoomScale = 1.0f

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val oldScale = zoomScale
            val newScale = (zoomScale * factor).coerceIn(0.3f, 8.0f)
            val scaleRatio = newScale / oldScale

            val focusX = detector.focusX
            val focusY = detector.focusY

            panOffsetX = focusX - (focusX - panOffsetX) * scaleRatio
            panOffsetY = focusY - (focusY - panOffsetY) * scaleRatio
            zoomScale = newScale

            invalidate()
            return true
        }
    })

    private val selectionBoxPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFF38BDF8.toInt()
    }

    private val handleFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val handleStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFF38BDF8.toInt()
    }

    private val lassoPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFC88A4B.toInt()
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 28f
        color = Color.BLACK
    }

    private var lastPointX = 0f
    private var lastPointY = 0f
    private var lastPointTime = 0L
    private var strokeStartTimeMs = 0L
    private var lastPressure = 0f

    private var holdToRecognizeRunnable: Runnable? = null

    init {
        inputFilter.onStylusDetected = {
            onAutoStylusSwitch?.invoke()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            onInteractionStarted?.invoke()
        }
        val index = event.actionIndex
        val safeIndex = index.coerceIn(0, event.pointerCount - 1)
        val isStylusEvent = event.hasStylusPointer()
        val isNavigationGesture = !isStylusStrokeActive && !isStylusEvent && event.pointerCount >= 2

        if (isNavigationGesture) {
            parent?.requestDisallowInterceptTouchEvent(true)
            scaleGestureDetector.onTouchEvent(event)
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            return true
        }

        if (!inputFilter.shouldAcceptPointer(event, safeIndex, isFingerDrawingEnabled)) {
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        when (currentToolMode) {
            CanvasToolMode.PEN, CanvasToolMode.PENCIL, CanvasToolMode.HIGHLIGHTER -> {
                handleDrawingTouch(event, action, safeIndex)
            }
            CanvasToolMode.SHAPE_PICKER -> {
                handleShapePickerTouch(event, action, safeIndex)
            }
            CanvasToolMode.SELECT -> {
                handleSelectTouch(event, action, safeIndex)
            }
            CanvasToolMode.LASSO -> {
                handleLassoTouch(event, action, safeIndex)
            }
            CanvasToolMode.TEXT -> {
                handleTextTouch(event, action, safeIndex)
            }
            CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> {
                handleEraserTouch(event, action, safeIndex)
            }
        }
        return true
    }

    private fun handleDrawingTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                strokeStartTimeMs = System.currentTimeMillis()
                lastPointTime = event.eventTime
                lastPressure = 0f

                val firstPoint = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
                activePointerId = event.getPointerId(safeIndex)
                isStylusStrokeActive = event.getToolType(safeIndex).isStylusTool()
                lastPointX = firstPoint.x
                lastPointY = firstPoint.y

                val strokeId = UUID.randomUUID().toString()
                val newActiveStroke = ActiveStroke(
                    id = strokeId,
                    color = currentColor,
                    baseWidth = currentBaseWidth,
                    tool = when (currentToolMode) {
                        CanvasToolMode.HIGHLIGHTER -> Stroke.Tool.HIGHLIGHTER
                        CanvasToolMode.PENCIL -> Stroke.Tool.PENCIL
                        else -> Stroke.Tool.PEN
                    }
                )
                newActiveStroke.addPoint(firstPoint)
                activeStroke = newActiveStroke
                // NOTE: hold-to-recognize timer is ONLY for SHAPE_PICKER mode.
                // Never fire it during freehand PEN/PENCIL/HIGHLIGHTER drawing.
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val current = activeStroke ?: return
                val pointerIndex = event.findActivePointerIndex() ?: return
                val historySize = event.historySize
                for (h in 0 until historySize) {
                    val historical = extractHistoricalPointSafely(event, pointerIndex, h, strokeStartTimeMs)
                    if (historical != null) current.addPoint(historical)
                }
                val p = extractPointSafely(event, pointerIndex, strokeStartTimeMs) ?: return
                current.addPoint(p)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                val current = activeStroke
                if (current != null) {
                    val pointerIndex = event.findActivePointerIndex() ?: safeIndex
                    val p = extractPointSafely(event, pointerIndex, strokeStartTimeMs)
                    if (p != null) current.addPoint(p)

                    val snapshot = synchronized(localElementsList) { localElementsList.toList() }
                    val nextZIndex = (snapshot.maxOfOrNull { it.zIndex } ?: -1) + 1
                    val committed = current.toCommittedStroke(
                        zIndex = nextZIndex,
                        createdAt = System.currentTimeMillis()
                    )
                    synchronized(localElementsList) {
                        localElementsList.add(committed)
                    }
                    activeStroke = null
                    onCommandIssued?.invoke(Command.AddElement(committed))
                    invalidate()
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isStylusStrokeActive = false
            }

            MotionEvent.ACTION_CANCEL -> {
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                activeStroke = null
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isStylusStrokeActive = false
                invalidate()
            }
        }
    }

    // Hold-to-recognize: ONLY used from SHAPE_PICKER mode, never from PEN/PENCIL/HIGHLIGHTER.
    private fun scheduleShapePickerHoldRecognize() {
        cancelHoldToRecognizeTimer()
        val runnable = Runnable {
            val current = activeStroke ?: return@Runnable
            val snapshot = current.toCommittedStroke(0, System.currentTimeMillis())
            val recognizedShape = shapeRecognizer.recognize(snapshot)
            if (recognizedShape != null) {
                synchronized(localElementsList) {
                    localElementsList.add(recognizedShape)
                }
                activeStroke = null
                onCommandIssued?.invoke(Command.AddElement(recognizedShape))
                post { invalidate() }
            } else if (snapshot.points.size >= 4) {
                val first = snapshot.points.first()
                val last = snapshot.points.last()
                val straightLine = Shape(
                    id = UUID.randomUUID().toString(),
                    zIndex = (localElementsList.maxOfOrNull { it.zIndex } ?: -1) + 1,
                    boundingBox = RectData(minOf(first.x, last.x), minOf(first.y, last.y), maxOf(first.x, last.x), maxOf(first.y, last.y)),
                    createdAt = System.currentTimeMillis(),
                    type = Shape.Type.LINE,
                    strokeColor = currentColor,
                    strokeWidth = currentBaseWidth
                )
                synchronized(localElementsList) {
                    localElementsList.add(straightLine)
                }
                activeStroke = null
                onCommandIssued?.invoke(Command.AddElement(straightLine))
                post { invalidate() }
            }
        }
        holdToRecognizeRunnable = runnable
        handler?.postDelayed(runnable, 600L)
    }

    private fun cancelHoldToRecognizeTimer() {
        holdToRecognizeRunnable?.let { handler?.removeCallbacks(it) }
        holdToRecognizeRunnable = null
    }

    private var activeShape: Shape? = null
    private var shapeStartX: Float = 0f
    private var shapeStartY: Float = 0f

    private fun handleShapePickerTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                strokeStartTimeMs = System.currentTimeMillis()
                shapeStartX = p.x
                shapeStartY = p.y
                val snapshot = synchronized(localElementsList) { localElementsList.toList() }
                val nextZIndex = (snapshot.maxOfOrNull { it.zIndex } ?: -1) + 1
                activeShape = Shape(
                    id = UUID.randomUUID().toString(),
                    zIndex = nextZIndex,
                    boundingBox = RectData(p.x, p.y, p.x + 1f, p.y + 1f),
                    createdAt = System.currentTimeMillis(),
                    type = currentInsertedShapeType,
                    strokeColor = currentColor,
                    strokeWidth = currentBaseWidth
                )
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                cancelHoldToRecognizeTimer()  // Cancel if user drags
                val current = activeShape
                if (current != null) {
                    val minX = minOf(shapeStartX, p.x)
                    val maxX = maxOf(shapeStartX, p.x)
                    val minY = minOf(shapeStartY, p.y)
                    val maxY = maxOf(shapeStartY, p.y)
                    activeShape = current.copy(
                        boundingBox = RectData(minX, minY, maxX, maxY)
                    )
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                cancelHoldToRecognizeTimer()
                val current = activeShape
                if (current != null) {
                    val minX = minOf(shapeStartX, p.x)
                    val maxX = maxOf(shapeStartX, p.x)
                    val minY = minOf(shapeStartY, p.y)
                    val maxY = maxOf(shapeStartY, p.y)
                    val finalShape = current.copy(
                        boundingBox = RectData(minX, minY, maxX, maxY)
                    )
                    synchronized(localElementsList) {
                        localElementsList.add(finalShape)
                    }
                    onCommandIssued?.invoke(Command.AddElement(finalShape))
                    activeShape = null
                    invalidate()
                }
            }
        }
    }

    private fun handleLassoTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                lassoPoints.clear()
                lassoSelectedElements.clear()
                onSelectionChanged?.invoke(false)
                lassoPoints.add(p)
                lassoPath.reset()
                lassoPath.moveTo(p.x, p.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                lassoPoints.add(p)
                lassoPath.lineTo(p.x, p.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                lassoPath.close()
                val snapshot = synchronized(localElementsList) { localElementsList.toList() }
                val selected = lassoEngine.findElementsInLasso(snapshot, lassoPoints)
                lassoSelectedElements.clear()
                lassoSelectedElements.addAll(selected)
                onSelectionChanged?.invoke(selected.isNotEmpty())
                // If exactly one element selected, also set it as the single SELECT target
                if (selected.size == 1) {
                    selectedElement = selected.first()
                    selectionHandler.selectAt(snapshot, selected.first().boundingBox.centerX, selected.first().boundingBox.centerY)
                }
                lassoPoints.clear()
                lassoPath.reset()
                invalidate()
            }
        }
    }

    private fun handleTextTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        if (action == MotionEvent.ACTION_UP) {
            val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
            onInsertTextBoxRequested?.invoke(p.x, p.y)
        }
    }

    private fun handleSelectTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
        val snapshot = synchronized(localElementsList) { localElementsList.toList() }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                isDraggingTopRotationHandle = false
                isDraggingSelectedElement = false
                isDraggingCornerHandle = false
                draggingCornerIndex = -1
                transformStartElement = null
                transformLastElement = null

                val prevSelected = selectedElement
                if (prevSelected != null) {
                    val bounds = prevSelected.boundingBox

                    // Check top rotation handle
                    val topRotateX = bounds.centerX
                    val topRotateY = bounds.top - 40f
                    if (hypot(p.x - topRotateX, p.y - topRotateY) < 30f) {
                        isDraggingTopRotationHandle = true
                        transformStartElement = prevSelected
                        initialRotationAngle = Math.toDegrees(atan2(
                            (p.y - bounds.centerY).toDouble(),
                            (p.x - bounds.centerX).toDouble()
                        )).toFloat()
                        selectDragLastX = p.x
                        selectDragLastY = p.y
                        return
                    }

                    // Check corner scale handles
                    val corners = listOf(
                        Pair(bounds.left - 6f, bounds.top - 6f),    // TL = 0
                        Pair(bounds.right + 6f, bounds.top - 6f),   // TR = 1
                        Pair(bounds.left - 6f, bounds.bottom + 6f), // BL = 2
                        Pair(bounds.right + 6f, bounds.bottom + 6f) // BR = 3
                    )
                    for ((i, corner) in corners.withIndex()) {
                        if (hypot(p.x - corner.first, p.y - corner.second) < 28f) {
                            isDraggingCornerHandle = true
                            draggingCornerIndex = i
                            transformStartElement = prevSelected
                            selectDragLastX = p.x
                            selectDragLastY = p.y
                            return
                        }
                    }

                    // Check if inside bounding box to drag/move
                    if (p.x >= bounds.left && p.x <= bounds.right && p.y >= bounds.top && p.y <= bounds.bottom) {
                        isDraggingSelectedElement = true
                        transformStartElement = prevSelected
                        selectDragLastX = p.x
                        selectDragLastY = p.y
                        return
                    }
                }

                // Tap outside or on new element — update selection
                val hit = snapshot.lastOrNull { el ->
                    p.x >= el.boundingBox.left && p.x <= el.boundingBox.right &&
                    p.y >= el.boundingBox.top && p.y <= el.boundingBox.bottom
                }
                selectedElement = hit
                selectionHandler.selectAt(snapshot, p.x, p.y)
                onSelectionChanged?.invoke(hit != null)
                selectDragLastX = p.x
                selectDragLastY = p.y
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val current = selectedElement ?: return
                val dx = p.x - selectDragLastX
                val dy = p.y - selectDragLastY
                selectDragLastX = p.x
                selectDragLastY = p.y

                when {
                    isDraggingSelectedElement -> {
                        // Move element by dx/dy
                        val oldBox = current.boundingBox
                        val newBox = RectData(oldBox.left + dx, oldBox.top + dy, oldBox.right + dx, oldBox.bottom + dy)
                        val updated = updateElementBoundingBox(current, newBox)
                        if (updated != null) {
                            synchronized(localElementsList) {
                                val idx = localElementsList.indexOfFirst { it.id == current.id }
                                if (idx >= 0) localElementsList[idx] = updated
                            }
                            selectedElement = updated
                            transformLastElement = updated
                            invalidate()
                        }
                    }

                    isDraggingCornerHandle -> {
                        val oldBox = current.boundingBox
                        val newBox = when (draggingCornerIndex) {
                            0 -> RectData(oldBox.left + dx, oldBox.top + dy, oldBox.right, oldBox.bottom) // TL
                            1 -> RectData(oldBox.left, oldBox.top + dy, oldBox.right + dx, oldBox.bottom) // TR
                            2 -> RectData(oldBox.left + dx, oldBox.top, oldBox.right, oldBox.bottom + dy) // BL
                            3 -> RectData(oldBox.left, oldBox.top, oldBox.right + dx, oldBox.bottom + dy) // BR
                            else -> oldBox
                        }
                        // Prevent degenerate box
                        val safeBox = if (newBox.right - newBox.left > 20f && newBox.bottom - newBox.top > 20f) newBox else oldBox
                        val updated = updateElementBoundingBox(current, safeBox)
                        if (updated != null) {
                            synchronized(localElementsList) {
                                val idx = localElementsList.indexOfFirst { it.id == current.id }
                                if (idx >= 0) localElementsList[idx] = updated
                            }
                            selectedElement = updated
                            transformLastElement = updated
                            invalidate()
                        }
                    }

                    isDraggingTopRotationHandle -> {
                        if (current is Shape) {
                            val currentAngle = Math.toDegrees(atan2(
                                (p.y - current.boundingBox.centerY).toDouble(),
                                (p.x - current.boundingBox.centerX).toDouble()
                            )).toFloat()
                            val delta = currentAngle - initialRotationAngle
                            initialRotationAngle = currentAngle
                            val cmd = selectionHandler.createRotateCommand(current, delta)
                            if (cmd != null) {
                                val updatedPage = cmd.apply(com.prsnl.document.model.Page(
                                    "t", "t", 0, documentWidth, documentHeight, currentBackground, snapshot
                                ))
                                synchronized(localElementsList) {
                                    localElementsList.clear()
                                    localElementsList.addAll(updatedPage.elements)
                                }
                                selectedElement = updatedPage.elements.firstOrNull { it.id == current.id }
                                transformLastElement = selectedElement
                                invalidate()
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                commitSelectionTransform()
                isDraggingTopRotationHandle = false
                isDraggingSelectedElement = false
                isDraggingCornerHandle = false
                draggingCornerIndex = -1
                transformStartElement = null
                transformLastElement = null
            }
        }
    }

    fun deleteSelection(): Boolean {
        val targets = buildList {
            selectedElement?.let { add(it) }
            addAll(lassoSelectedElements)
        }.distinctBy { it.id }
        if (targets.isEmpty()) return false

        synchronized(localElementsList) {
            localElementsList.removeAll { element -> targets.any { it.id == element.id } }
        }
        selectedElement = null
        lassoSelectedElements.clear()
        selectionHandler.clearSelection()
        onSelectionChanged?.invoke(false)
        val command = if (targets.size == 1) {
            Command.DeleteElement(targets.first())
        } else {
            Command.CompoundCommand(targets.map { Command.DeleteElement(it) })
        }
        onCommandIssued?.invoke(command)
        invalidate()
        return true
    }

    private fun commitSelectionTransform() {
        val start = transformStartElement ?: return
        val end = transformLastElement ?: selectedElement ?: return
        if (start.boundingBox == end.boundingBox && start == end) return

        val command = when {
            isDraggingSelectedElement -> Command.ReplaceElement(start, end)
            isDraggingCornerHandle -> Command.ReplaceElement(start, end)
            isDraggingTopRotationHandle -> Command.ReplaceElement(start, end)
            else -> null
        }
        if (command != null) {
            onCommandIssued?.invoke(command)
        }
    }

    /** Returns a copy of [element] with its bounding box replaced by [newBox], for any element type. */
    private fun updateElementBoundingBox(element: Element, newBox: RectData): Element? {
        return when (element) {
            is Shape -> element.copy(boundingBox = newBox)
            is ImageElement -> element.copy(boundingBox = newBox)
            is TextBox -> element.copy(boundingBox = newBox)
            is Stroke -> {
                val oldBox = element.boundingBox
                val sx = if (oldBox.width == 0f) 1f else newBox.width / oldBox.width
                val sy = if (oldBox.height == 0f) 1f else newBox.height / oldBox.height
                element.copy(
                    boundingBox = newBox,
                    points = element.points.map { point ->
                        point.copy(
                            x = newBox.left + (point.x - oldBox.left) * sx,
                            y = newBox.top + (point.y - oldBox.top) * sy
                        )
                    }
                )
            }
            else -> null
        }
    }

    private fun handleEraserTouch(event: MotionEvent, action: Int, safeIndex: Int) {
        activeStroke = null
        val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                eraserTouchPoint = Pair(p.x, p.y)
                val snapshot = synchronized(localElementsList) { localElementsList.toList() }
                val mode = if (currentToolMode == CanvasToolMode.STROKE_ERASER) EraserMode.STROKE_ERASER else EraserMode.PIXEL_ERASER
                val eraseCommand = eraserEngine.eraseAt(snapshot, p.x, p.y, eraserRadius, mode)
                if (eraseCommand != null) {
                    val updatedPage = com.prsnl.document.model.Page(
                        id = "temp", notebookId = "temp", index = 0, width = documentWidth, height = documentHeight,
                        background = currentBackground, elements = snapshot
                    )
                    val appliedPage = eraseCommand.apply(updatedPage)
                    synchronized(localElementsList) {
                        localElementsList.clear()
                        localElementsList.addAll(appliedPage.elements)
                    }
                    onCommandIssued?.invoke(eraseCommand)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                eraserTouchPoint = null
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)

            canvas.save()
            canvas.translate(panOffsetX, panOffsetY)
            val scale = currentCanvasScale()
            canvas.scale(scale, scale)

            // Paper background with page index
            backgroundRenderer.renderBackground(canvas, currentBackground, documentWidth, documentHeight, pageIndex = pageIndex)

            val elementsSnapshot = synchronized(localElementsList) { localElementsList.toList() }

            // Render committed elements
            for (element in elementsSnapshot) {
                when (element) {
                    is Stroke -> strokeRenderer.renderCommittedStroke(canvas, element)
                    is Shape -> shapeRenderer.renderShape(canvas, element)
                    is TextBox -> {
                        canvas.drawText(element.content, element.boundingBox.left, element.boundingBox.top + 28f, textPaint)
                    }
                    is ImageElement -> {
                        try {
                            val bitmap = BitmapFactory.decodeFile(element.assetPath)
                            if (bitmap != null) {
                                val rect = RectF(element.boundingBox.left, element.boundingBox.top, element.boundingBox.right, element.boundingBox.bottom)
                                canvas.drawBitmap(bitmap, null, rect, null)
                            }
                        } catch (_: Exception) {}
                    }
                    else -> {}
                }
            }

            // Render active stroke
            val active = activeStroke
            if (active != null) {
                strokeRenderer.renderActiveStroke(canvas, active)
            }

            // Render active shape
            val activeS = activeShape
            if (activeS != null) {
                shapeRenderer.renderShape(canvas, activeS)
            }

            // Render active Lasso path loop
            if (lassoPoints.size > 1) {
                canvas.drawPath(lassoPath, lassoPaint)
            }

            // Render lasso multi-selection highlights
            for (el in lassoSelectedElements) {
                val b = el.boundingBox
                canvas.drawRect(b.left - 4f, b.top - 4f, b.right + 4f, b.bottom + 4f, selectionBoxPaint)
            }

            // Render Selection Bounding Box with Animated Marching-Ants Border & Handles
            val sel = selectedElement
            if (sel != null) {
                val box = sel.boundingBox
                dashPhase = (dashPhase + 1.5f) % 32f
                selectionBoxPaint.pathEffect = DashPathEffect(floatArrayOf(16f, 12f), dashPhase)

                // Draw Marching Ants Dashed Border
                canvas.drawRect(box.left - 6f, box.top - 6f, box.right + 6f, box.bottom + 6f, selectionBoxPaint)

                // Corner Scale Handles (White circles with Sky-Blue outline)
                val handles = listOf(
                    Pair(box.left - 6f, box.top - 6f),
                    Pair(box.right + 6f, box.top - 6f),
                    Pair(box.left - 6f, box.bottom + 6f),
                    Pair(box.right + 6f, box.bottom + 6f)
                )
                for ((hx, hy) in handles) {
                    canvas.drawCircle(hx, hy, 12f, handleFillPaint)
                    canvas.drawCircle(hx, hy, 12f, handleStrokePaint)
                }

                // Top Center Axis Rotation Handle (line + circle)
                val topRotateX = box.centerX
                val topRotateY = box.top - 40f
                canvas.drawLine(topRotateX, box.top - 6f, topRotateX, topRotateY, handleStrokePaint)
                canvas.drawCircle(topRotateX, topRotateY, 14f, handleFillPaint)
                canvas.drawCircle(topRotateX, topRotateY, 14f, handleStrokePaint)

                postInvalidateOnAnimation()
            }

            canvas.restore()
        } catch (_: Exception) {}
    }

    private fun extractPointSafely(event: MotionEvent, index: Int, startTime: Long): StrokePoint? {
        val count = event.pointerCount
        if (index < 0 || index >= count) return null
        return try {
            val (cx, cy) = screenToCanvas(event.getX(index), event.getY(index))
            val rawP = if (isPressureSensitivityEnabled) event.getPressure(index) else 1.0f
            val dt = (event.eventTime - lastPointTime).coerceAtLeast(1L)
            val dist = hypot(cx - lastPointX, cy - lastPointY)
            val velocity = dist / dt.toFloat()

            val smoothedP = if (isPressureSensitivityEnabled) {
                if (lastPressure > 0f) 0.75f * lastPressure + 0.25f * rawP else rawP
            } else 1.0f
            lastPressure = smoothedP

            val velocityModulation = (1.0f / (1.0f + 0.12f * velocity)).coerceIn(0.3f, 1.0f)
            val finalPressure = if (isPressureSensitivityEnabled) {
                (smoothedP * 0.7f + velocityModulation * 0.3f).coerceIn(0.2f, 1.2f)
            } else 1.0f

            lastPointX = cx
            lastPointY = cy
            lastPointTime = event.eventTime

            StrokePoint(
                x = cx,
                y = cy,
                pressure = finalPressure,
                timestampMs = event.eventTime - startTime
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractHistoricalPointSafely(event: MotionEvent, index: Int, historyIndex: Int, startTime: Long): StrokePoint? {
        val count = event.pointerCount
        if (index < 0 || index >= count) return null
        return try {
            val (cx, cy) = screenToCanvas(event.getHistoricalX(index, historyIndex), event.getHistoricalY(index, historyIndex))
            val rawP = if (isPressureSensitivityEnabled) event.getHistoricalPressure(index, historyIndex) else 1.0f
            val eventTime = event.getHistoricalEventTime(historyIndex)
            val dt = (eventTime - lastPointTime).coerceAtLeast(1L)
            val dist = hypot(cx - lastPointX, cy - lastPointY)
            val velocity = dist / dt.toFloat()
            val smoothedP = if (isPressureSensitivityEnabled) {
                if (lastPressure > 0f) 0.75f * lastPressure + 0.25f * rawP else rawP
            } else 1.0f
            lastPressure = smoothedP
            val velocityModulation = (1.0f / (1.0f + 0.12f * velocity)).coerceIn(0.3f, 1.0f)
            val finalPressure = if (isPressureSensitivityEnabled) {
                (smoothedP * 0.7f + velocityModulation * 0.3f).coerceIn(0.2f, 1.2f)
            } else 1.0f

            lastPointX = cx
            lastPointY = cy
            lastPointTime = eventTime

            StrokePoint(
                x = cx,
                y = cy,
                pressure = finalPressure,
                timestampMs = eventTime - startTime
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun screenToCanvas(sx: Float, sy: Float): Pair<Float, Float> {
        val scale = currentCanvasScale()
        val cx = (sx - panOffsetX) / scale
        val cy = (sy - panOffsetY) / scale
        return Pair(cx, cy)
    }

    private fun currentCanvasScale(): Float {
        if (width <= 0 || height <= 0) return 1f
        val fitScale = min(width / documentWidth, height / documentHeight).coerceAtLeast(0.001f)
        return fitScale * zoomScale
    }

    private fun MotionEvent.hasStylusPointer(): Boolean {
        for (i in 0 until pointerCount) {
            if (getToolType(i).isStylusTool()) return true
        }
        return false
    }

    private fun Int.isStylusTool(): Boolean {
        return this == MotionEvent.TOOL_TYPE_STYLUS || this == MotionEvent.TOOL_TYPE_ERASER
    }

    private fun MotionEvent.findActivePointerIndex(): Int? {
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) return actionIndex.coerceIn(0, pointerCount - 1)
        val index = findPointerIndex(activePointerId)
        return if (index >= 0) index else null
    }
}
