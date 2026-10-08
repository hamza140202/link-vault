package com.linkvault.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = IndigoPrimaryLight,
    onPrimaryContainer = IndigoPrimaryDark,
    background = Slate50,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Slate100,
    onSurface = Slate900,
    onSurfaceVariant = Slate600,
    outline = Slate200
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = IndigoPrimaryDark,
    onPrimaryContainer = IndigoPrimaryLight,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onSurface = Slate50,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

@Composable
fun LinkVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
