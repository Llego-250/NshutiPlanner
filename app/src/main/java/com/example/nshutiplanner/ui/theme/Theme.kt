package com.example.nshutiplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Cyan500,
    onSecondary = Indigo900,
    secondaryContainer = Cyan100,
    onSecondaryContainer = Indigo900,
    tertiary = Coral500,
    onTertiary = Color.White,
    tertiaryContainer = Coral100,
    onTertiaryContainer = Indigo900,
    background = SurfaceLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Indigo300,
    error = Color(0xFFB00020)
)

private val DarkColors = darkColorScheme(
    primary = Indigo300,
    onPrimary = Indigo900,
    primaryContainer = CardDark,
    onPrimaryContainer = Indigo100,
    secondary = Cyan300,
    onSecondary = Indigo900,
    secondaryContainer = CardDark,
    onSecondaryContainer = Cyan100,
    tertiary = Coral300,
    onTertiary = Indigo900,
    tertiaryContainer = CardDark,
    onTertiaryContainer = Coral100,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    onSurfaceVariant = TextSubDark,
    outline = Indigo700,
    error = Color(0xFFCF6679)
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
