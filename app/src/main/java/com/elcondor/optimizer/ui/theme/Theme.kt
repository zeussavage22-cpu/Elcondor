package com.elcondor.optimizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.elcondor.optimizer.R

val Good = Color(0xFF4ADE80)
val Warn = Color(0xFFFBBF24)
val Bad = Color(0xFFFB7185)
val Accent = Color(0xFF8FA8FF)
val Info = Accent

val Manrope = FontFamily(
    Font(R.font.manrope_extralight, FontWeight.ExtraLight),
    Font(R.font.manrope_light, FontWeight.Light),
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

private val Dark = darkColorScheme(
    primary = Accent, onPrimary = Color(0xFF0A0F24),
    primaryContainer = Color(0xFF1C2340), onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFB7C4FF),
    background = Color.Black, onBackground = Color(0xFFF2F2F4),
    surface = Color.Black, onSurface = Color(0xFFF2F2F4),
    onSurfaceVariant = Color(0xFF8E8E96),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF1B1B1F),
    surfaceContainerHighest = Color(0xFF26262B),
    outline = Color(0xFF2E2E34), outlineVariant = Color(0xFF1F1F24),
)

private val Light = lightColorScheme(
    primary = Color(0xFF3550D6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E8FF), onPrimaryContainer = Color(0xFF0E1A5C),
    background = Color(0xFFF6F6F8), onBackground = Color(0xFF111114),
    surface = Color(0xFFF6F6F8), onSurface = Color(0xFF111114),
    onSurfaceVariant = Color(0xFF6B6B73),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEDEDF1),
    surfaceContainerHighest = Color(0xFFE2E2E8),
    outline = Color(0xFFDADAE0), outlineVariant = Color(0xFFEAEAEE),
)

private fun t(size: Int, w: FontWeight, line: Int = (size * 1.3).toInt(), spacing: Float = 0f) =
    TextStyle(fontFamily = Manrope, fontWeight = w, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private val Type = Typography(
    displayLarge = t(76, FontWeight.ExtraLight, 80, -2f),
    displayMedium = t(52, FontWeight.Light, 58, -1.5f),
    displaySmall = t(38, FontWeight.Light, 44, -1f),
    headlineLarge = t(32, FontWeight.SemiBold, 38, -0.8f),
    headlineMedium = t(26, FontWeight.SemiBold, 32, -0.5f),
    headlineSmall = t(22, FontWeight.SemiBold, 28, -0.3f),
    titleLarge = t(20, FontWeight.SemiBold, 26, -0.2f),
    titleMedium = t(16, FontWeight.SemiBold, 22),
    titleSmall = t(14, FontWeight.SemiBold, 20),
    bodyLarge = t(16, FontWeight.Medium, 22),
    bodyMedium = t(14, FontWeight.Medium, 20),
    bodySmall = t(13, FontWeight.Normal, 18),
    labelLarge = t(14, FontWeight.SemiBold, 20),
    labelMedium = t(12, FontWeight.Medium, 16),
    labelSmall = t(11, FontWeight.SemiBold, 14, 0.8f),
)

@Composable
fun CondorTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) Dark else Light
    MaterialTheme(colorScheme = scheme, typography = Type) {
        // Color de texto base para todo lo que no está dentro de un Surface
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
    }
}
