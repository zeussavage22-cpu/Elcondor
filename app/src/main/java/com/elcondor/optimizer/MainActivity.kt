package com.elcondor.optimizer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.elcondor.optimizer.data.Action
import com.elcondor.optimizer.data.AppsRepo
import com.elcondor.optimizer.data.Fmt
import com.elcondor.optimizer.data.Nav
import com.elcondor.optimizer.data.Perms
import com.elcondor.optimizer.ui.screens.*
import com.elcondor.optimizer.ui.theme.CondorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Filled.Home),
    BATTERY("Batería", Icons.Filled.BatteryChargingFull),
    NETWORK("Red", Icons.Filled.Wifi),
    CLEAN("Limpiar", Icons.Filled.CleaningServices),
    APPS("Apps", Icons.Filled.Apps),
    TOOLS("Herramientas", Icons.Filled.Home),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CondorTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    val onAction: (Action) -> Unit = { a ->
        when (a) {
            Action.BATTERY -> tab = Tab.BATTERY
            Action.NETWORK -> tab = Tab.NETWORK
            Action.CLEANER -> tab = Tab.CLEAN
            Action.APPS -> tab = Tab.APPS
            Action.USAGE_PERM -> Perms.openUsageAccess(ctx)
            Action.FILES_PERM -> Perms.openAllFiles(ctx)
            Action.BOOST -> scope.launch {
                val freed = withContext(Dispatchers.IO) { AppsRepo.boost(ctx) }
                Toast.makeText(ctx, "Optimizado: ${Fmt.bytes(freed)} de RAM liberada", Toast.LENGTH_LONG).show()
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.filter { it != Tab.TOOLS }.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            Tab.HOME -> HomeScreen(m, refresh, onAction, onTools = { tab = Tab.TOOLS })
            Tab.BATTERY -> BatteryScreen(m, refresh, onAction)
            Tab.NETWORK -> NetworkScreen(m, refresh, onAction)
            Tab.CLEAN -> CleanerScreen(m, refresh, onAction)
            Tab.APPS -> AppsScreen(m, refresh, onAction)
            Tab.TOOLS -> ToolsScreen(m, refresh, onBack = { tab = Tab.HOME })
        }
    }
}
