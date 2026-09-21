package com.prsnl.core.util

import kotlin.math.hypot

object DistanceUtils {
    /**
     * Standard A4 physical width in meters (210mm = 0.21m).
     * Assumption: A typical document page corresponds to a standard A4 physical sheet.
     */
    const val PHYSICAL_A4_WIDTH_METERS: Float = 0.21f

    /**
     * Default digital document width in abstract units (matches default Page width 1200f).
     */
    const val DEFAULT_DOCUMENT_WIDTH: Float = 1200f

    /**
     * Conversion factor from document units to meters:
     * 0.21m / 1200 units ≈ 0.000175 meters/unit (or 5714 units per meter).
     */
    const val DEFAULT_UNITS_TO_METERS: Float = PHYSICAL_A4_WIDTH_METERS / DEFAULT_DOCUMENT_WIDTH

    /**
     * Converts raw document space distance units into estimated real-world meters.
     *
     * @param units Total accumulated Euclidean length in document space units.
     * @param documentWidth Width of the page canvas in document units. Defaults to 1200f.
     */
    fun unitsToMeters(units: Float, documentWidth: Float = DEFAULT_DOCUMENT_WIDTH): Float {
        if (units <= 0f) return 0f
        val safeWidth = if (documentWidth > 0f) documentWidth else DEFAULT_DOCUMENT_WIDTH
        return units * (PHYSICAL_A4_WIDTH_METERS / safeWidth)
    }

    /**
     * Calculates Euclidean distance sum along a path of (x, y) coordinates.
     */
    fun calculatePathLength(points: List<Pair<Float, Float>>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 1 until points.size) {
            val (x1, y1) = points[i - 1]
            val (x2, y2) = points[i]
            total += hypot(x2 - x1, y2 - y1)
        }
        return total
    }

    /**
     * Returns a human-friendly visual landmark comparison based on total meters written.
     */
    fun getLandmarkComparison(meters: Float): String {
        return when {
            meters <= 0.1f -> "Your stylus journey begins with the very first stroke."
            meters < 2f -> "About as long as a student study desk."
            meters < 10f -> "Longer than a standard classroom whiteboard."
            meters < 25f -> "As long as a full-size public bus (~12m)."
            meters < 50f -> "Longer than a majestic Blue Whale (~30m)!"
            meters < 100f -> "Approaching the length of a FIFA football field (~105m)!"
            meters < 200f -> "Longer than a Boeing 747 airliner (~70m)."
            meters < 350f -> "Taller than the Eiffel Tower in Paris (~330m)!"
            meters < 500f -> "Taller than the Empire State Building roof (~381m)!"
            meters < 900f -> "Taller than the Burj Khalifa skyscraper (~828m)!"
            meters < 2000f -> "Over 1 kilometer of pure handwritten ideas!"
            meters < 5000f -> "As long as a brisk 3-mile outdoor run!"
            meters < 10000f -> "Over 5 kilometers of continuous handwritten thoughts!"
            else -> "A marathon of knowledge! You could wrap a small town in your writing."
        }
    }
}
