package dev.darknet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DarkNet's palette: near-black night with a violet and cyan signal. */
object Dn {
    val Background = Color(0xFF07070E)
    val Surface = Color(0xFF111120)
    val SurfaceHigh = Color(0xFF191830)
    val Border = Color(0xFF232241)
    val Text = Color(0xFFF2F0FF)
    val Muted = Color(0xFF9C99BF)
    val Violet = Color(0xFF8B7CFF)
    val VioletSoft = Color(0xFFB9AEFF)
    val Cyan = Color(0xFF22D3EE)
    val Green = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Red = Color(0xFFF87171)

    val Title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = Text)
    val Heading = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Text)
    val Body = TextStyle(fontSize = 14.sp, color = Text)
    val Small = TextStyle(fontSize = 12.sp, color = Muted)
    val Label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Muted)
    val Value = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Text)
}

@Composable
fun DarkNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
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
