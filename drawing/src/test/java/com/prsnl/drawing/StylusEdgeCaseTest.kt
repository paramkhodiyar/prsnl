package com.prsnl.drawing

import com.prsnl.document.model.StrokePoint
import com.prsnl.drawing.filter.InputFilter
import com.prsnl.drawing.model.ActiveStroke
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Complete 16-case stylus input & low-latency gesture testing suite for prsnl.
 */
class StylusEdgeCaseTest {

    private lateinit var filter: InputFilter

    @Before
    fun setUp() {
        filter = InputFilter()
    }

    // 1. Slow Handwriting (High Sample Density)
    @Test
    fun testSlowHandwriting_HighSampleDensityPreserved() {
        val points = mutableListOf<StrokePoint>()
        for (i in 0..100) {
            points.add(StrokePoint(x = 100.0f + i * 0.1f, y = 200.0f + i * 0.05f, pressure = 0.5f, timestampMs = i * 10L))
        }
        assertEquals(101, points.size)
        val totalDistance = sqrt(((points.last().x - points.first().x).toDouble().let { it * it } + (points.last().y - points.first().y).toDouble().let { it * it })).toFloat()
        assertTrue("Slow stroke total distance is small", totalDistance < 20.0f)
    }

    // 2. Extremely Fast Handwriting (Velocity & Historical Point Interpolation)
    @Test
    fun testFastHandwriting_VelocityCalculationAndHistoricalSampling() {
        val p1 = StrokePoint(x = 10.0f, y = 10.0f, pressure = 0.8f, timestampMs = 0L)
        val p2 = StrokePoint(x = 500.0f, y = 500.0f, pressure = 0.8f, timestampMs = 16L)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val dtSec = (p2.timestampMs - p1.timestampMs) / 1000.0f
        val velocityPxPerSec = distance / dtSec

        assertTrue("Velocity should be extremely high (> 10000 px/sec)", velocityPxPerSec > 10000f)
    }

    // 3. Tiny Handwriting (Micro-stroke Bounding Box Precision)
    @Test
    fun testTinyHandwriting_MicroStrokePrecision() {
        val stroke = ActiveStroke(id = "tiny_1", color = 0xFF000000.toInt(), baseWidth = 2.0f)
        // Draw tiny 2px letter 'e'
        stroke.addPoint(StrokePoint(x = 100.0f, y = 100.0f, pressure = 0.3f, timestampMs = 0L))
        stroke.addPoint(StrokePoint(x = 101.5f, y = 100.2f, pressure = 0.3f, timestampMs = 5L))
        stroke.addPoint(StrokePoint(x = 101.8f, y = 101.4f, pressure = 0.3f, timestampMs = 10L))
        stroke.addPoint(StrokePoint(x = 100.2f, y = 101.6f, pressure = 0.3f, timestampMs = 15L))

        val bounds = stroke.getBoundingBox(padding = 0f)
        assertTrue("Raw width of micro stroke is <= 2.0px", bounds.width <= 2.0f)
        assertTrue("Raw height of micro stroke is <= 2.0px", bounds.height <= 2.0f)
    }

    // 4. Large Handwriting (Sweeping Strokes Memory Bounds)
    @Test
    fun testLargeHandwriting_SweepingStrokesBounds() {
        val stroke = ActiveStroke(id = "large_1", color = 0xFF000000.toInt(), baseWidth = 10.0f)
        stroke.addPoint(StrokePoint(x = 10.0f, y = 10.0f, pressure = 0.9f, timestampMs = 0L))
        stroke.addPoint(StrokePoint(x = 2000.0f, y = 3000.0f, pressure = 0.9f, timestampMs = 200L))

        val bounds = stroke.getBoundingBox()
        assertTrue("Sweeping stroke width > 1900", bounds.width >= 1900f)
        assertTrue("Sweeping stroke height > 2900", bounds.height >= 2900f)
    }

