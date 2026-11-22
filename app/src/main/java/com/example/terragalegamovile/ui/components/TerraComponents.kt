package com.example.terragalegamovile.ui.components

import com.example.terragalegamovile.ui.theme.Selected

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.data.t

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.ui.theme.BorderSoft
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.NotoSerif
import com.example.terragalegamovile.ui.theme.PinyonScript
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.SourceSans
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.example.terragalegamovile.ui.theme.Cream
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.example.terragalegamovile.ui.theme.Forest
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.terragalegamovile.R
import com.example.terragalegamovile.data.NotificationStore
import com.example.terragalegamovile.ui.theme.Gold

// Logo "Terra" (Pinyon Script) + "GALEGA" (serifa), como en la navbar de la web
@Composable
fun TerraLogo(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    terraColor: Color = Terra,
    galegaColor: Color = Charcoal
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Terra",
            fontFamily = PinyonScript,
            fontSize = (30 * scale).sp,
            lineHeight = (24 * scale).sp,
            color = terraColor
        )
        Text(
            "GALEGA",
            fontFamily = NotoSerif,
            fontSize = (18 * scale).sp,
            letterSpacing = 1.sp,
            color = galegaColor,
            modifier = Modifier.offset(y = (-8).dp)        )
    }
}

// Tarjeta de vidrio (.glass-card): blanco translúcido, borde claro, sombra suave
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Charcoal.copy(alpha = 0.08f),
                spotColor = Charcoal.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(Paper.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape)
            .padding(contentPadding),
        content = content
    )
}

enum class ButtonStyle { Primary, Outline, Sage, Danger }

