package com.prsnl.drawing.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class CanvasToolMode {
    PEN,
    PENCIL,
    HIGHLIGHTER,
    SHAPE_PICKER,
    SELECT,
    LASSO,
    TEXT,
    STROKE_ERASER,
    PIXEL_ERASER
}

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentToolMode: CanvasToolMode = CanvasToolMode.PEN
        set(value) {
            field = value
            if (value != CanvasToolMode.SELECT && value != CanvasToolMode.LASSO) {
                selectedElement = null
                lassoSelectedElements.clear()
                onSelectionChanged?.invoke(false)
            }
            invalidate()
        }

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
        set(value) {
            field = value
            invalidate()
        }

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
    var onEditTextBoxRequested: ((TextBox) -> Unit)? = null
    var onToolModeAutoSwitchRequested: ((CanvasToolMode) -> Unit)? = null
    var onInteractionStarted: (() -> Unit)? = null
    var onSelectionChanged: ((Boolean) -> Unit)? = null

    private var lastTappedTextBoxId: String? = null
    private var lastTextBoxTapTimeMs: Long = 0L

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
    private val lassoSelectedElements = mutableListOf<Element>()

    // Transform State for SELECT mode
    private var isDraggingTopRotationHandle = false
    private var initialRotationAngle = 0f
    private var isDraggingSelectedElement = false
    private var isDraggingCornerHandle = false
    private var draggingCornerIndex = -1   // 0=TL, 1=TR, 2=BL, 3=BR
    private var selectedElement: Element? = null
    private var transformStartElement: Element? = null
    private var transformStartGroup = listOf<Element>()
    private var transformLastElement: Element? = null
    private var selectDragLastX = 0f
    private var selectDragLastY = 0f
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
        strokeWidth = 3f
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
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                strokeStartTimeMs = System.currentTimeMillis()
                lastPointTime = event.eventTime
                val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
                lastPointX = p.x
                lastPointY = p.y
                lastPressure = p.pressure

                val tool = when (currentToolMode) {
                    CanvasToolMode.HIGHLIGHTER -> Stroke.Tool.HIGHLIGHTER
                    CanvasToolMode.PENCIL -> Stroke.Tool.PENCIL
                    else -> Stroke.Tool.PEN
                }

                val newActive = ActiveStroke(
                    id = UUID.randomUUID().toString(),
                    color = currentColor,
                    baseWidth = currentBaseWidth,
                    tool = tool
                )
                newActive.addPoint(p)
                activeStroke = newActive
                isStylusStrokeActive = true
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val active = activeStroke ?: return
                val historySize = event.historySize
                for (h in 0 until historySize) {
                    val hp = extractHistoricalPointSafely(event, safeIndex, h, strokeStartTimeMs)
                    if (hp != null) {
                        active.addPoint(hp)
                    }
                }
                val p = extractPointSafely(event, safeIndex, strokeStartTimeMs)
                if (p != null) {
                    active.addPoint(p)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                isStylusStrokeActive = false
                val active = activeStroke
                if (active != null && active.points.isNotEmpty()) {
                    val snapshot = synchronized(localElementsList) { localElementsList.toList() }
                    val nextZIndex = (snapshot.maxOfOrNull { it.zIndex } ?: -1) + 1
                    val finalStroke = active.toCommittedStroke(nextZIndex, strokeStartTimeMs)
                    synchronized(localElementsList) {
                        localElementsList.add(finalStroke)
                    }
                    onCommandIssued?.invoke(Command.AddElement(finalStroke))
                }
                activeStroke = null
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                isStylusStrokeActive = false
                activeStroke = null
                invalidate()
            }
        }
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
                cancelHoldToRecognizeTimer()
                val current = activeShape
                if (current != null) {
                    val box = if (current.type == Shape.Type.LINE || current.type == Shape.Type.ARROW || current.type == Shape.Type.ARROW_DOUBLE) {
                        RectData(shapeStartX, shapeStartY, p.x, p.y)
                    } else {
                        val minX = minOf(shapeStartX, p.x)
                        val maxX = maxOf(shapeStartX, p.x)
                        val minY = minOf(shapeStartY, p.y)
                        val maxY = maxOf(shapeStartY, p.y)
                        RectData(minX, minY, maxX, maxY)
                    }
                    activeShape = current.copy(boundingBox = box)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                cancelHoldToRecognizeTimer()
                val current = activeShape
                if (current != null) {
                    val box = if (current.type == Shape.Type.LINE || current.type == Shape.Type.ARROW || current.type == Shape.Type.ARROW_DOUBLE) {
                        RectData(shapeStartX, shapeStartY, p.x, p.y)
                    } else {
                        val minX = minOf(shapeStartX, p.x)
                        val maxX = maxOf(shapeStartX, p.x)
                        val minY = minOf(shapeStartY, p.y)
                        val maxY = maxOf(shapeStartY, p.y)
                        RectData(minX, minY, maxX, maxY)
                    }
                    val finalShape = current.copy(boundingBox = box)
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

    fun deleteSelectedElements() {
        val selSingle = selectedElement
        val selGroup = lassoSelectedElements.toList()
        if (selGroup.isNotEmpty()) {
            val cmds = selGroup.map { Command.DeleteElement(it) }
            onCommandIssued?.invoke(Command.CompoundCommand(cmds))
            lassoSelectedElements.clear()
            selectedElement = null
            onSelectionChanged?.invoke(false)
            invalidate()
        } else if (selSingle != null) {
            onCommandIssued?.invoke(Command.DeleteElement(selSingle))
            selectedElement = null
            onSelectionChanged?.invoke(false)
            invalidate()
        }
    }

    fun getActiveSelectionBox(): RectData? {
        if (lassoSelectedElements.isNotEmpty()) {
            val minLeft = lassoSelectedElements.minOf { minOf(it.boundingBox.left, it.boundingBox.right) }
            val minTop = lassoSelectedElements.minOf { minOf(it.boundingBox.top, it.boundingBox.bottom) }
            val maxRight = lassoSelectedElements.maxOf { maxOf(it.boundingBox.left, it.boundingBox.right) }
            val maxBottom = lassoSelectedElements.maxOf { maxOf(it.boundingBox.top, it.boundingBox.bottom) }
            return RectData(minLeft, minTop, maxRight, maxBottom)
        }
        val sel = selectedElement ?: return null
        val b = sel.boundingBox
        val minLeft = minOf(b.left, b.right)
        val minTop = minOf(b.top, b.bottom)
        val maxRight = maxOf(b.left, b.right)
        val maxBottom = maxOf(b.top, b.bottom)
        return RectData(minLeft, minTop, maxRight, maxBottom)
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
                if (selected.size == 1) {
                    selectedElement = selected.first()
                    selectionHandler.selectAt(snapshot, selected.first().boundingBox.centerX, selected.first().boundingBox.centerY)
                } else if (selected.size > 1) {
                    selectedElement = null
                }
                if (selected.isNotEmpty()) {
                    onToolModeAutoSwitchRequested?.invoke(CanvasToolMode.SELECT)
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
                transformStartElement = selectedElement
                transformStartGroup = lassoSelectedElements.toList()
                transformLastElement = null

                val activeBox = getActiveSelectionBox()
                if (activeBox != null) {
                    val bounds = activeBox

                    // Check top rotation handle
                    val topRotateX = bounds.centerX
                    val topRotateY = bounds.top - 40f
                    if (hypot(p.x - topRotateX, p.y - topRotateY) < 30f) {
                        isDraggingTopRotationHandle = true
                        transformStartElement = selectedElement
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
                            transformStartElement = selectedElement
                            selectDragLastX = p.x
                            selectDragLastY = p.y
                            return
                        }
                    }

                    // Check inside bounding box to drag/move
                    if (p.x >= bounds.left && p.x <= bounds.right && p.y >= bounds.top && p.y <= bounds.bottom) {
                        isDraggingSelectedElement = true
                        transformStartElement = selectedElement
                        selectDragLastX = p.x
                        selectDragLastY = p.y
                        return
                    }
                }

                // Tap outside -> clear lasso multi-selection & find hit element
                lassoSelectedElements.clear()
                val hit = snapshot.lastOrNull { el ->
                    val b = el.boundingBox
                    val minL = minOf(b.left, b.right)
                    val minT = minOf(b.top, b.bottom)
                    val maxR = maxOf(b.left, b.right)
                    val maxB = maxOf(b.top, b.bottom)
                    p.x >= minL && p.x <= maxR && p.y >= minT && p.y <= maxB
                }

                if (hit is TextBox) {
                    val now = System.currentTimeMillis()
                    if (hit.id == lastTappedTextBoxId && (now - lastTextBoxTapTimeMs) < 350L) {
                        onEditTextBoxRequested?.invoke(hit)
                        lastTappedTextBoxId = null
                        return
                    } else {
                        lastTappedTextBoxId = hit.id
                        lastTextBoxTapTimeMs = now
                    }
                } else {
                    lastTappedTextBoxId = null
                }

                selectedElement = hit
                selectionHandler.selectAt(snapshot, p.x, p.y)
                onSelectionChanged?.invoke(hit != null)
                selectDragLastX = p.x
                selectDragLastY = p.y
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = p.x - selectDragLastX
                val dy = p.y - selectDragLastY
                selectDragLastX = p.x
                selectDragLastY = p.y

                val isGroup = lassoSelectedElements.isNotEmpty()

                when {
                    isDraggingSelectedElement -> {
                        if (isGroup) {
                            val updatedGroup = lassoSelectedElements.mapNotNull { el ->
                                val oldB = el.boundingBox
                                val newB = RectData(oldB.left + dx, oldB.top + dy, oldB.right + dx, oldB.bottom + dy)
                                updateElementBoundingBox(el, newB)
                            }
                            synchronized(localElementsList) {
                                for (up in updatedGroup) {
                                    val idx = localElementsList.indexOfFirst { it.id == up.id }
                                    if (idx >= 0) localElementsList[idx] = up
                                }
                            }
                            lassoSelectedElements.clear()
                            lassoSelectedElements.addAll(updatedGroup)
                            invalidate()
                        } else {
                            val current = selectedElement ?: return
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
                    }

                    isDraggingCornerHandle -> {
                        val groupBox = getActiveSelectionBox() ?: return
                        val oldBox = groupBox
                        val newBox = when (draggingCornerIndex) {
                            0 -> RectData(oldBox.left + dx, oldBox.top + dy, oldBox.right, oldBox.bottom) // TL
                            1 -> RectData(oldBox.left, oldBox.top + dy, oldBox.right + dx, oldBox.bottom) // TR
                            2 -> RectData(oldBox.left + dx, oldBox.top, oldBox.right, oldBox.bottom + dy) // BL
                            3 -> RectData(oldBox.left, oldBox.top, oldBox.right + dx, oldBox.bottom + dy) // BR
                            else -> oldBox
                        }
                        val safeBox = if (newBox.right - newBox.left > 20f && newBox.bottom - newBox.top > 20f) newBox else oldBox
                        val sx = if (oldBox.width == 0f) 1f else safeBox.width / oldBox.width
                        val sy = if (oldBox.height == 0f) 1f else safeBox.height / oldBox.height

                        if (isGroup) {
                            val updatedGroup = lassoSelectedElements.mapNotNull { el ->
                                val b = el.boundingBox
                                val nLeft = safeBox.left + (b.left - oldBox.left) * sx
                                val nTop = safeBox.top + (b.top - oldBox.top) * sy
                                val nRight = nLeft + b.width * sx
                                val nBottom = nTop + b.height * sy
                                updateElementBoundingBox(el, RectData(nLeft, nTop, nRight, nBottom))
                            }
                            synchronized(localElementsList) {
                                for (up in updatedGroup) {
                                    val idx = localElementsList.indexOfFirst { it.id == up.id }
                                    if (idx >= 0) localElementsList[idx] = up
                                }
                            }
                            lassoSelectedElements.clear()
                            lassoSelectedElements.addAll(updatedGroup)
                            invalidate()
                        } else {
                            val current = selectedElement ?: return
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
                    }

                    isDraggingTopRotationHandle -> {
                        val groupBox = getActiveSelectionBox() ?: return
                        val currentAngle = Math.toDegrees(atan2(
                            (p.y - groupBox.centerY).toDouble(),
                            (p.x - groupBox.centerX).toDouble()
                        )).toFloat()
                        val delta = currentAngle - initialRotationAngle
                        initialRotationAngle = currentAngle

                        val targets = if (isGroup) lassoSelectedElements else listOfNotNull(selectedElement)
                        val updatedGroup = targets.mapNotNull { el ->
                            rotateElementByAngle(el, groupBox.centerX, groupBox.centerY, delta)
                        }

                        synchronized(localElementsList) {
                            for (up in updatedGroup) {
                                val idx = localElementsList.indexOfFirst { it.id == up.id }
                                if (idx >= 0) localElementsList[idx] = up
                            }
                        }

                        if (isGroup) {
                            lassoSelectedElements.clear()
                            lassoSelectedElements.addAll(updatedGroup)
                        } else if (updatedGroup.isNotEmpty()) {
                            selectedElement = updatedGroup.first()
                        }
                        invalidate()
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

    private fun rotateElementByAngle(element: Element, originX: Float, originY: Float, angleDeg: Float): Element? {
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        fun rotatePoint(px: Float, py: Float): Pair<Float, Float> {
            val dx = px - originX
            val dy = py - originY
            val nx = originX + (dx * cosA - dy * sinA)
            val ny = originY + (dx * sinA + dy * cosA)
            return Pair(nx, ny)
        }

        return when (element) {
            is Shape -> {
                val newAngle = (element.rotation + angleDeg) % 360f
                element.copy(rotation = newAngle)
            }
            is Stroke -> {
                val newPoints = element.points.map { pt ->
                    val (nx, ny) = rotatePoint(pt.x, pt.y)
                    pt.copy(x = nx, y = ny)
                }
                val minX = newPoints.minOf { it.x }
                val minY = newPoints.minOf { it.y }
                val maxX = newPoints.maxOf { it.x }
                val maxY = newPoints.maxOf { it.y }
                element.copy(points = newPoints, boundingBox = RectData(minX, minY, maxX, maxY))
            }
            is TextBox -> {
                val (nx, ny) = rotatePoint(element.boundingBox.left, element.boundingBox.top)
                val w = element.boundingBox.width
                val h = element.boundingBox.height
                element.copy(boundingBox = RectData(nx, ny, nx + w, ny + h))
            }
            is ImageElement -> {
                val (nx, ny) = rotatePoint(element.boundingBox.left, element.boundingBox.top)
                val w = element.boundingBox.width
                val h = element.boundingBox.height
                element.copy(boundingBox = RectData(nx, ny, nx + w, ny + h))
            }
            else -> null
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
        if (lassoSelectedElements.isNotEmpty() && transformStartGroup.isNotEmpty()) {
            val replaceCmds = mutableListOf<Command>()
            for (startEl in transformStartGroup) {
                val endEl = lassoSelectedElements.find { it.id == startEl.id }
                if (endEl != null && startEl != endEl) {
                    replaceCmds.add(Command.ReplaceElement(startEl, endEl))
                }
            }
            if (replaceCmds.isNotEmpty()) {
                val compound = if (replaceCmds.size == 1) replaceCmds.first() else Command.CompoundCommand(replaceCmds)
                onCommandIssued?.invoke(compound)
            }
            transformStartGroup = emptyList()
            return
        }

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

            // Paper background
            backgroundRenderer.renderBackground(canvas, currentBackground, documentWidth, documentHeight, pageIndex = pageIndex)

            // Render committed elements
            val elementsToDraw = synchronized(localElementsList) { localElementsList.toList() }
            for (element in elementsToDraw) {
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
                            val bitmap = android.graphics.BitmapFactory.decodeFile(element.assetPath)
                            if (bitmap != null) {
                                val b = element.boundingBox
                                canvas.drawBitmap(bitmap, null, android.graphics.RectF(b.left, b.top, b.right, b.bottom), null)
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

            // Render Selection Bounding Box with Animated Marching-Ants Border & Handles
            val activeBox = getActiveSelectionBox()
            if (activeBox != null) {
                val box = activeBox
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
        val hCount = event.historySize
        if (index < 0 || index >= count || historyIndex < 0 || historyIndex >= hCount) return null
        return try {
            val hx = event.getHistoricalX(index, historyIndex)
            val hy = event.getHistoricalY(index, historyIndex)
            val eventTime = event.getHistoricalEventTime(historyIndex)
            val (cx, cy) = screenToCanvas(hx, hy)
            val rawP = if (isPressureSensitivityEnabled) event.getHistoricalPressure(index, historyIndex) else 1.0f
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

    private fun cancelHoldToRecognizeTimer() {
        holdToRecognizeRunnable?.let {
            removeCallbacks(it)
            holdToRecognizeRunnable = null
        }
    }
}
