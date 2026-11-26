package com.elcondor.optimizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Good = Color(0xFF2EBD85)
val Warn = Color(0xFFF5A524)
val Bad = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

private val Dark = darkColorScheme(
    primary = Color(0xFF7CC4FF), secondary = Color(0xFF4FC3F7),
    background = Color(0xFF0E1116), surface = Color(0xFF0E1116),
    surfaceContainer = Color(0xFF171B22), surfaceContainerHigh = Color(0xFF1E232B),
)
private val Light = lightColorScheme(primary = Color(0xFF0B57D0), secondary = Color(0xFF0B3D91))

@Composable
fun CondorTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
