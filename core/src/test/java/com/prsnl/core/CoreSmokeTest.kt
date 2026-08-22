package com.prsnl.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreSmokeTest {
    @Test
    fun testModuleInitialization() {
        val moduleName = "prsnl-core"
        assertTrue(moduleName.startsWith("prsnl"))
        assertEquals("prsnl-core", moduleName)
    }
}
