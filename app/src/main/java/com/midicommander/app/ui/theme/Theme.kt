package com.midicommander.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Color Palette ────────────────────────────────────────────────────────────

object MidiColors {
    // Backgrounds
    val DeepBlack      = Color(0xFF080810)
    val SurfaceDark    = Color(0xFF0F0F1A)
    val SurfaceMid     = Color(0xFF1A1A2E)
    val SurfaceCard    = Color(0xFF1E1E30)
    val CardBorder     = Color(0xFF2A2A42)

    // Accents
    val PurpleGlow     = Color(0xFF7C4DFF)
    val PurpleLight    = Color(0xFFB388FF)
    val CyanAccent     = Color(0xFF00E5FF)
    val GreenActive    = Color(0xFF00E676)
    val OrangeWarm     = Color(0xFFFF6D00)
    val RedAlert       = Color(0xFFFF1744)
    val PinkAccent     = Color(0xFFFF4081)
    val AmberWarm      = Color(0xFFFFAB00)

    // Text
    val TextPrimary    = Color(0xFFF0F0FF)
    val TextSecondary  = Color(0xFF9090B0)
    val TextMuted      = Color(0xFF505070)

    // Button colors pool
    val ButtonColors = listOf(
        Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFF00E676),
        Color(0xFFFF6D00), Color(0xFFFF4081), Color(0xFFFFAB00),
        Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFFF6E40),
        Color(0xFFEA80FC), Color(0xFFCCFF90), Color(0xFFFFD740)
    )
}

// ─── Typography ───────────────────────────────────────────────────────────────

private val MidiTypography = Typography()

// ─── Dark Color Scheme ────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary         = MidiColors.PurpleGlow,
    onPrimary       = Color.White,
    primaryContainer = MidiColors.SurfaceMid,
    secondary       = MidiColors.CyanAccent,
    onSecondary     = Color.Black,
    background      = MidiColors.DeepBlack,
    onBackground    = MidiColors.TextPrimary,
    surface         = MidiColors.SurfaceDark,
    onSurface       = MidiColors.TextPrimary,
    surfaceVariant  = MidiColors.SurfaceCard,
    onSurfaceVariant = MidiColors.TextSecondary,
    error           = MidiColors.RedAlert,
    outline         = MidiColors.CardBorder
)

private val LightColorScheme = lightColorScheme(
    primary         = Color(0xFF5C35CC),
    onPrimary       = Color.White,
    secondary       = Color(0xFF0097A7),
    background      = Color(0xFFF5F5FF),
    surface         = Color(0xFFFFFFFF),
    onBackground    = Color(0xFF1A1A2E),
    onSurface       = Color(0xFF1A1A2E)
)

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun MidiCommanderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MidiTypography,
        content     = content
    )
}
