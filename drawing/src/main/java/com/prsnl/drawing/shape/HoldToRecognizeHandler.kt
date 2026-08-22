package com.prsnl.drawing.shape

import android.os.Handler
import android.os.Looper
import com.prsnl.document.model.StrokePoint
import kotlin.math.hypot

class HoldToRecognizeHandler(
    private val config: ShapeRecognitionConfig = ShapeRecognitionConfig(),
    private val onHoldRecognized: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastPoint: StrokePoint? = null
    private var isHolding = false

    private val holdRunnable = Runnable {
        isHolding = true
        onHoldRecognized()
    }

    fun onPointAdded(point: StrokePoint) {
        val prev = lastPoint
        lastPoint = point

        if (prev == null) {
            scheduleHoldTimer()
            return
        }

        val dt = point.timestampMs - prev.timestampMs
        val dx = point.x - prev.x
        val dy = point.y - prev.y
        val dist = hypot(dx, dy)
        val velocity = if (dt > 0) dist / dt else 0f

        if (velocity < config.velocityStillThreshold) {
            // Pointer is motionless, start/keep hold timer running
            if (!isHolding) {
                scheduleHoldTimer()
            }
        } else {
            // Pointer moved, cancel hold timer
            cancelTimer()
        }
    }

    fun cancelTimer() {
        handler.removeCallbacks(holdRunnable)
        isHolding = false
        lastPoint = null
    }

    private fun scheduleHoldTimer() {
        handler.removeCallbacks(holdRunnable)
        handler.postDelayed(holdRunnable, config.holdDurationMs)
    }
}
