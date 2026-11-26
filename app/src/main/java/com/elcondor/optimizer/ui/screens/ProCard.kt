package com.elcondor.optimizer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.Nav
import com.elcondor.optimizer.data.Pro
import com.elcondor.optimizer.data.ProStatus
import com.elcondor.optimizer.ui.components.PillButton
import com.elcondor.optimizer.ui.components.muted
import com.elcondor.optimizer.ui.theme.Accent
import com.elcondor.optimizer.ui.theme.Good
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tarjeta para activar el Modo Pro con Shizuku, con instrucciones según el estado. */
@Composable
fun ProCard(onChanged: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    val status = Pro.status

    fun grant() {
        working = true
        scope.launch {
            val n = withContext(Dispatchers.IO) { Pro.grantAll(ctx) }
            working = false
            Toast.makeText(ctx, "Modo Pro listo: $n permisos concedidos", Toast.LENGTH_LONG).show()
            onChanged()
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainer)))
            .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Modo Pro · Shizuku", style = MaterialTheme.typography.titleMedium)
                Text(when (status) {
                    ProStatus.READY -> "Activo"
                    ProStatus.NO_PERMISSION -> "Shizuku está corriendo: falta conectar"
                    ProStatus.NOT_RUNNING -> "Shizuku instalado pero apagado"
                    ProStatus.NOT_INSTALLED -> "Recomendado: sin bloqueos de permisos"
                    ProStatus.UNSUPPORTED -> "Actualiza Shizuku"
                }, style = MaterialTheme.typography.bodySmall, color = if (status == ProStatus.READY) Good else muted())
            }
            if (status == ProStatus.READY) Box(Modifier.size(32.dp).clip(CircleShape).background(Good.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = Good, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        when (status) {
            ProStatus.READY -> {
                Text("Cóndor tiene permisos de administrador (sin root): consumo real en mAh, limpieza total de caché, detener y congelar apps, y más.",
                    style = MaterialTheme.typography.bodySmall, color = muted())
                Spacer(Modifier.height(14.dp))
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
                else FilledTonalButton(onClick = { grant() }, shape = CircleShape, modifier = Modifier.fillMaxWidth()) { Text("Volver a dar todos los permisos") }
            }
            ProStatus.NO_PERMISSION -> {
                Text("Toca Conectar y elige \"Permitir siempre\". Cóndor se dará todos los permisos automáticamente.",
                    style = MaterialTheme.typography.bodySmall, color = muted())
                Spacer(Modifier.height(14.dp))
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
                else PillButton("Conectar", Modifier.fillMaxWidth()) { Pro.requestPermission { grant() } }
            }
            ProStatus.NOT_RUNNING, ProStatus.NOT_INSTALLED, ProStatus.UNSUPPORTED -> {
                Text("Shizuku da a Cóndor permisos de ADB sin root y sin computadora. Así se salta el bloqueo de \"ajustes restringidos\".",
                    style = MaterialTheme.typography.bodySmall, color = muted())
                Spacer(Modifier.height(12.dp))
                val notInstalled = status == ProStatus.NOT_INSTALLED || status == ProStatus.UNSUPPORTED
                if (notInstalled) ProStep(1, "Instala Shizuku desde Play Store (gratis).")
                ProStep(if (notInstalled) 2 else 1, "Activa Opciones de desarrollador: Ajustes › Acerca del teléfono › Información de software › toca 7 veces \"Número de compilación\".")
                ProStep(if (notInstalled) 3 else 2, "Conéctate a un Wi-Fi y activa Opciones de desarrollador › \"Depuración inalámbrica\".")
                ProStep(if (notInstalled) 4 else 3, "Abre Shizuku › \"Emparejamiento\". Abre Depuración inalámbrica › \"Vincular con código\" y escribe el código en la notificación de Shizuku.")
                ProStep(if (notInstalled) 5 else 4, "En Shizuku toca \"Iniciar\". Vuelve aquí y toca Conectar.")
                Spacer(Modifier.height(12.dp))
                if (notInstalled) PillButton("Instalar Shizuku", Modifier.fillMaxWidth()) { Pro.installShizuku(ctx) }
                else PillButton("Abrir Shizuku", Modifier.fillMaxWidth()) { Pro.openShizuku(ctx) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { Nav.developer(ctx) }, shape = CircleShape, modifier = Modifier.fillMaxWidth()) { Text("Opciones de desarrollador") }
                Text("Después de reiniciar el teléfono hay que volver a tocar \"Iniciar\" en Shizuku. Los permisos que Cóndor ya recibió se mantienen.",
                    style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun ProStep(n: Int, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text("$n", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.width(22.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