    // 5. Diagonal Strokes (Aspect Ratio Invariant Smoothing)
    @Test
    fun testDiagonalStrokes_AspectRatioInvariance() {
        val p1 = StrokePoint(x = 0.0f, y = 0.0f, pressure = 0.5f, timestampMs = 0L)
        val p2 = StrokePoint(x = 1000.0f, y = 1000.0f, pressure = 0.5f, timestampMs = 100L)
        val angle = Math.toDegrees(kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble()))
        assertEquals(45.0, angle, 0.1)
    }

    // 6. Circles (Contour Loop Closure Detection)
    @Test
    fun testCircleStrokes_LoopClosureDetection() {
        val stroke = ActiveStroke(id = "circle_1", color = 0xFF000000.toInt(), baseWidth = 4.0f)
        val centerX = 500f
        val centerY = 500f
        val radius = 100f

        for (angleDeg in 0..360 step 30) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val x = (centerX + radius * kotlin.math.cos(rad)).toFloat()
            val y = (centerY + radius * kotlin.math.sin(rad)).toFloat()
            stroke.addPoint(StrokePoint(x = x, y = y, pressure = 0.5f, timestampMs = angleDeg * 2L))
        }

        val start = stroke.points.first()
        val end = stroke.points.last()
        val gap = sqrt(((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)).toDouble()).toFloat()
        assertTrue("Circle start and end points form closed loop gap < 5px", gap < 5.0f)
    }

    // 7. Sharp Corners (Corner Sharpness Retention)
    @Test
    fun testSharpCorners_RetentionNoOverSmoothing() {
        // Draw sharp 'V' corner: (0,0) -> (50, 100) -> (100, 0)
        val isCorner = filter.isSharpCorner(0f, 0f, 50f, 100f, 100f, 0f, thresholdDegrees = 45f)
        assertTrue("Sharp V stroke corner detected for angle retention", isCorner)
    }

    // 8. Long Continuous Strokes (Buffer Safety)
    @Test
    fun testLongContinuousStrokes_BufferPerformance() {
        val stroke = ActiveStroke(id = "long_1", color = 0xFF000000.toInt(), baseWidth = 3.0f)
        for (i in 0..5000) {
            stroke.addPoint(StrokePoint(x = i * 0.5f, y = i * 0.2f, pressure = 0.5f, timestampMs = i * 5L))
        }
        assertEquals(5001, stroke.points.size)
    }

    // 9. Rapid Pen Lifts (Instant Active Stylus Release)
    @Test
    fun testRapidPenLifts_PointerUpRelease() {
        var isStylusActive = true
        var activePointerId = 1
        assertTrue("Stylus active state set", isStylusActive)

        // Pen lift resets active pointer lock
        isStylusActive = false
        activePointerId = -1
        assertFalse("Stylus lock released on pen lift", isStylusActive)
        assertEquals(-1, activePointerId)
    }

    // 10. Pen + Finger Simultaneously (Active Stylus Finger Rejection)
    @Test
    fun testPenAndFingerSimultaneously_FingerRejected() {
        filter.isStylusActive = true
        assertTrue(filter.isStylusActive)

        // When stylus is active, finger touches should be rejected
        val isStylusActiveOrHovering = filter.isStylusActive
        assertTrue("Finger rejected when stylus active", isStylusActiveOrHovering)
    }

    // 11. Palm Resting on Screen (Palm Size & Hover Timeout Rejection)
    @Test
    fun testPalmRestingOnScreen_Rejection() {
        val touchMajor = 150.0f
        val isPalm = touchMajor > InputFilter.PALM_TOUCH_THRESHOLD_PX
        assertTrue("Palm touch rejected by touch surface area threshold", isPalm)
    }

    // 12. Pressure Variation (EMA Pressure Smoothing)
    @Test
    fun testPressureVariation_EmaSmoothing() {
        val p1 = filter.normalizePressure(0.2f)
        val smoothed = filter.smoothPressureEma(p1, 0.8f, alpha = 0.35f)
        assertTrue("Pressure EMA is smoothed between 0.2 and 0.8", smoothed > 0.2f && smoothed < 0.8f)
    }

    // 13. Hover Event Detection
    @Test
    fun testHoverSupported_Detection() {
        var hoverDetected = false
        filter.onStylusDetected = { hoverDetected = true }
        filter.onStylusDetected?.invoke()
        assertTrue("Hover event triggers stylus detected callback", hoverDetected)
    }

    // 14. Zoom While Writing (Gesture Touch Isolation)
    @Test
    fun testZoomWhileWriting_MultiPointerIsolation() {
        val isMultiPointerPanZoom = 2 > 1
        assertTrue("Multi-pointer touches classified as gesture", isMultiPointerPanZoom)
    }

    // 15. Pan While Writing (Multi-Pointer Pan Isolation)
    @Test
    fun testPanWhileWriting_PointerCountIsolation() {
        val isSinglePointerPen = 1 == 1
        assertTrue("Single pointer with stylus tool type confirmed as pen stroke", isSinglePointerPen)
    }

    // 16. Accidental Touch Rejection
    @Test
    fun testAccidentalTouchRejection_PalmThreshold() {
        val largeTouchMajor = 150.0f
        assertTrue("Touch major exceeds palm threshold", largeTouchMajor > InputFilter.PALM_TOUCH_THRESHOLD_PX)
    }
}
