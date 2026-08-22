package com.prsnl.drawing

import com.prsnl.drawing.filter.InputFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFilterTest {

    @Test
    fun testPressureNormalization() {
        val filter = InputFilter()
        assertEquals(0.0f, filter.normalizePressure(-0.5f), 0.001f)
        assertEquals(0.5f, filter.normalizePressure(0.5f), 0.001f)
        assertEquals(1.0f, filter.normalizePressure(1.5f), 0.001f)
    }

    @Test
    fun testPalmRejectionStateManagement() {
        val filter = InputFilter()
        assertTrue(filter.activeStylusPointerId == -1)
    }
}
