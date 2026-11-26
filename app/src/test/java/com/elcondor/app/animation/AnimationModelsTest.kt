package com.elcondor.app.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AnimationModelsTest {

    @Test fun `format uses dot decimal even in spanish locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("0.25", AnimationScale.format(0.25f))
            assertEquals("0.5", AnimationScale.format(0.5f))
            assertEquals("1", AnimationScale.format(1f))
            assertEquals("0", AnimationScale.format(0f))
            assertEquals("10", AnimationScale.format(10f))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `parse handles null and garbage as 1x`() {
        assertEquals(1f, AnimationScale.parse("null"))
        assertEquals(1f, AnimationScale.parse(""))
        assertEquals(1f, AnimationScale.parse("99"))
        assertEquals(0.5f, AnimationScale.parse("0.5\n"))
    }

    @Test fun `command for all chains three settings`() {
        assertEquals(
            "settings put global window_animation_scale 0.25 && " +
                "settings put global transition_animation_scale 0.25 && " +
                "settings put global animator_duration_scale 0.25",
            AnimationScale.commandForAll(0.25f),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects out of range scale`() {
        AnimationScale.commandForAll(-1f)
    }

    @Test fun `fast and uniform`() {
        assertTrue(AnimationScales(0.25f, 0.25f, 0.25f).isFast)
        assertFalse(AnimationScales(0.25f, 1f, 0.25f).isFast)
        assertEquals(0.5f, AnimationScales(0.5f, 0.5f, 0.5f).uniform)
        assertNull(AnimationScales(0.5f, 1f, 0.5f).uniform)
    }

    @Test fun `immersive commands quote the wildcard`() {
        assertEquals("settings put global policy_control 'immersive.full=*'", ImmersiveMode.FULL.command)
        assertEquals("settings delete global policy_control", ImmersiveMode.OFF.command)
    }

    @Test fun `immersive parse`() {
        assertEquals(ImmersiveMode.FULL, ImmersiveMode.parse("immersive.full=*\n"))
        assertEquals(ImmersiveMode.OFF, ImmersiveMode.parse("null"))
        assertEquals(ImmersiveMode.OFF, ImmersiveMode.parse("immersive.full=com.foo"))
    }
}