// Botón redondeado como .btn de la web
@Composable
fun TerraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    compact: Boolean = false      // <-- NUEVO: botón más pequeño
) {
    val shape = RoundedCornerShape(50)

    val background = when (style) {
        ButtonStyle.Primary -> Modifier.background(Brush.linearGradient(listOf(Terra, Burgundy)), shape)
        ButtonStyle.Sage -> Modifier.background(Forest, shape)
        else -> Modifier
    }
    val border = when (style) {
        ButtonStyle.Outline -> BorderStroke(1.dp, Charcoal.copy(alpha = 0.2f))
        ButtonStyle.Danger -> BorderStroke(1.dp, ErrorText.copy(alpha = 0.35f))
        else -> null
    }
    val textColor = when (style) {
        ButtonStyle.Primary, ButtonStyle.Sage -> Color.White
        ButtonStyle.Outline -> Charcoal.copy(alpha = 0.7f)
        ButtonStyle.Danger -> ErrorText
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.6f)
            .then(
                if (style == ButtonStyle.Primary && enabled)
                    Modifier.shadow(10.dp, shape, ambientColor = Terra.copy(alpha = 0.3f), spotColor = Terra.copy(alpha = 0.3f))
                else Modifier
            )
            .clip(shape)
            .then(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 18.dp else 32.dp,
                vertical = if (compact) 9.dp else 14.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontFamily = SourceSans,
            fontWeight = FontWeight.Medium,
            fontSize = if (compact) 13.sp else 16.sp,
            // Los botones pequeños van en una sola línea; los grandes pueden partirse centrados
            maxLines = if (compact) 1 else Int.MAX_VALUE,
            softWrap = !compact,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Campo de texto como .input-field: etiqueta pequeña arriba, borde que se vuelve terracota al enfocar
@Composable
fun TerraTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = TextLabel
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = Charcoal.copy(alpha = 0.35f)) },
            shape = RoundedCornerShape(50),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Paper.copy(alpha = 0.7f),
                unfocusedContainerColor = Paper.copy(alpha = 0.7f),
                focusedBorderColor = Terra,
                unfocusedBorderColor = BorderSoft,
                focusedTextColor = Charcoal,
                unfocusedTextColor = Charcoal,
                cursorColor = Terra
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Campo de contraseña con el ojo para ver u ocultar lo que se escribe (como en la web)
@Composable
fun TerraPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••"
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = TextLabel
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = Charcoal.copy(alpha = 0.35f)) },
            shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None
            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { visible = !visible }
                        .buttonDescription(if (visible) t("Ocultar contraseña") else t("Mostrar contraseña"))
                ) {
                    EyeIcon(open = !visible, color = Charcoal.copy(alpha = 0.55f))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Paper.copy(alpha = 0.7f),
                unfocusedContainerColor = Paper.copy(alpha = 0.7f),
                focusedBorderColor = Terra,
                unfocusedBorderColor = BorderSoft,
                focusedTextColor = Charcoal,
                unfocusedTextColor = Charcoal,
                cursorColor = Terra
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun Modifier.buttonDescription(description: String): Modifier =
    this.then(
        Modifier.semantics {
            contentDescription = description
            role = androidx.compose.ui.semantics.Role.Button
        }
    )

// Ojo dibujado a mano (abierto, o tachado cuando la contraseña se ve)
@Composable
private fun EyeIcon(open: Boolean, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        val eye = Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.02f, w * 0.94f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.98f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(eye, color, style = stroke)
        drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
        if (!open) {
            drawLine(color, Offset(w * 0.15f, h * 0.15f), Offset(w * 0.85f, h * 0.85f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        }
    }
}

// Botones a su medida: cada botón mide lo que pide su texto y va centrado. Si hay
// varios uno encima de otro, todos toman el ancho del más largo. Dentro, los
// botones usan Modifier.fillMaxWidth() (llenan solo este bloque, no la pantalla).
@Composable
fun ButtonStack(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                .widthIn(min = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

// Interruptor de modo oscuro: luna en modo claro, sol en modo oscuro (mismos trazos
// de la web). Cambia toda la app al instante y queda guardado en el teléfono.
private val MOON_PATH = androidx.compose.ui.graphics.vector.PathParser()
    .parsePathString("M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z").toPath()

@Composable
fun DarkModeToggle(modifier: Modifier = Modifier) {
    val dark = com.example.terragalegamovile.ui.theme.ThemeState.dark
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Charcoal.copy(alpha = 0.06f))
            .clickable { com.example.terragalegamovile.ui.theme.ThemeState.toggle() }
            .semantics { contentDescription = if (dark) t("Activar modo claro") else t("Activar modo oscuro") }
    ) {
        Canvas(Modifier.size(20.dp)) {
            val s = size.minDimension / 24f
            val stroke = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (dark) {
                // Sol
                drawCircle(Gold, radius = 4.5f * s, center = Offset(12f * s, 12f * s), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = Offset(12f * s, 12f * s)
                    drawLine(
                        Gold,
                        Offset(c.x + (8f * s * Math.cos(a)).toFloat(), c.y + (8f * s * Math.sin(a)).toFloat()),
                        Offset(c.x + (10.5f * s * Math.cos(a)).toFloat(), c.y + (10.5f * s * Math.sin(a)).toFloat()),
                        1.8f * s, StrokeCap.Round
                    )
                }
            } else {
                withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                    drawPath(MOON_PATH, Charcoal, style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

// Selector de idioma: Español / English / Català. Cambia toda la app al instante
// y queda guardado en el teléfono.
@Composable
fun LanguagePicker(modifier: Modifier = Modifier, compact: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Charcoal.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        com.example.terragalegamovile.data.I18n.languages.forEach { (code, name) ->
            val active = com.example.terragalegamovile.data.I18n.lang == code
            Text(
                // Compacto: ES / EN / CA (para que quepa junto a un título)
                if (compact) code.uppercase() else name,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) Terra else Charcoal.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(shape)
                    .background(if (active) Selected else Color.Transparent)
                    .clickable { com.example.terragalegamovile.data.I18n.select(code) }
                    .semantics {
                        selected = active
                        if (compact) contentDescription = name
                    }
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 7.dp)
            )
        }
    }
}

// Aviso de error como el de la web: caja suave granate, con ícono y, si se puede,
// un botón para reintentar. Se usa en lugar de un texto rojo suelto.
@Composable
fun ErrorNotice(
    text: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ErrorText.copy(alpha = 0.08f))
            .border(1.dp, ErrorText.copy(alpha = 0.18f), shape)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(ErrorText.copy(alpha = 0.14f))
        ) {
            Text("!", color = ErrorText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = ErrorText,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
        if (onRetry != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                t("Reintentar"),
                color = ErrorText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRetry)
                    .border(1.dp, Burgundy.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// Etiqueta redondeada de estado (como "Pedido cancelado" en la web)
@Composable
fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// Dato con etiqueta pequeña arriba y valor debajo
@Composable
fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = TextLabel
        )
        Text(value, fontSize = 16.sp)
    }
}

// PIN en cuatro casillas
@Composable
fun PinDigits(pin: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        pin.forEach { digit ->
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Paper)
                    .border(1.dp, Terra.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    digit.toString(),
                    fontFamily = SourceSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Terra
                )
            }
        }
    }
}

