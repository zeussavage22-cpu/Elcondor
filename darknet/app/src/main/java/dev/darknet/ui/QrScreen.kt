package dev.darknet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.darknet.ShizukuLink
import org.json.JSONObject

/** The standard Wi-Fi QR payload phone cameras understand. */
fun wifiQr(ssid: String, password: String, security: Int): String {
    fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
    val type = when {
        password.isEmpty() || security == 0 -> "nopass"
        security >= 2 -> "SAE"
        else -> "WPA"
    }
    return if (type == "nopass") "WIFI:T:nopass;S:${esc(ssid)};;" else "WIFI:T:$type;S:${esc(ssid)};P:${esc(password)};;"
}

private fun encode(text: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1))
}.getOrNull()

/** Shows a QR code others scan with their camera to join the hotspot. */
@Composable
fun QrPanel() {
    var info by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { JSONObject(ShizukuLink.service().hotspotInfo()) }
            .onSuccess { o ->
                if (o.has("error")) error = o.optString("error")
                else info = Triple(o.optString("ssid"), o.optString("password"), o.optInt("security", 1))
            }
            .onFailure { error = it.message }
    }

    Panel {
        SectionLabel("Conectarse con QR")
        val current = info
        when {
            current != null -> {
                val matrix = remember(current) { encode(wifiQr(current.first, current.second, current.third)) }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(220.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (matrix != null) QrCanvas(matrix)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(current.first, style = Dn.Heading)
                    Text(
                        if (showPassword) "Contraseña: ${current.second}" else "Toca para ver la contraseña",
                        style = Dn.Small.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(top = 2.dp).let { m ->
                            m.then(Modifier.clip(RoundedCornerShape(6.dp)))
                        },
                    )
                    androidx.compose.material3.TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Mostrar contraseña", color = Dn.VioletSoft)
                    }
                }
                Text("Abre la cámara del otro teléfono y apunta al código.", style = Dn.Small)
            }
            error != null -> Text("No se pudo leer el hotspot: $error", style = Dn.Small)
            else -> Text("Leyendo el hotspot…", style = Dn.Small)
        }
    }
}

@Composable
private fun QrCanvas(matrix: BitMatrix) {
    Canvas(Modifier.fillMaxSize()) {
        val cell = size.width / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) drawRect(Color(0xFF15132B), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
