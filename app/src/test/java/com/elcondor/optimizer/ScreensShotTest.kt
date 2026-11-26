package com.elcondor.optimizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.elcondor.optimizer.ui.screens.*
import com.elcondor.optimizer.ui.theme.CondorTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renderiza cada pantalla para revisar diseño y que no falle al componerse. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-night-xxhdpi")
class ScreensShotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { CondorTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() } } }
        repeat(30) { rule.mainClock.advanceTimeBy(100); Thread.sleep(60) }
        rule.onRoot().captureRoboImage("screens/$name.png")
    }

    @Test fun home() = shot("1_home") { HomeScreen(Modifier, 0, {}, {}) }
    @Test fun battery() = shot("2_battery") { BatteryScreen(Modifier, 0) {} }
    @Test fun network() = shot("3_network") { NetworkScreen(Modifier, 0) {} }
    @Test fun cleaner() = shot("4_cleaner") { CleanerScreen(Modifier, 0) {} }
    @Test fun apps() = shot("5_apps") { AppsScreen(Modifier, 0) {} }
    @Test fun tools() = shot("6_tools") { ToolsScreen(Modifier, 0, {}, {}) }
    @Test fun perms() = shot("0_perms") { PermissionsScreen(Modifier, 0) {} }
    @Test fun fullApp() = shot("7_app") { App() }
}
