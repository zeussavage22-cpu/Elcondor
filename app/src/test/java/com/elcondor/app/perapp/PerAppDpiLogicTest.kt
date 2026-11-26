package com.elcondor.app.perapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerAppDpiLogicTest {

    private val rules = mapOf("com.instagram.android" to 480, "com.whatsapp" to 520)
    private val ime = setOf("com.samsung.android.honeyboard")

    @Test fun `app with rule applies its dpi`() {
        assertEquals(
            DpiDecision.ApplyRule(480),
            PerAppDpiLogic.decide("com.instagram.android", rules, ime, ruleModeActive = false),
        )
    }

    @Test fun `switching between rule apps applies the new rule`() {
        assertEquals(
            DpiDecision.ApplyRule(520),
            PerAppDpiLogic.decide("com.whatsapp", rules, ime, ruleModeActive = true),
        )
    }

    @Test fun `leaving a rule app restores baseline`() {
        assertEquals(
            DpiDecision.RestoreBaseline,
            PerAppDpiLogic.decide("com.sec.android.app.launcher", rules, ime, ruleModeActive = true),
        )
    }

    @Test fun `app without rule while in baseline does nothing`() {
        assertEquals(DpiDecision.Ignore, PerAppDpiLogic.decide("com.foo", rules, ime, ruleModeActive = false))
    }

    @Test fun `keyboard and system ui never trigger a change`() {
        assertEquals(DpiDecision.Ignore, PerAppDpiLogic.decide("com.samsung.android.honeyboard", rules, ime, true))
        assertEquals(DpiDecision.Ignore, PerAppDpiLogic.decide("com.android.systemui", rules, ime, true))
    }

    @Test fun `baseline command`() {
        assertEquals("wm density 560", PerAppDpiLogic.baselineCommand(560))
        assertEquals("wm density reset", PerAppDpiLogic.baselineCommand(null))
    }

    private val me = "com.elcondor.app/com.elcondor.app.perapp.ForegroundAppService"
    private val other = "com.other/com.other.Svc"

    @Test fun `enable appends and keeps other services`() {
        assertEquals(listOf(other, me), PerAppDpiLogic.editAccessibilityList(other, me, enable = true))
        assertEquals(listOf(me), PerAppDpiLogic.editAccessibilityList("null", me, enable = true))
        // Sin duplicados si ya estaba.
        assertEquals(listOf(other, me), PerAppDpiLogic.editAccessibilityList("$me:$other", me, enable = true))
    }

    @Test fun `disable removes long and short form`() {
        val short = "com.elcondor.app/.perapp.ForegroundAppService"
        assertEquals(listOf(other), PerAppDpiLogic.editAccessibilityList("$other:$short", me, enable = false))
        assertEquals(emptyList<String>(), PerAppDpiLogic.editAccessibilityList(me, me, enable = false))
    }

    @Test fun `refuses to rewrite a list with unexpected entries`() {
        assertNull(PerAppDpiLogic.editAccessibilityList("com.x/.A;reboot", me, enable = true))
    }

    @Test fun `accessibility command quoting`() {
        assertEquals(
            "settings put secure enabled_accessibility_services 'a.b/c.D:$me' && settings put secure accessibility_enabled 1",
            PerAppDpiLogic.accessibilityListCommand(listOf("a.b/c.D", me), enableAccessibility = true),
        )
        assertEquals(
            "settings delete secure enabled_accessibility_services",
            PerAppDpiLogic.accessibilityListCommand(emptyList(), enableAccessibility = false),
        )
    }
}
