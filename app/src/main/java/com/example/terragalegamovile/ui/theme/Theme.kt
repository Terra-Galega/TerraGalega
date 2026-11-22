package com.example.terragalegamovile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TerraColorScheme = lightColorScheme(
    primary = Terra,
    onPrimary = Color.White,
    secondary = Forest,
    onSecondaryFixedVariant  = Available,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = InkFixed,
    background = CreamFixed,
    onBackground = InkFixed,
    surface = Color.White,
    onSurface = InkFixed,
    surfaceVariant = Color(0xFFECE9E1),
    onSurfaceVariant = InkFixed.copy(alpha = 0.68f),
    outline = InkFixed.copy(alpha = 0.12f),
    error = Burgundy,
    onError = Color.White
)

// Modo oscuro (misma paleta que la web en oscuro)
private val TerraDarkColorScheme = darkColorScheme(
    primary = Terra,
    onPrimary = Color.White,
    secondary = Forest,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = InkFixed,
    background = Color(0xFF1B1916),
    onBackground = CreamFixed,
    surface = Color(0xFF262320),
    onSurface = CreamFixed,
    surfaceVariant = Color(0xFF2F2B26),
    onSurfaceVariant = CreamFixed.copy(alpha = 0.76f),
    outline = CreamFixed.copy(alpha = 0.14f),
    error = Color(0xFFE88A8A),
    onError = InkFixed
)

@Composable
fun TerraGalegaMovileTheme(content: @Composable () -> Unit) {
    val dark = ThemeState.dark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Íconos de la barra de estado: oscuros sobre crema, claros en modo oscuro
                val bars = WindowCompat.getInsetsController(window, view)
                bars.isAppearanceLightStatusBars = !dark
                bars.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) TerraDarkColorScheme else TerraColorScheme,
        typography = Typography,
        content = content
    )
}