// PIN que se escribe en casillas (mismo aspecto que PinDigits)
@Composable
fun PinInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= length && it.all(Char::isDigit)) onValueChange(it) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                repeat(length) { i ->
                    val digit = value.getOrNull(i)
                    val active = focused && i == value.length.coerceAtMost(length - 1)
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 64.dp)
                            .clip(shape)
                            .background(Paper)
                            .border(
                                if (active) 2.dp else 1.dp,
                                if (active) Terra else Terra.copy(alpha = 0.3f),
                                shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit != null) {
                            Text(
                                digit.toString(),
                                fontFamily = SourceSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 30.sp,
                                color = Terra
                            )
                        }
                    }
                }
            }
        }
    )
}



// Abre el perfil del cliente desde la barra de arriba (lo asigna AppNav)
object ProfileNav {
    var open: (() -> Unit)? = null
}

// Abre la bandeja de notificaciones desde la barra de arriba (lo asigna AppNav)
object NotificationsNav {
    var open: (() -> Unit)? = null
}

// Abre el asistente virtual desde la barra de arriba (lo asigna AppNav)
object ChatNav {
    var open: (() -> Unit)? = null
}

// Campanita con el número de notificaciones sin leer
@Composable
fun BellButton(count: Int, onClick: () -> Unit) {
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .semantics { contentDescription = if (count > 0) t("Notificaciones, {} sin leer", count) else t("Notificaciones") }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            WebBellIcon(color = Terra, modifier = Modifier.size(26.dp))
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Burgundy),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (count > 9) "9+" else "$count",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    // Fuente normal del sistema (no la del tema)
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    lineHeight = 10.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }
}

// Campana de la navbar de la web (mismo trazo SVG, cuadrícula de 24, línea 1.8)
private val BELL_PATH = androidx.compose.ui.graphics.vector.PathParser()
    .parsePathString(
        "M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2a2 2 0 01-.6 1.4L4 17h5m6 0a3 3 0 11-6 0m6 0H9"
    )
    .toPath()

@Composable
fun WebBellIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(
                BELL_PATH,
                color,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// Asistente virtual: solo el ícono de mensaje con signo de pregunta, sin fondo
@Composable
fun ChatButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = if (com.example.terragalegamovile.ui.SupportStore.hasUnread)
                    t("Asistente virtual, tienes mensajes nuevos del restaurante") else t("Asistente virtual")
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ChatIcon(color = Terra, modifier = Modifier.size(28.dp))
        // Mensajes nuevos del restaurante
        if (com.example.terragalegamovile.ui.SupportStore.hasUnread) {
            com.example.terragalegamovile.ui.UnreadDot(
                Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp)
            )
        }
    }
}

