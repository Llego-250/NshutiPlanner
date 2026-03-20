package com.example.nshutiplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = LavenderDark,
    onPrimary = SurfaceLight,
    primaryContainer = LavenderLight,
    onPrimaryContainer = TextPrimary,
    secondary = TealDark,
    onSecondary = SurfaceLight,
    secondaryContainer = TealLight,
    onSecondaryContainer = TextPrimary,
    tertiary = PeachDark,
    tertiaryContainer = PeachLight,
    background = SurfaceLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Lavender
)

private val DarkColors = darkColorScheme(
    primary = Lavender,
    onPrimary = SurfaceDark,
    primaryContainer = CardDark,
    onPrimaryContainer = TextOnDark,
    secondary = Teal,
    onSecondary = SurfaceDark,
    secondaryContainer = CardDark,
    onSecondaryContainer = TextOnDark,
    tertiary = Peach,
    tertiaryContainer = CardDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    onSurfaceVariant = Color(0xFFB0A8CC),
    outline = Color(0xFF4A4260)
)

@Composable
fun NshutiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = NshutiTypography, content = content)
}
