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

    var isFingerDrawingEnabled: Boolean = true
    var isPressureSensitivityEnabled: Boolean = true

    var onCommandIssued: ((Command) -> Unit)? = null
    var onAutoStylusSwitch: (() -> Unit)? = null
    var onInsertTextBoxRequested: ((x: Float, y: Float) -> Unit)? = null

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

    // Transform State
    private var isDraggingTopRotationHandle = false
    private var initialRotationAngle = 0f

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
            zoomScale = (zoomScale * factor).coerceIn(0.5f, 5.0f)
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
        scaleGestureDetector.onTouchEvent(event)

        val action = event.actionMasked
        val index = event.actionIndex
        val safeIndex = index.coerceIn(0, event.pointerCount - 1)

        if (!inputFilter.shouldAcceptPointer(event, safeIndex)) {
            return false
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
                lastPressure = 0f

                val firstPoint = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
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
                scheduleHoldToRecognizeTimer()
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val current = activeStroke ?: return
                val p = extractPointSafely(event, safeIndex, strokeStartTimeMs) ?: return
                val dx = p.x - lastPointX
                val dy = p.y - lastPointY
                if (hypot(dx, dy) > 1.2f) {
                    cancelHoldToRecognizeTimer()
                }
                current.addPoint(p)
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                cancelHoldToRecognizeTimer()
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                val current = activeStroke
                if (current != null) {
                    val p = extractPointSafely(event, safeIndex, strokeStartTimeMs)
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
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelHoldToRecognizeTimer()
                inputFilter.onPointerUpOrCancel(event, safeIndex)
                activeStroke = null
                invalidate()
            }
        }
    }

    private fun scheduleHoldToRecognizeTimer() {
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
        handler?.postDelayed(runnable, 400L)
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
                if (selected.isNotEmpty()) {
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
        val selectedId = selectionHandler.selectedElementId

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (selectedId != null) {
                    val selectedElement = snapshot.firstOrNull { it.id == selectedId }
                    if (selectedElement != null) {
                        val bounds = selectedElement.boundingBox
                        val topRotateX = bounds.centerX
                        val topRotateY = bounds.top - 40f

                        if (hypot(p.x - topRotateX, p.y - topRotateY) < 30f) {
                            isDraggingTopRotationHandle = true
                            initialRotationAngle = Math.toDegrees(atan2((p.y - bounds.centerY).toDouble(), (p.x - bounds.centerX).toDouble())).toFloat()
                            return
                        }
                    }
                }
                isDraggingTopRotationHandle = false
                selectionHandler.selectAt(snapshot, p.x, p.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingTopRotationHandle && selectedId != null) {
                    val selectedElement = snapshot.firstOrNull { it.id == selectedId }
                    if (selectedElement is Shape) {
                        val currentAngle = Math.toDegrees(atan2((p.y - selectedElement.boundingBox.centerY).toDouble(), (p.x - selectedElement.boundingBox.centerX).toDouble())).toFloat()
                        val delta = currentAngle - initialRotationAngle
                        initialRotationAngle = currentAngle
                        val cmd = selectionHandler.createRotateCommand(selectedElement, delta)
                        if (cmd != null) {
                            val updatedPage = cmd.apply(com.prsnl.document.model.Page("t", "t", 0, width.toFloat(), height.toFloat(), currentBackground, snapshot))
                            synchronized(localElementsList) {
                                localElementsList.clear()
                                localElementsList.addAll(updatedPage.elements)
                            }
                            onCommandIssued?.invoke(cmd)
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingTopRotationHandle = false
            }
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
                        id = "temp", notebookId = "temp", index = 0, width = width.toFloat(), height = height.toFloat(),
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
            canvas.scale(zoomScale, zoomScale)

            // Paper background with page index
            backgroundRenderer.renderBackground(canvas, currentBackground, width.toFloat(), height.toFloat(), pageIndex = pageIndex)

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

            // Render Selection Bounding Box with Animated Marching-Ants Border & Top Rotation Handle
            val selectedId = selectionHandler.selectedElementId
            if (selectedId != null) {
                val selectedElement = elementsSnapshot.firstOrNull { it.id == selectedId }
                if (selectedElement != null) {
                    val box = selectedElement.boundingBox
                    dashPhase = (dashPhase + 1.5f) % 32f
                    selectionBoxPaint.pathEffect = DashPathEffect(floatArrayOf(16f, 12f), dashPhase)

                    // Draw Marching Ants Dashed Border
                    canvas.drawRect(box.left - 6f, box.top - 6f, box.right + 6f, box.bottom + 6f, selectionBoxPaint)

                    // Corner Scale Handles (White Circles with Sky-Blue outline)
                    val handles = listOf(
                        Pair(box.left - 6f, box.top - 6f),
                        Pair(box.right + 6f, box.top - 6f),
                        Pair(box.left - 6f, box.bottom + 6f),
                        Pair(box.right + 6f, box.bottom + 6f)
                    )
                    for ((hx, hy) in handles) {
                        canvas.drawCircle(hx, hy, 10f, handleFillPaint)
                        canvas.drawCircle(hx, hy, 10f, handleStrokePaint)
                    }

                    // Top Center Axis Rotation Handle
                    val topRotateX = box.centerX
                    val topRotateY = box.top - 40f
                    canvas.drawLine(topRotateX, box.top - 6f, topRotateX, topRotateY, handleStrokePaint)
                    canvas.drawCircle(topRotateX, topRotateY, 12f, handleFillPaint)
                    canvas.drawCircle(topRotateX, topRotateY, 12f, handleStrokePaint)

                    postInvalidateOnAnimation()
                }
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

    private fun screenToCanvas(sx: Float, sy: Float): Pair<Float, Float> {
        val cx = (sx - panOffsetX) / zoomScale
        val cy = (sy - panOffsetY) / zoomScale
        return Pair(cx, cy)
    }
}