// Globo de conversación con un signo de pregunta (ayuda), dibujado a mano
@Composable
fun ChatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bubble = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.12f)
            quadraticTo(w * 0.94f, h * 0.12f, w * 0.94f, h * 0.28f)
            lineTo(w * 0.94f, h * 0.60f)
            quadraticTo(w * 0.94f, h * 0.76f, w * 0.78f, h * 0.76f)
            lineTo(w * 0.44f, h * 0.76f)
            lineTo(w * 0.24f, h * 0.92f)
            lineTo(w * 0.26f, h * 0.76f)
            lineTo(w * 0.22f, h * 0.76f)
            quadraticTo(w * 0.06f, h * 0.76f, w * 0.06f, h * 0.60f)
            lineTo(w * 0.06f, h * 0.28f)
            quadraticTo(w * 0.06f, h * 0.12f, w * 0.22f, h * 0.12f)
            close()
        }
        drawPath(bubble, color, style = stroke)
        // Signo de pregunta
        val q = Path().apply {
            moveTo(w * 0.40f, h * 0.34f)
            quadraticTo(w * 0.40f, h * 0.24f, w * 0.50f, h * 0.24f)
            quadraticTo(w * 0.61f, h * 0.24f, w * 0.61f, h * 0.34f)
            quadraticTo(w * 0.61f, h * 0.41f, w * 0.50f, h * 0.45f)
            lineTo(w * 0.50f, h * 0.51f)
        }
        drawPath(q, color, style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.62f))
    }
}

// Inicial del usuario en su círculo, con "Mi perfil" debajo
@Composable
private fun ProfileChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Terra, Burgundy))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontFamily = Playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            t("Mi perfil"),
            fontFamily = SourceSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Charcoal.copy(alpha = 0.75f),
            maxLines = 1
        )
    }
}
// Barra superior: estilo de la navbar de la web cuando está en modo vidrio
// Barra superior: réplica de la navbar de la web en estado "stayed" (vidrio esmerilado)
// Contenedor de pantalla: el contenido ocupa TODO (también bajo la barra) y la barra flota encima
// desenfocando lo que pasa por detrás, como la navbar de la web.
@Composable
fun TerraScaffold(
    role: String,
    name: String,
    onLogout: () -> Unit,
    cartCount: Int? = null,
    onCart: (() -> Unit)? = null,
    bottomBar: (@Composable (HazeState) -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val blur by animateDpAsState(if (FilterUi.open) 5.dp else 0.dp, label = "blur")
    val dim by animateFloatAsState(if (FilterUi.open) 0.16f else 0f, label = "dim")
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topArea = statusTop + 66.dp
    val fadeEnd = statusTop + 100.dp
    val solidUntil = ((statusTop + 62.dp) / fadeEnd).coerceIn(0f, 1f)
    // El color se lee aquí (no dentro de remember) para que la barra cambie al instante
    // al pasar entre modo claro y oscuro
    val pageColor = Cream
    val fadeStops = remember(solidUntil, pageColor) {
        buildList {
            add(0f to pageColor)
            add(solidUntil to pageColor)
            val steps = 10
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val alpha = 1f - (t * t * (3f - 2f * t))
                add((solidUntil + (1f - solidUntil) * t) to pageColor.copy(alpha = alpha))
            }
        }.toTypedArray()
    }
    var bottomHeight by remember { mutableStateOf(0.dp) }
    // El asistente virtual solo existe para el cliente (el perfil está en la barra de abajo)
    val onChat = if (role == "Cliente") ChatNav.open else null
    // La campanita sale en el inicio del cliente (la izquierda está libre, el carrito va abajo)
    // y en todas las pantallas del mesero (avisos de cocina)
    val onBell = if ((role == "Cliente" && bottomBar != null) || role == "Mesero") NotificationsNav.open else null


    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .hazeSource(hazeState)
                .blur(blur, BlurredEdgeTreatment.Rectangle)
        ) {
            content(PaddingValues(top = topArea, bottom = if (imeOpen) 0.dp else bottomHeight))        }

        // Difuminado crema: sólido detrás del logo y se desvanece suave hacia el contenido
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(fadeEnd)
                .background(Brush.verticalGradient(*fadeStops))
        )

        // Logo centrado
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop + 14.dp)
        ) {
            TerraLogo(
                scale = 0.8f,
                terraColor = Terra,
                galegaColor = Charcoal
            )
        }

        // Izquierda: carrito, solo en pantallas sin barra inferior
        if (bottomBar == null && onCart != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = statusTop + 10.dp)
            ) {
                CartButton(count = cartCount ?: 0, onClick = onCart)
            }
        }

        if (onBell != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = statusTop + 10.dp)
            ) {
                BellButton(count = NotificationStore.unread, onClick = onBell)
            }
        }

        // Derecha: asistente virtual del cliente o, en otros roles, el botón de salir
        if (onChat != null) {
            ChatButton(
                onClick = onChat,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = statusTop + 10.dp)
            )
        } else if (bottomBar == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = statusTop + 10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Paper)
                    .border(1.dp, Burgundy, CircleShape)
                    .semantics { contentDescription = t("Cerrar sesión") }
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                LogoutIcon(bracket = Burgundy, arrow = Terra, modifier = Modifier.size(22.dp))
            }
        }

        if (bottomBar != null && !imeOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomHeight = with(density) { it.height.toDp() } }
            ) {
                bottomBar(hazeState)
            }
        }

        if (dim > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
    }
}


