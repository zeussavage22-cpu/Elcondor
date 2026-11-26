package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.elcondor.optimizer.ui.theme.Bad
import kotlinx.coroutines.isActive

@Composable
fun ToolsScreen(modifier: Modifier, refresh: Int, onBack: () -> Unit, onPerms: () -> Unit) {
    val ctx = LocalContext.current
    var id by remember { mutableStateOf(DeviceInfo.identity(ctx)) }
    var freqs by remember { mutableStateOf(DeviceInfo.cpuFreqsMhz()) }
    var mem by remember { mutableStateOf(DeviceInfo.memory(ctx)) }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    val media = remember(refresh) { Perms.hasMedia(ctx) }
    val files = remember(refresh) { Perms.hasAllFiles() }
    LaunchedEffect(refresh) {
        while (isActive) { id = DeviceInfo.identity(ctx); freqs = DeviceInfo.cpuFreqsMhz(); mem = DeviceInfo.memory(ctx); delay(2000) }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Atrás") }
            }
            Header("Herramientas", id.model)
        }
        item {
            SectionLabel("Modo Pro")
            ProCard()
        }
        if (Pro.canTweak || Pro.ready) item { ProTools() }
        item {
            SectionLabel("Permisos")
            Block(padding = 12.dp) {
                val n = listOf(usage, media, files).count { it }
                NavRow(Icons.Outlined.Shield, "Configurar permisos", "$n de 3 activos", if (n == 3) "Listo" else "Revisar",
                    if (n == 3) Good else Warn, onPerms)
            }
        }
        item {
            SectionLabel("Dispositivo")
            Block(padding = 18.dp) {
                KeyValue("Android", id.android)
                id.oneUi?.let { KeyValue("One UI", it) }
                id.soc?.let { KeyValue("Procesador", it) }
                KeyValue("RAM", "${Fmt.bytes(mem.usedBytes)} / ${Fmt.bytes(mem.totalBytes)}")
                KeyValue("Encendido hace", Fmt.duration(id.uptimeMs))
                KeyValue("Temperatura interna", DeviceInfo.thermalLabel(id.thermalStatus))
                id.thermalHeadroom?.let { KeyValue("Margen térmico", "${((1 - it).coerceIn(0f, 1f) * 100).toInt()} %") }
            }
        }
        if (freqs.any { it != null }) item {
            SectionLabel("CPU en vivo")
            Block(padding = 18.dp) {
                freqs.forEachIndexed { i, f -> if (f != null) BarRow("Núcleo $i", "$f MHz", f / 4500f, MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            SectionLabel("Atajos Samsung")
            Block(padding = 12.dp) {
                NavRow(Icons.Outlined.HealthAndSafety, "Device Care", "Optimización oficial") { Nav.deviceCare(ctx) }
                NavRow(Icons.Outlined.BatteryChargingFull, "Batería", "Protección y ahorro") { Nav.batterySettings(ctx) }
                NavRow(Icons.Outlined.Bedtime, "Apps en suspensión", "Límites en segundo plano") { Nav.backgroundLimits(ctx) }
                NavRow(Icons.Outlined.DataUsage, "Uso de datos", null) { Nav.dataUsage(ctx) }
                NavRow(Icons.Outlined.Dns, "DNS privado", "Más ajustes de conexión") { Nav.privateDns(ctx) }
                NavRow(Icons.Outlined.DisplaySettings, "Pantalla", null) { Nav.display(ctx) }
                NavRow(Icons.Outlined.SystemUpdate, "Actualizar software", null) { Nav.softwareUpdate(ctx) }
                NavRow(Icons.Outlined.Security, "Seguridad y privacidad", "Auto Blocker, permisos") { Nav.security(ctx) }
                NavRow(Icons.Outlined.DeveloperMode, "Opciones de desarrollador", "Animaciones 0.5x") { Nav.developer(ctx) }
            }
        }
        item {
            SectionLabel("Trucos")
            Block(padding = 18.dp) {
                listOf(
                    "Opciones de desarrollador › escalas de animación a 0.5x: se siente el doble de rápido.",
                    "Device Care › Optimización automática con reinicio semanal.",
                    "Agrega el botón Optimizar a tus Ajustes rápidos: desliza la cortina › lápiz › arrástralo.",
                ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = muted(), modifier = Modifier.padding(vertical = 5.dp)) }
            }
        }
        item {
            Text("Cóndor Care 2.0 · Todo se procesa en tu teléfono.", style = MaterialTheme.typography.labelMedium, color = muted(),
                modifier = Modifier.padding(top = 20.dp, start = 4.dp))
        }
    }
}

@Composable
private fun ProTools() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var anim by remember { mutableStateOf(Pro.animationScale(ctx)) }
    var busy by remember { mutableStateOf<String?>(null) }
    var health by remember { mutableStateOf<Int?>(null) }
    var confirmReboot by remember { mutableStateOf(false) }
    LaunchedEffect(Pro.status) { health = withContext(Dispatchers.IO) { Pro.batteryHealthPercent() } }

    fun run(label: String, done: (Any?) -> String, block: () -> Any?) {
        busy = label
        scope.launch {
            val r = withContext(Dispatchers.IO) { block() }
            busy = null
            Toast.makeText(ctx, done(r), Toast.LENGTH_LONG).show()
        }
    }

    SectionLabel("Herramientas Pro")
    Block(padding = 16.dp) {
        Text("Velocidad de animaciones", style = MaterialTheme.typography.bodyLarge)
        Text("Más rápido = el teléfono se siente más ágil", style = MaterialTheme.typography.bodySmall, color = muted())
        Spacer(Modifier.height(10.dp))
        Segmented(listOf(0f to "Sin", 0.5f to "0.5x", 0.75f to "0.75x", 1f to "Normal"), listOf(0f, 0.5f, 0.75f, 1f).minByOrNull { kotlin.math.abs(it - anim) } ?: 1f) { v ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) { Pro.setAnimationScale(ctx, v) }
                if (ok) anim = v else Toast.makeText(ctx, "Necesita el Modo Pro", Toast.LENGTH_SHORT).show()
            }
        }
        Spacer(Modifier.height(10.dp))
        busy?.let {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = muted())
        }
        if (Pro.ready) {
            NavRow(Icons.Outlined.DeleteSweep, "Borrar caché de todas las apps", "Sin perder datos ni sesiones") {
                if (busy == null) run("Borrando caché…", { "${Fmt.bytes(it as Long)} liberados" }) { Pro.trimCaches(ctx) }
            }
            NavRow(Icons.Outlined.Speed, "Compilar apps para más velocidad", "Lo que Android hace de noche. Tarda unos minutos") {
                if (busy == null) run("Optimizando apps… puedes seguir usando el teléfono", { if (it == true) "Apps optimizadas" else "Terminado" }) { Pro.optimizeApps() }
            }
            NavRow(Icons.Outlined.RestartAlt, "Reiniciar teléfono", "Limpia la memoria por completo") { confirmReboot = true }
        }
        health?.let { KeyValue("Capacidad real de la batería", "$it %", when { it >= 85 -> Good; it >= 75 -> Warn; else -> Bad }) }
    }

    if (confirmReboot) AlertDialog(
        onDismissRequest = { confirmReboot = false },
        title = { Text("¿Reiniciar ahora?") },
        text = { Text("Después del reinicio abre Shizuku y toca Iniciar para volver a activar el Modo Pro.") },
        confirmButton = { TextButton(onClick = { confirmReboot = false; scope.launch(Dispatchers.IO) { Pro.reboot() } }) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = { confirmReboot = false }) { Text("Cancelar") } },
    )
}
