package com.prsnl.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceUtilsTest {

    @Test
    fun testUnitsToMetersStandardPage() {
        val units = 1200f
        val meters = DistanceUtils.unitsToMeters(units, 1200f)
        assertEquals(0.21f, meters, 0.0001f)
    }

    @Test
    fun testCalculatePathLength() {
        val points = listOf(
            0f to 0f,
            3f to 4f, // 5
            6f to 8f  // +5 = 10
        )
        val length = DistanceUtils.calculatePathLength(points)
        assertEquals(10f, length, 0.0001f)
    }

    @Test
    fun testLandmarkComparison() {
        val comparisonZero = DistanceUtils.getLandmarkComparison(0f)
        assertTrue(comparisonZero.isNotEmpty())

        val comparisonTower = DistanceUtils.getLandmarkComparison(340f)
        assertTrue(comparisonTower.contains("Eiffel Tower"))
    }
}