// Ícono de salir: puerta + flecha, dibujado en dos tonos
@Composable
fun LogoutIcon(bracket: Color, arrow: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Puerta (corchete)
        val door = Path().apply {
            moveTo(w * 0.42f, h * 0.14f)
            lineTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.18f, h * 0.86f)
            lineTo(w * 0.42f, h * 0.86f)
        }
        drawPath(door, bracket, style = stroke)

        // Flecha
        val arrowPath = Path().apply {
            moveTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.50f)
            moveTo(w * 0.68f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.68f, h * 0.70f)
        }
        drawPath(arrowPath, arrow, style = stroke)
    }
}

// Botón redondo del carrito con insignia de cantidad (como el de la navbar de la web)
@Composable
fun CartButton(count: Int, onClick: () -> Unit) {
    // Igual que el carrito de la navbar de la web: solo el ícono, sin fondo ni borde
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .semantics { contentDescription = t("Carrito") }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            WebCartIcon(color = Terra, modifier = Modifier.size(26.dp))
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(Burgundy),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (count > 99) "99+" else "$count",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// Carrito de la navbar de la web (mismo trazo SVG, cuadrícula de 24, línea 1.8)
private val CART_PATH = androidx.compose.ui.graphics.vector.PathParser()
    .parsePathString(
        "M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17" +
            "m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
    )
    .toPath()

@Composable
fun WebCartIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(CART_PATH, color, style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun CartIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.04f, h * 0.08f)
            lineTo(w * 0.20f, h * 0.08f)
            lineTo(w * 0.34f, h * 0.60f)
            lineTo(w * 0.84f, h * 0.60f)
            lineTo(w * 0.96f, h * 0.26f)
            lineTo(w * 0.24f, h * 0.26f)
        }
        drawPath(path, color, style = stroke)
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.40f, h * 0.84f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.80f, h * 0.84f))
    }
}



// Barra inferior flotante tipo píldora, de vidrio, solo con íconos.
// selected: 0 = Carta, 1 = Órdenes, -1 = ninguno
@Composable
fun ClientBottomBar(
    hazeState: HazeState,
    selected: Int,
    cartCount: Int,
    onMenu: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onLogout: () -> Unit,
    // Se lee al tocar (no al dibujar): al abrir la app ProfileNav.open aún no está
    // asignado y la opción no aparecía hasta navegar
    onProfile: () -> Unit = { ProfileNav.open?.invoke() }
) {
    val pill = RoundedCornerShape(50)
    // Se lee fuera del bloque del desenfoque: así la barra cambia al instante con el modo oscuro
    val glassColor = Cream
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp)
            .shadow(
                16.dp, pill,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(pill)
            .hazeEffect(state = hazeState) {
                blurRadius = 40.dp
                noiseFactor = 0.02f
                backgroundColor = glassColor
                tints = listOf(HazeTint(Forest.copy(alpha = 0.55f)))
            }
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.04f))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.15f))
                ),
                pill
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(selected == 0, "Carta", onMenu) {
            Image(
                painter = painterResource(
                    if (selected == 0) R.drawable.nav_home_on else R.drawable.nav_home_off
                ),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
        }
        NavItem(false, t("Carrito"), onCart, badge = cartCount) { NavIcon(1, it) }
        NavItem(selected == 1, "Órdenes", onOrders) { NavIcon(2, it) }
        NavItem(selected == 2, "Perfil", onProfile) { NavIcon(3, it) }

    }
}

