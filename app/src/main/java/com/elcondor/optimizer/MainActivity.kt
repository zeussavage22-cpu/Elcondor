package com.elcondor.optimizer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.screens.*
import com.elcondor.optimizer.ui.theme.CondorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val label: String, val icon: ImageVector, val inBar: Boolean = true) {
    HOME("Inicio", Icons.Outlined.Home),
    BATTERY("Batería", Icons.Outlined.BatteryChargingFull),
    NETWORK("Red", Icons.Outlined.Wifi),
    CLEAN("Limpiar", Icons.Outlined.CleaningServices),
    APPS("Apps", Icons.Outlined.GridView),
    TOOLS("Herramientas", Icons.Outlined.Build, false),
    PERMS("Permisos", Icons.Outlined.Shield, false),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CondorTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs = remember { Perms.prefs(ctx) }
    var tab by rememberSaveable { mutableStateOf(if (prefs.getBoolean("onboarded", false)) Tab.HOME else Tab.PERMS) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    val onAction: (Action) -> Unit = { a ->
        when (a) {
            Action.BATTERY -> tab = Tab.BATTERY
            Action.NETWORK -> tab = Tab.NETWORK
            Action.CLEANER -> tab = Tab.CLEAN
            Action.APPS -> tab = Tab.APPS
            Action.USAGE_PERM, Action.FILES_PERM -> tab = Tab.PERMS
            Action.BOOST -> scope.launch {
                val freed = withContext(Dispatchers.IO) { AppsRepo.boost(ctx) }
                Toast.makeText(ctx, "${Fmt.bytes(freed)} de RAM liberada", Toast.LENGTH_SHORT).show()
                refresh++
            }
            Action.DISPLAY -> Nav.display(ctx)
            Action.LOCATION -> Nav.location(ctx)
            Action.BLUETOOTH -> Nav.bluetooth(ctx)
            Action.NFC -> Nav.nfc(ctx)
            Action.WIFI -> Nav.wifi(ctx)
            Action.BG_LIMITS -> Nav.backgroundLimits(ctx)
            Action.DEVICE_CARE -> Nav.deviceCare(ctx)
            Action.DATA_USAGE -> Nav.dataUsage(ctx)
            Action.UPDATE -> Nav.softwareUpdate(ctx)
            Action.NONE -> {}
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 24 }) togetherWith fadeOut(tween(160))
            },
            label = "tabs",
        ) { t ->
            val m = Modifier.fillMaxSize()
            when (t) {
                Tab.HOME -> HomeScreen(m, refresh, onAction, onTools = { tab = Tab.TOOLS })
                Tab.BATTERY -> BatteryScreen(m, refresh, onAction)
                Tab.NETWORK -> NetworkScreen(m, refresh, onAction)
                Tab.CLEAN -> CleanerScreen(m, refresh, onAction)
                Tab.APPS -> AppsScreen(m, refresh, onAction)
                Tab.TOOLS -> ToolsScreen(m, refresh, onBack = { tab = Tab.HOME }, onPerms = { tab = Tab.PERMS })
                Tab.PERMS -> PermissionsScreen(m, refresh, onDone = {
                    prefs.edit().putBoolean("onboarded", true).apply(); tab = Tab.HOME
                })
            }
        }

        AnimatedVisibility(
            visible = tab.inBar,
            enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBar(tab) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); tab = it }
        }
    }
}

@Composable
private fun FloatingBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier.navigationBarsPadding().padding(bottom = 16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.filter { it.inBar }.forEach { t ->
            val sel = t == current
            Row(
                Modifier.clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(t) }
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon, t.label, Modifier.size(22.dp),
                    tint = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                if (sel) {
                    Spacer(Modifier.width(8.dp))
                    Text(t.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
