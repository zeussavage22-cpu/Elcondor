package dev.darknet

import android.Manifest
import androidx.compose.animation.togetherWith
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.Lock
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

class MainActivity : FragmentActivity() {

    private val prefs by lazy { Prefs(this) }

    /** Unlocked since the app came to the front (only matters with the lock on). */
    private var unlocked by mutableStateOf(false)
    private var lockMessage by mutableStateOf("")
    private var leftAt = 0L

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Shows the system fingerprint/face/PIN prompt; [onOk] runs on success. If the phone has
     * no screen lock at all, [strict] refuses (turning the lock on) and otherwise lets the
     * person in, so nobody gets locked out after removing their fingerprints.
     */
    private fun authenticate(title: String, strict: Boolean = false, onOk: () -> Unit) {
        val can = BiometricManager.from(this).canAuthenticate(authenticators)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            lockMessage = "Configura una huella, cara o PIN en los ajustes del teléfono para usar el bloqueo."
            if (strict) android.widget.Toast.makeText(this, lockMessage, android.widget.Toast.LENGTH_LONG).show() else onOk()
            return
        }
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockMessage = ""
                    onOk()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    lockMessage = errString.toString()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Usa tu huella, cara o PIN")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    override fun onStop() {
        super.onStop()
        leftAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // Lock again after 30 s in the background.
        if (leftAt > 0 && System.currentTimeMillis() - leftAt > 30_000) unlocked = false
    }

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
                // Until Shizuku is ready, look again every 2 s: a permission granted in
                // Shizuku's own screen sends DarkNet no message.
                androidx.compose.runtime.LaunchedEffect(shizuku) {
                    while (shizuku != ShizukuState.READY) {
                        kotlinx.coroutines.delay(2000)
                        ShizukuLink.refresh()
                    }
                }
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
                if (settings.lock && !unlocked) {
                    LockScreen(lockMessage) { authenticate("Desbloquear DarkNet") { unlocked = true } }
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
                            Tab.SPEED -> SpeedScreen(active = status.isActive, onRun = { lifecycleScope.launch { SpeedTest.run() } })
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
                                onBand = { v -> scope.launch { prefs.setBand(v) } },
                                onStability = { v -> scope.launch { prefs.setStabilityAlerts(v) } },
                                onBbr = { v -> scope.launch { prefs.setBbr(v) } },
                                onMtu = { v -> scope.launch { prefs.setMtu(v) } },
                                onCombine = { v -> scope.launch { prefs.setCombine(v) } },
                                onHeatLimit = { v -> scope.launch { prefs.setHeatLimit(v) } },
                                onHeatStop = { v -> scope.launch { prefs.setHeatStop(v) } },
                                onSpeedIcon = { v -> scope.launch { prefs.setSpeedIcon(v) } },
                                onVpnLock = { v -> scope.launch { prefs.setVpnLock(v) } },
                                onAutoRestart = { v -> scope.launch { prefs.setAutoRestartData(v) } },
                                onLock = { v ->
                                    // Confirm it's you before turning the lock on or off.
                                    authenticate(if (v) "Activar el bloqueo" else "Quitar el bloqueo", strict = v) {
                                        unlocked = true
                                        scope.launch { prefs.setLock(v) }
                                    }
                                },
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
        ShizukuLink.onAppResumed()
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
private fun LockScreen(message: String, onUnlock: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().background(Dn.Background).statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(androidx.compose.material.icons.Icons.Filled.Lock, null, tint = Dn.VioletSoft, modifier = Modifier.height(56.dp).width(56.dp))
        Text("DarkNet está bloqueada", style = Dn.Heading)
        if (message.isNotEmpty()) Text(message, style = Dn.Small)
        androidx.compose.material3.Button(
            onClick = onUnlock,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) { Text("Desbloquear", color = Dn.Background) }
        Text("El hotspot sigue funcionando mientras está bloqueada.", style = Dn.Small)
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
