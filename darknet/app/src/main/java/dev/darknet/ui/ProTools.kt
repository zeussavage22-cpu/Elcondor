package dev.darknet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.darknet.ShizukuLink
import dev.darknet.data.Bytes
import dev.darknet.data.ShareStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** One line of the "does everything go through DarkNet?" check. */
data class Check(val name: String, val state: String, val detail: String)

fun parseChecks(json: String): List<Check> = runCatching {
    val a = JSONArray(json)
    List(a.length()) { i -> a.getJSONObject(i).let { Check(it.optString("name"), it.optString("state"), it.optString("detail")) } }
}.getOrElse { listOf(Check("Comprobación", "fail", it.message ?: "respuesta inválida")) }

/** Engine details plus the one-tap check. */
@Composable
fun CheckCard(status: ShareStatus) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var checks by remember { mutableStateOf<List<Check>?>(null) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Motor", Modifier.weight(1f))
            Text(engineSummary(status), style = Dn.Small)
        }
        checks?.forEach { CheckLine(it) }
        Button(
            onClick = {
                running = true
                scope.launch {
                    checks = withContext(Dispatchers.IO) {
                        runCatching { parseChecks(ShizukuLink.service().diagnose()) }
                            .getOrElse { listOf(Check("Comprobación", "fail", it.message ?: "error")) }
                    }
                    running = false
                }
            },
            enabled = !running,
            colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) {
            if (running) {
                CircularProgressIndicator(Modifier.size(16.dp), color = Dn.Background, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Comprobando…", color = Dn.Background)
            } else {
                Text(if (checks == null) "Comprobar que todo pasa por DarkNet" else "Comprobar de nuevo", color = Dn.Background)
            }
        }
    }
}

private fun engineSummary(s: ShareStatus) = buildList {
    if (s.fastPath) add("TCP")
    if (s.fastPath6) add("IPv6")
    if (s.udpFast) add("UDP")
    if (s.congestion.startsWith("bbr")) add("BBR")
    if (s.mtu > 0) add("MTU ${s.mtu}")
    if (s.combined >= 2) add("${s.combined} redes")
}.joinToString(" · ").ifEmpty { "motor de respaldo" }

@Composable
private fun CheckLine(c: Check) {
    val (icon, tint) = when (c.state) {
        "ok" -> Icons.Filled.CheckCircle to Dn.Green
        "warn" -> Icons.Filled.Warning to Dn.Amber
        "fail" -> Icons.Filled.Error to Dn.Red
        else -> Icons.Filled.Info to Dn.Cyan
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(c.name, style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
            Text(c.detail, style = Dn.Small)
        }
    }
}

/** Download speed with DarkNet's engine and without it, one after the other. */
@Composable
fun CompareCard(active: Boolean) {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf("") }
    var direct by remember { mutableStateOf<JSONObject?>(null) }
    var engine by remember { mutableStateOf<JSONObject?>(null) }
    Panel {
        SectionLabel("Motor contra directo")
        Text(
            "Descarga unos segundos por los datos del teléfono y luego por el motor de DarkNet, y compara. " +
                "Gasta hasta unos 100 MB.",
            style = Dn.Small,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Directo", result(direct, phase == "direct"), Modifier.weight(1f))
            Stat("Con DarkNet", result(engine, phase == "engine"), Modifier.weight(1f))
        }
        val d = direct?.optDouble("mbps", Double.NaN) ?: Double.NaN
        val e = engine?.optDouble("mbps", Double.NaN) ?: Double.NaN
        if (!d.isNaN() && !e.isNaN() && d > 0) {
            val ratio = e / d * 100
            Text(
                when {
                    ratio >= 95 -> "El motor entrega el %.0f %% de tu velocidad directa: sin pérdida apreciable.".format(ratio)
                    ratio >= 75 -> "El motor entrega el %.0f %% de tu velocidad directa.".format(ratio)
                    else -> "El motor entrega el %.0f %%. Repite la prueba: la señal cambia mucho entre una y otra.".format(ratio)
                },
                style = Dn.Small.copy(color = if (ratio >= 75) Dn.Green else Dn.Amber),
            )
        }
        Button(
            onClick = {
                scope.launch {
                    direct = null
                    engine = null
                    phase = "direct"
                    direct = measure("direct")
                    phase = "engine"
                    engine = measure("engine")
                    phase = ""
                }
            },
            enabled = active && phase.isEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) { Text(if (active) "Comparar" else "Empieza a compartir para comparar", color = Dn.Background) }
    }
}

