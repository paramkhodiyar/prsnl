package com.prsnl.drawing.filter

import android.view.MotionEvent

class InputFilter {
    var isStylusActive = false
        internal set
    var activeStylusPointerId: Int = -1
        internal set
    private var lastStylusHoverTimeMs: Long = 0L

    var onStylusDetected: (() -> Unit)? = null

    fun handleHoverEvent(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            val toolType = event.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
                lastStylusHoverTimeMs = System.currentTimeMillis()
                onStylusDetected?.invoke()
                return true
            }
        }
        return false
    }

    fun shouldAcceptPointer(event: MotionEvent, pointerIndex: Int): Boolean {
        val toolType = event.getToolType(pointerIndex)
        val pointerId = event.getPointerId(pointerIndex)
        val currentTime = System.currentTimeMillis()

        // 1. Stylus or Digital Pen touch event detected -> 0ms Instant Lock
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            isStylusActive = true
            activeStylusPointerId = pointerId
            lastStylusHoverTimeMs = currentTime
            onStylusDetected?.invoke()
            return true
        }

        // 2. Active 0ms Palm Rejection:
        // If stylus hovered within last 3000ms or is active, instantly reject finger touches
        val isStylusHovering = (currentTime - lastStylusHoverTimeMs) < 3000L
        if (isStylusActive || isStylusHovering) {
            if (toolType == MotionEvent.TOOL_TYPE_FINGER) {
                return false
            }
        }

        return true
    }

    fun normalizePressure(rawPressure: Float): Float {
        return rawPressure.coerceIn(0.0f, 1.0f)
    }

    fun onPointerUpOrCancel(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId == activeStylusPointerId) {
            isStylusActive = false
            activeStylusPointerId = -1
        }
    }
}
