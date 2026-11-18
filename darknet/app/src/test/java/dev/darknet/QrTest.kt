package dev.darknet

import dev.darknet.ui.wifiQr
import org.junit.Assert.assertEquals
import org.junit.Test

class QrTest {
    @Test
    fun `wpa2, wpa3 and open networks`() {
        assertEquals("WIFI:T:WPA;S:Casa;P:clave123;;", wifiQr("Casa", "clave123", 1))
        assertEquals("WIFI:T:SAE;S:S25;P:clave123;;", wifiQr("S25", "clave123", 3))
        assertEquals("WIFI:T:nopass;S:Libre;;", wifiQr("Libre", "", 0))
    }

    @Test
    fun `escapes reserved characters`() {
        val bs = "\\"
        assertEquals("WIFI:T:WPA;S:a${bs};b;P:p${bs},w;;", wifiQr("a;b", "p,w", 1))
    }
}
