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

    fun shouldAcceptPointer(event: MotionEvent, pointerIndex: Int, isFingerDrawingEnabled: Boolean = false): Boolean {
        val toolType = event.getToolType(pointerIndex)
        val pointerId = event.getPointerId(pointerIndex)
        val currentTime = System.currentTimeMillis()

        // 1. Stylus or Digital Pen touch event detected -> 0ms Instant Lock
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            val wasAlreadyActive = isStylusActive
            isStylusActive = true
            activeStylusPointerId = pointerId
            lastStylusHoverTimeMs = currentTime
            if (!wasAlreadyActive) {
                onStylusDetected?.invoke()
            }
            return true
        }

        // 2. Finger input handling:
        if (toolType == MotionEvent.TOOL_TYPE_FINGER) {
            // Palm Rejection: Reject touches with unusually large contact area (palm resting on glass)
            try {
                val touchMajor = event.getTouchMajor(pointerIndex)
                if (touchMajor > PALM_TOUCH_THRESHOLD_PX) {
                    return false
                }
            } catch (_: Exception) {}

            // When finger drawing is turned OFF (default), finger touches MUST NOT draw ink!
            if (!isFingerDrawingEnabled) {
                return false
            }
            // Active Palm Rejection when finger drawing is enabled but stylus is hovering/active
            val isStylusHovering = (currentTime - lastStylusHoverTimeMs) < 3000L
            if (isStylusActive || isStylusHovering) {
                return false
            }
        }

        return true
    }

    fun normalizePressure(rawPressure: Float): Float {
        return rawPressure.coerceIn(0.0f, 1.0f)
    }

    /**
     * Exponential Moving Average (EMA) pressure smoothing to eliminate line jitter on stylus pressure transitions
     */
    fun smoothPressureEma(previousPressure: Float, currentPressure: Float, alpha: Float = 0.35f): Float {
        val normalized = normalizePressure(currentPressure)
        if (previousPressure <= 0f) return normalized
        return alpha * normalized + (1f - alpha) * previousPressure
    }

    /**
     * Determines whether three consecutive points form a sharp corner that must be preserved without over-smoothing.
     * Angle calculation between vectors (p1->p2) and (p2->p3).
     */
    fun isSharpCorner(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, thresholdDegrees: Float = 60f): Boolean {
        val v1x = x2 - x1
        val v1y = y2 - y1
        val v2x = x3 - x2
        val v2y = y3 - y2

        val dot = v1x * v2x + v1y * v2y
        val mag1 = kotlin.math.sqrt((v1x * v1x + v1y * v1y).toDouble()).toFloat()
        val mag2 = kotlin.math.sqrt((v2x * v2x + v2y * v2y).toDouble()).toFloat()

        if (mag1 < 1e-4f || mag2 < 1e-4f) return false

        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0f, 1.0f)
        val angleDegrees = kotlin.math.acos(cosTheta.toDouble()).toFloat() * (180f / Math.PI.toFloat())

        return angleDegrees > thresholdDegrees
    }

    fun onPointerUpOrCancel(event: MotionEvent, pointerIndex: Int) {
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId == activeStylusPointerId) {
            isStylusActive = false
            activeStylusPointerId = -1
        }
    }

    companion object {
        const val PALM_TOUCH_THRESHOLD_PX = 120.0f
    }
}
