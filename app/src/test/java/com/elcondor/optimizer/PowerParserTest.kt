package com.elcondor.optimizer

import com.elcondor.optimizer.data.Pro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PowerParserTest {
    private val sample = """
Battery History (1% used, 12KB used of 4096KB, 80 strings using 9KB):
                    0 (15) RESET:TIME: 2026-11-25-08-12-03

  Estimated power use (mAh):
    Capacity: 5000, Computed drain: 1047, actual drain: 922-968
    Global
      screen: 425 apps: 425 duration: 4h 2m 27s 410ms
      cpu: 270 apps: 270 duration: 16h 5m 54s 15ms
      bluetooth: 1.58 apps: 1.58 duration: 51s 838ms
      mobile_radio: 151 apps: 151 duration: 9h 54m 4s 23ms
      wifi: 16.2 apps: 16.2 duration: 1h 0m 18s 66ms
      idle: 0.2
    UID 0: 52.4 fg: 0 bg: 0 fgs: 0 cached: 0 ( cpu=52.4 )
    UID 1000: 61.2 ( cpu=40.1 wakelock=2.10 )
    UID u0a196: 222 fg: 214 bg: 4.57 fgs: 3.23 cached: 0 ( screen=173 cpu=27.8 )
    UID u0a301: 88.5 fg: 10.0 bg: 70.0 fgs: 8.5 cached: 0 ( cpu=60 mobile_radio=28 )
    UID u0a301: 1.0
    UID u0a12: 0.01

  Per-app mobile ms per packet:
    UID u0a999: 500
""".trimIndent()

    @Test fun parsesAndroid14Format() {
        val r = Pro.parsePower(sample) { uid -> "app$uid" to null }
        assertNotNull(r); r!!
        assertEquals(5000.0, r.capacity!!, 0.01)
        assertEquals(1047.0, r.computedDrain!!, 0.01)
        assertEquals(listOf("screen", "cpu", "mobile_radio", "wifi", "bluetooth"), r.components.map { it.key })
        assertEquals("Pantalla", r.components.first().label)
        // u0a196 = 10196, ordenado por mAh; u0a12 (<0.05) descartado; duplicado ignorado; sección siguiente no se mezcla
        assertEquals(listOf(10196, 10301, 1000, 0), r.apps.map { it.uid })
        assertEquals(70.0, r.apps[1].bgMah!!, 0.01)
        assertEquals(null, r.apps[2].fgMah)
    }

    @Test fun oldFormatUidLowercase() {
        val r = Pro.parsePower("""
  Estimated power use (mAh):
    Capacity: 4000, Computed drain: 500, actual drain: 0
    Uid u0a50: 120
    Uid 1000: 30
""".trimIndent().prependIndent("")) { "x" to null }
        assertEquals(listOf(10050, 1000), r!!.apps.map { it.uid })
    }
}