// Negro grisáceo del ícono y texto seleccionados
private val NavActive = Color(0xFF3B3B38)

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    badge: Int = 0,
    icon: @Composable (Color) -> Unit
) {
    val contentColor = if (selected) NavActive else Color.White.copy(alpha = 0.8f)

    // La casilla completa es el área de toque; el resaltado va solo alrededor del contenido
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.Center) {
                icon(contentColor)
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-4).dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Terra)
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badge > 9) "9+" else "$badge",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )
                    }
                }
            }
            Text(
                label,
                fontFamily = SourceSans,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
// Íconos de la barra: 0 = carta, 1 = carrito, 2 = órdenes, 3 = perfil
@Composable
private fun NavIcon(kind: Int, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            0 -> { // carta con tenedor y cuchillo
                drawRoundRect(
                    color, topLeft = Offset(w * 0.20f, h * 0.08f), size = Size(w * 0.60f, h * 0.84f),
                    cornerRadius = CornerRadius(w * 0.10f), style = stroke
                )
                drawLine(color, Offset(w * 0.38f, h * 0.28f), Offset(w * 0.38f, h * 0.72f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * 0.31f, h * 0.26f), Offset(w * 0.31f, h * 0.40f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * 0.45f, h * 0.26f), Offset(w * 0.45f, h * 0.40f), sw, StrokeCap.Round)
                drawLine(color, Offset(w * 0.63f, h * 0.28f), Offset(w * 0.63f, h * 0.72f), sw, StrokeCap.Round)
            }
            1 -> { // carrito
                val cart = Path().apply {
                    moveTo(w * 0.06f, h * 0.14f)
                    lineTo(w * 0.20f, h * 0.14f)
                    lineTo(w * 0.32f, h * 0.62f)
                    lineTo(w * 0.80f, h * 0.62f)
                    lineTo(w * 0.92f, h * 0.28f)
                    lineTo(w * 0.24f, h * 0.28f)
                }
                drawPath(cart, color, style = stroke)
                drawCircle(color, w * 0.065f, Offset(w * 0.38f, h * 0.82f))
                drawCircle(color, w * 0.065f, Offset(w * 0.76f, h * 0.82f))
            }
            3 -> { // perfil: cabeza y hombros
                drawCircle(color, w * 0.19f, Offset(w * 0.5f, h * 0.30f), style = stroke)
                val body = Path().apply {
                    moveTo(w * 0.16f, h * 0.90f)
                    quadraticTo(w * 0.16f, h * 0.58f, w * 0.5f, h * 0.58f)
                    quadraticTo(w * 0.84f, h * 0.58f, w * 0.84f, h * 0.90f)
                }
                drawPath(body, color, style = stroke)
            }
            else -> { // órdenes
                drawRoundRect(
                    color, topLeft = Offset(w * 0.20f, h * 0.08f), size = Size(w * 0.60f, h * 0.84f),
                    cornerRadius = CornerRadius(w * 0.10f), style = stroke
                )
                listOf(0.34f, 0.54f, 0.74f).forEach { y ->
                    drawLine(color, Offset(w * 0.34f, h * y), Offset(w * 0.66f, h * y), sw, StrokeCap.Round)
                }
            }
        }
    }
}