private suspend fun measure(mode: String): JSONObject = withContext(Dispatchers.IO) {
    runCatching { JSONObject(ShizukuLink.service().speedTest(mode)) }
        .getOrElse { JSONObject().put("error", it.message ?: "error") }
}

private fun result(r: JSONObject?, running: Boolean): String = when {
    running -> "midiendo…"
    r == null -> "–"
    r.has("error") -> "falló"
    else -> "%.0f Mbps".format(r.optDouble("mbps")) + " · " + Bytes.format(r.optLong("bytes"))
}

/** Restarts mobile data to get a fresh tower or IP, and shows what changed. */
@Composable
fun RestartDataCard() {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    Panel {
        SectionLabel("Datos móviles")
        Text("Apaga y prende la conexión móvil para tomar una antena o una IP mejor, sin modo avión. Tarda unos segundos.", style = Dn.Small)
        result?.let { r ->
            if (r.has("error")) {
                Text("No se pudo: ${r.optString("error")}", style = Dn.Small.copy(color = Dn.Red))
            } else {
                val before = r.optString("before").ifEmpty { "?" }
                val after = r.optString("after").ifEmpty { "?" }
                Text("Volvieron en %.0f s.".format(r.optDouble("seconds")), style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    if (before != after) "IP nueva: $before → $after" else "Misma IP ($after): tu operadora la mantuvo.",
                    style = Dn.Small.copy(color = Dn.Cyan),
                )
            }
        }
        Button(
            onClick = {
                running = true
                scope.launch {
                    result = withContext(Dispatchers.IO) {
                        runCatching { JSONObject(ShizukuLink.service().restartData()) }
                            .getOrElse { JSONObject().put("error", it.message ?: "error") }
                    }
                    running = false
                }
            },
            enabled = !running,
            colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) { Text(if (running) "Reiniciando…" else "Reiniciar datos móviles", color = Dn.Background) }
    }
}

/** Normal download against a video file, to see if the carrier slows one of them. */
@Composable
fun ThrottleCard() {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf("") }
    var normal by remember { mutableStateOf<JSONObject?>(null) }
    var video by remember { mutableStateOf<JSONObject?>(null) }
    Panel {
        SectionLabel("¿Tu operadora te limita?")
        Text(
            "Descarga un archivo normal y luego un video por los datos del teléfono, y compara. Gasta hasta unos 100 MB. " +
                "Si tu operadora reconoce el video por la app (YouTube, Netflix), esta prueba puede no notarlo.",
            style = Dn.Small,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Normal", result(normal, phase == "normal"), Modifier.weight(1f))
            Stat("Video", result(video, phase == "video"), Modifier.weight(1f))
        }
        if (phase.isEmpty() && normal != null) {
            val v = dev.darknet.data.Throttle.verdict(
                normal?.takeIf { !it.has("error") }?.optDouble("mbps"),
                video?.takeIf { !it.has("error") }?.optDouble("mbps"),
            )
            Text(v.text, style = Dn.Small.copy(color = if (v.warn) Dn.Amber else Dn.Green))
        }
        Button(
            onClick = {
                scope.launch {
                    normal = null
                    video = null
                    phase = "normal"
                    normal = measureUrl("https://speed.cloudflare.com/__down?bytes=12000000")
                    phase = "video"
                    video = measureUrl(VIDEO_URL)
                    phase = ""
                }
            },
            enabled = phase.isEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) { Text("Probar", color = Dn.Background) }
    }
}

private const val VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

private suspend fun measureUrl(url: String): JSONObject = withContext(Dispatchers.IO) {
    runCatching { JSONObject(ShizukuLink.service().speedTestUrl("direct", url)) }
        .getOrElse { JSONObject().put("error", it.message ?: "error") }
}
