package com.elcondor.app.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayModelsTest {

    @Test fun `density without override`() {
        val r = DisplayParsers.parseDensity("Physical density: 420\n")!!
        assertEquals(420, r.physical)
        assertNull(r.override)
        assertEquals(420, r.effective)
    }

    @Test fun `density with override`() {
        val r = DisplayParsers.parseDensity("Physical density: 420\nOverride density: 380\n")!!
        assertEquals(380, r.override)
        assertEquals(380, r.effective)
    }

    @Test fun `density garbage returns null`() {
        assertNull(DisplayParsers.parseDensity("Error: something"))
    }

    @Test fun `size with and without override`() {
        val plain = DisplayParsers.parseSize("Physical size: 1080x2400")!!
        assertEquals(Resolution(1080, 2400), plain.physical)
        assertNull(plain.override)

        val over = DisplayParsers.parseSize("Physical size: 1080x2400\nOverride size: 900x2000")!!
        assertEquals(Resolution(900, 2000), over.effective)
    }

    @Test fun `resolution formats for wm size`() {
        assertEquals("1080x2400", Resolution(1080, 2400).toString())
    }

    @Test fun `scaled keeps even pixels`() {
        val s = Resolution(1080, 2400).scaled(0.75f)
        assertEquals(Resolution(810, 1800), s)
        assertEquals(0, Resolution(1081, 2401).scaled(0.67f).width % 2)
    }

    @Test fun `density limits`() {
        assertNull(DisplayLimits.validateDensity(400))
        assertNotNull(DisplayLimits.validateDensity(50))
        assertNotNull(DisplayLimits.validateDensity(2000))
    }

    @Test fun `size validation`() {
        val panel = Resolution(1080, 2400)
        assertNull(DisplayLimits.validateSize(Resolution(810, 1800), panel))
        assertNotNull("demasiado pequeño", DisplayLimits.validateSize(Resolution(300, 600), panel))
        assertNotNull("fuera de escala", DisplayLimits.validateSize(Resolution(2000, 4000), panel))
        assertNotNull("orientación invertida", DisplayLimits.validateSize(Resolution(2400, 1080), panel))
        assertNotNull("proporción deformada", DisplayLimits.validateSize(Resolution(1080, 1300), panel))
    }
}
