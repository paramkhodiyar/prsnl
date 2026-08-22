package com.prsnl.drawing

import com.prsnl.document.model.Background
import com.prsnl.drawing.render.BackgroundRenderer
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackgroundRendererTest {

    @Test
    fun testBackgroundRendererInstantiation() {
        val renderer = BackgroundRenderer()
        assertNotNull(renderer)

        val bg = Background(
            type = Background.Type.GRID,
            lineSpacing = 40f,
            colorLight = 0xFFFFFFFF.toInt(),
            colorDark = 0xFF181818.toInt()
        )
        assertNotNull(bg)
    }
}
