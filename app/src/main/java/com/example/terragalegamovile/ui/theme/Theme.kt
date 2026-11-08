package com.example.terragalegamovile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TerraColorScheme = lightColorScheme(
    primary = Terra,
    onPrimary = Color.White,
    secondary = Forest,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = Charcoal,
    background = Cream,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFECE9E1),
    onSurfaceVariant = TextSoft,
    outline = BorderSoft,
    error = Burgundy,
    onError = Color.White
)

@Composable
fun TerraGalegaMovileTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Íconos oscuros en la barra de estado, porque el fondo es crema
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }
    MaterialTheme(colorScheme = TerraColorScheme, typography = Typography, content = content)
}