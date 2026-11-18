package dev.darknet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** An accent the user can pick: the main color and its lighter partner. */
enum class Accent(val label: String, val main: Color, val soft: Color, val partner: Color) {
    VIOLET("Violeta", Color(0xFF8B7CFF), Color(0xFFB9AEFF), Color(0xFF22D3EE)),
    CYAN("Cian", Color(0xFF22B8D6), Color(0xFF7FE3F5), Color(0xFF8B7CFF)),
    GREEN("Verde", Color(0xFF22B573), Color(0xFF7DE3B4), Color(0xFF22D3EE)),
    PINK("Rosa", Color(0xFFE0569B), Color(0xFFF5A3CB), Color(0xFF8B7CFF)),
    ORANGE("Naranja", Color(0xFFF08A24), Color(0xFFF8C089), Color(0xFFE0569B)),
}

/** All colors of one look. Dark is the default; light is optional. */
data class Palette(
    val isDark: Boolean,
    val accent: Accent,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val glow: Color,
) {
    companion object {
        fun of(accent: Accent, dark: Boolean) = if (dark) {
            Palette(true, accent, Color(0xFF07070E), Color(0xFF111120), Color(0xFF191830), Color(0xFF232241),
                Color(0xFFF2F0FF), Color(0xFF9C99BF), Color(0xFF120F2A))
        } else {
            Palette(false, accent, Color(0xFFF6F5FC), Color(0xFFFFFFFF), Color(0xFFEFEDFA), Color(0xFFE1DEF0),
                Color(0xFF15132B), Color(0xFF5D5A7E), Color(0xFFE9E5FF))
        }
    }
}

/**
 * DarkNet's colors and text styles. They read the current [palette], a
 * Compose state, so changing the theme recolors every screen at once.
 */
object Dn {
    var palette by mutableStateOf(Palette.of(Accent.VIOLET, dark = true))

    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceHigh get() = palette.surfaceHigh
    val Border get() = palette.border
    val Text get() = palette.text
    val Muted get() = palette.muted
    val Glow get() = palette.glow
    val Violet get() = palette.accent.main
    val VioletSoft get() = if (palette.isDark) palette.accent.soft else palette.accent.main
    val Cyan get() = palette.accent.partner
    val Green get() = Color(0xFF34D399)
    val Amber get() = Color(0xFFFBBF24)
    val Red get() = Color(0xFFF87171)

    val Title get() = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = Text)
    val Heading get() = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Text)
    val Body get() = TextStyle(fontSize = 14.sp, color = Text)
    val Small get() = TextStyle(fontSize = 12.sp, color = Muted)
    val Label get() = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Muted)
    val Value get() = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Text)
}

@Composable
fun DarkNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = (if (Dn.palette.isDark) darkColorScheme() else lightColorScheme()).copy(
            primary = Dn.Violet,
            onPrimary = Dn.Background,
            secondary = Dn.Cyan,
            background = Dn.Background,
            surface = Dn.Surface,
            onSurface = Dn.Text,
            onBackground = Dn.Text,
            surfaceVariant = Dn.SurfaceHigh,
            onSurfaceVariant = Dn.Muted,
            outline = Dn.Border,
        ),
        content = content,
    )
}

/** The card every section of DarkNet sits on. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Dn.Surface)
            .border(1.dp, Dn.Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Dn.Label, modifier = modifier)
}
