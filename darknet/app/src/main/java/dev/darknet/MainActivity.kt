package dev.darknet

import android.Manifest
import androidx.compose.animation.togetherWith
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.darknet.data.Prefs
import dev.darknet.data.Settings
import dev.darknet.ui.DarkNetTheme
import dev.darknet.ui.DevicesScreen
import dev.darknet.ui.Dn
import dev.darknet.ui.HomeScreen
import dev.darknet.ui.SettingsScreen
import dev.darknet.ui.SpeedScreen
import kotlinx.coroutines.launch

class DarkNetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuLink.init(this)
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home),
    ACTIVITY("Actividad", Icons.Filled.BarChart),
    SPEED("Velocidad", Icons.Filled.Speed),
    DEVICES("Equipos", Icons.Filled.Devices),
    SETTINGS("Ajustes", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_PHONE_STATE), 1)
        reconnect()

        setContent {
            val settings by prefs.settings.collectAsState(initial = null)
            val loaded = settings ?: return@setContent
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = when (loaded.theme) { "light" -> false; "system" -> systemDark; else -> true }
            val accent = runCatching { dev.darknet.ui.Accent.valueOf(loaded.accent) }.getOrDefault(dev.darknet.ui.Accent.VIOLET)
            dev.darknet.ui.Dn.palette = dev.darknet.ui.Palette.of(accent, dark)
            androidx.compose.runtime.SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            DarkNetTheme {
                val status by ShareService.status.collectAsState()
                val shizuku by ShizukuLink.state.collectAsState()
                val settings = loaded
                val scope0 = rememberCoroutineScope()
                if (!settings.onboarded) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Dn.Background).statusBarsPadding()) {
                        dev.darknet.ui.Onboarding(
                            shizuku = shizuku,
                            onRequestShizuku = ShizukuLink::requestPermission,
                            onDone = { scope0.launch { prefs.setOnboarded(true) } },
                        )
                    }
                    return@DarkNetTheme
                }
                var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
                var log by remember { mutableStateOf<List<String>?>(null) }
                val scope = rememberCoroutineScope()

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Dn.Glow, Dn.Background, Dn.Background)))
                        .statusBarsPadding(),
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (androidx.compose.animation.slideInHorizontally { it / 6 * dir } + androidx.compose.animation.fadeIn()) togetherWith
                                (androidx.compose.animation.slideOutHorizontally { -it / 6 * dir } + androidx.compose.animation.fadeOut())
                        },
                        modifier = Modifier.weight(1f),
                        label = "tabs",
                    ) { tab ->
                        when (tab) {
                            Tab.HOME -> HomeScreen(
                                status = status,
                                shizuku = shizuku,
                                fastEngineEnabled = settings.fastEngine,
                                quotaGb = settings.quotaGb,
                                quotaResetDay = settings.quotaResetDay,
                                onToggle = {
                                    if (status.isActive) ShareService.stop(this@MainActivity) else ShareService.start(this@MainActivity)
                                },
                                onFixShizuku = ShizukuLink::requestPermission,
                            )
                            Tab.ACTIVITY -> dev.darknet.ui.ActivityScreen()
                            Tab.SPEED -> SpeedScreen(onRun = { lifecycleScope.launch { SpeedTest.run() } })
                            Tab.DEVICES -> DevicesScreen(status, shizuku == ShizukuState.READY)
                            Tab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                shizuku = shizuku,
                                log = log,
                                onFastEngine = { v -> scope.launch { prefs.setFastEngine(v) } },
                                onUsb = { v -> scope.launch { prefs.setUsb(v) } },
                                onQuota = { v -> scope.launch { prefs.setQuota(v) } },
                                onResetDay = { v -> scope.launch { prefs.setQuotaResetDay(v) } },
                                onQuotaStop = { v -> scope.launch { prefs.setQuotaStop(v) } },
                                onIdle = { v -> scope.launch { prefs.setIdleMinutes(v) } },
                                onAlerts = { v -> scope.launch { prefs.setDeviceAlerts(v) } },
                                onAccent = { v -> scope.launch { prefs.setAccent(v) } },
                                onTheme = { v -> scope.launch { prefs.setTheme(v) } },
                                onDns = { v -> scope.launch { prefs.setDns(v) } },
                                onShizuku = ShizukuLink::requestPermission,
                                onShowLog = { scope.launch { log = ShareService.log() } },
                                onHideLog = { log = null },
                            )
                        }
                    }
                    BottomBar(tab) { tab = it }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuLink.refresh()
    }

    /** If sharing was left running (the app was closed), pick its status back up. */
    private fun reconnect() {
        lifecycleScope.launch {
            if (ShizukuLink.state.value != ShizukuState.READY || ShareService.status.value.isActive) return@launch
            val active = runCatching {
                org.json.JSONObject(ShizukuLink.service().status()).optString("state") == "active"
            }.getOrDefault(false)
            if (active) ShareService.start(this@MainActivity)
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(Dn.Surface)
            .border(1.dp, Dn.Border, shape)
            .height(64.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Dn.Violet else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tab.icon, tab.label, tint = if (selected) Dn.Background else Dn.Muted)
                if (selected) {
                    Spacer(Modifier.width(6.dp))
                    Text(tab.label, color = Dn.Background, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
