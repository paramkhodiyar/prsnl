package com.prsnl.drawing.sampler

import android.view.MotionEvent
import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.filter.InputFilter

class PointSampler(
    private val inputFilter: InputFilter = InputFilter()
) {

    fun extractPoints(event: MotionEvent, pointerIndex: Int, strokeStartTimeMs: Long): List<StrokePoint> {
        val points = mutableListOf<StrokePoint>()
        if (pointerIndex < 0 || pointerIndex >= event.pointerCount) return emptyList()

        // Safely extract historical motion samples
        try {
            val historySize = event.historySize
            for (h in 0 until historySize) {
                val hx = event.getHistoricalX(pointerIndex, h)
                val hy = event.getHistoricalY(pointerIndex, h)
                val hp = inputFilter.normalizePressure(event.getHistoricalPressure(pointerIndex, h))
                val ht = event.getHistoricalEventTime(h) - strokeStartTimeMs

                points.add(
                    StrokePoint(
                        x = hx,
                        y = hy,
                        pressure = hp,
                        timestampMs = ht
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore historical sampling bounds mismatch on hardware fallback
        }

        // Safely extract current motion sample
        try {
            val cx = event.getX(pointerIndex)
            val cy = event.getY(pointerIndex)
            val cp = inputFilter.normalizePressure(event.getPressure(pointerIndex))
            val ct = event.eventTime - strokeStartTimeMs

            points.add(
                StrokePoint(
                    x = cx,
                    y = cy,
                    pressure = cp,
                    timestampMs = ct
                )
            )
        } catch (_: Exception) {}

        return points
    }
}
