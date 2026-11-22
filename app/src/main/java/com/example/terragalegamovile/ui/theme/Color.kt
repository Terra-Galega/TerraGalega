package com.example.terragalegamovile.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Modo claro u oscuro de la app. Los colores de abajo que cambian con el modo son
// propiedades con get(): al cambiar `dark`, todo lo que los usa se vuelve a dibujar.
object ThemeState {
    // null = como el teléfono; "light" o "dark" = lo que eligió en la app
    var mode by mutableStateOf<String?>(null)
    // Modo del teléfono (MainActivity lo lee al abrir; Android recrea la pantalla si cambia)
    var systemDark by mutableStateOf(false)

    val dark: Boolean
        get() = when (mode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }

    // Cambia entre claro y oscuro y lo guarda en el teléfono
    fun toggle() {
        mode = if (dark) "light" else "dark"
        com.example.terragalegamovile.data.AppStorage.saveTheme(mode)
    }
}

private val isDark: Boolean get() = ThemeState.dark

// ---------- Colores de marca (iguales en los dos modos, como en la web: los botones
// de color no cambian) ----------
val Terra = Color(0xFFB2571F)
val Gold = Color(0xFFD99A21)
val Burgundy = Color(0xFF8A2222)
val tab = Color(0xFF622316)
val Forest = Color(0xFF566034)
val Available = Color(0xFF6E8532)

// Siempre claros u oscuros (texto sobre fotos o sobre fondos que no cambian)
val CreamFixed = Color(0xFFF5F2EB)
val InkFixed = Color(0xFF2C2C2C)

// ---------- Colores que cambian con el modo (paleta oscura de la web: styles.scss) ----------

// Fondo de página: crema / #1b1916
val Cream: Color get() = if (isDark) Color(0xFF1B1916) else CreamFixed

// Texto principal: carbón / crema
val Charcoal: Color get() = if (isDark) CreamFixed else InkFixed

// Superficies que en claro son blancas (tarjetas, campos, ventanas): blanco / #2f2b26
val Paper: Color get() = if (isDark) Color(0xFF2F2B26) else Color.White

// Opción seleccionada (idioma, pestañas de inicio de sesión) y botón de Google:
// blanco en claro; gris clarito en oscuro para que no deslumbre
val Selected: Color get() = if (isDark) Color(0xFFCFCBC3) else Color.White

// Fondo suave de burbujas y campos del chat: crema / #2f2b26
val SoftSurface: Color get() = if (isDark) Color(0xFF2F2B26) else CreamFixed

// Paso pendiente de la barra de progreso del pedido: #ece9e1 / #23201c
val TrackOff: Color get() = if (isDark) Color(0xFF23201C) else Color(0xFFECE9E1)

// Texto de error: granate / granate claro (el oscuro no se lee sobre fondo oscuro)
val ErrorText: Color get() = if (isDark) Color(0xFFE88A8A) else Burgundy
val ErrorSurface: Color get() = if (isDark) Color(0x26E88A8A) else Color(0xFFFBEAEA)

// Verde de "pagado" y terracota como texto
val OkText: Color get() = if (isDark) Color(0xFFA3B987) else Color(0xFF4A6B3A)
val TerraText: Color get() = if (isDark) Color(0xFFE58E57) else Terra

// Textos y bordes con transparencia, como en la web
val TextSoft: Color get() = Charcoal.copy(alpha = 0.68f)
val TextLabel: Color get() = Charcoal.copy(alpha = 0.40f)
val BorderSoft: Color get() = Charcoal.copy(alpha = 0.12f)
