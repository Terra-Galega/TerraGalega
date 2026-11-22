package com.example.terragalegamovile.ui.components

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
            .background(Color.White.copy(alpha = 0.7f))
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
        ButtonStyle.Danger -> BorderStroke(1.dp, Burgundy.copy(alpha = 0.3f))
        else -> null
    }
    val textColor = when (style) {
        ButtonStyle.Primary, ButtonStyle.Sage -> Color.White
        ButtonStyle.Outline -> Charcoal.copy(alpha = 0.7f)
        ButtonStyle.Danger -> Burgundy
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
            fontSize = if (compact) 13.sp else 16.sp        )
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
                focusedContainerColor = Color.White.copy(alpha = 0.7f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
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
                        .buttonDescription(if (visible) "Ocultar contraseña" else "Mostrar contraseña")
                ) {
                    EyeIcon(open = !visible, color = Charcoal.copy(alpha = 0.55f))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.7f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
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
                    .background(Color.White)
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
                            .background(Color.White)
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
                .semantics { contentDescription = if (count > 0) "Notificaciones, $count sin leer" else "Notificaciones" }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = Terra,
                modifier = Modifier.size(28.dp)
            )
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

// Asistente virtual: solo el ícono de mensaje con signo de pregunta, sin fondo
@Composable
fun ChatButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics { contentDescription = "Asistente virtual" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ChatIcon(color = Terra, modifier = Modifier.size(28.dp))
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
            "Mi perfil",
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
    val fadeStops = remember(solidUntil) {
        buildList {
            add(0f to Cream)
            add(solidUntil to Cream)
            val steps = 10
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val alpha = 1f - (t * t * (3f - 2f * t))
                add((solidUntil + (1f - solidUntil) * t) to Cream.copy(alpha = alpha))
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
                galegaColor = Color(0xFF1A1A1A)
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
                    .background(Color.White)
                    .border(1.dp, Burgundy, CircleShape)
                    .semantics { contentDescription = "Cerrar sesión" }
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (pressed) Terra else Color.White)
                .border(1.dp, Terra, CircleShape)
                .semantics { contentDescription = "Carrito" }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CartIcon(color = if (pressed) Color.White else Terra, modifier = Modifier.size(22.dp))
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
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
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
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
                backgroundColor = Cream
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
        NavItem(false, "Carrito", onCart, badge = cartCount) { NavIcon(1, it) }
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
