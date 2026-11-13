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
import androidx.compose.ui.res.painterResource
import com.example.terragalegamovile.R
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
            modifier = Modifier.offset(y = (-2).dp)
        )
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
    enabled: Boolean = true
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
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontFamily = SourceSans,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
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
            shape = RoundedCornerShape(12.dp),
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
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    fontFamily = Playfair,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Terra
                )
            }
        }
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
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topArea = statusTop + 80.dp          // el contenido empieza más abajo (antes 52)
    val fadeEnd = statusTop + 96.dp          // hasta dónde llega el difuminado
    val solidUntil = ((statusTop + 54.dp) / fadeEnd).coerceIn(0f, 1f)   // crema sólido detrás del logo
    // Degradado con curva suave (smoothstep) en vez de lineal, para que no se note el corte
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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .hazeSource(hazeState)
                .blur(blur, BlurredEdgeTreatment.Rectangle)   // <-- NUEVO

        ) {
            content(PaddingValues(top = topArea, bottom = bottomHeight))
        }

        // Difuminado crema para que el contenido no se cruce con el logo al hacer scroll
        // Difuminado crema: sólido detrás del logo y se desvanece suave hacia el contenido
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(fadeEnd)
                .background(Brush.verticalGradient(*fadeStops))
        )

        // Solo el logo, en la esquina
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = statusTop + 10.dp)
        ) {
            TerraLogo(
                scale = 0.8f,
                terraColor = Terra,
                galegaColor = Color(0xFF1A1A1A)
            )
        }

        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomHeight = with(density) { it.height.toDp() } }
            ) {
                bottomBar(hazeState)
            }
        } else {
            // Pantallas sin barra inferior: botones sueltos arriba a la derecha
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = statusTop + 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCart != null) {
                    CartButton(count = cartCount ?: 0, onClick = onCart)
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Burgundy, CircleShape)
                        .semantics { contentDescription = "Cerrar sesión" }
                        .clickable(onClick = onLogout),
                    contentAlignment = Alignment.Center
                ) {
                    LogoutIcon(bracket = Burgundy, arrow = Terra, modifier = Modifier.size(22.dp))
                    if (dim > 0f) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                    }
                }
            }
        }
    }
}

// Barra superior: réplica de .nav-bar--stayed (vidrio esmerilado con blur real)
@Composable
fun TerraTopBar(
    role: String,
    name: String,
    onLogout: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    cartCount: Int? = null,
    onCart: (() -> Unit)? = null
) {
    val pill = RoundedCornerShape(50)
    var menuOpen by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    // El menú sale pegado a la derecha, debajo del nombre
    val below = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ) = IntOffset(
                x = anchorBounds.right - popupContentSize.width,
                y = anchorBounds.bottom + gapPx
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // box-shadow: 0 10px 30px rgba(0,0,0,.08)
            .shadow(
                elevation = 10.dp,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            // backdrop-filter: blur(35px) + background rgba(206,206,206,.505)
            // backdrop-filter más fuerte + tinte verde Forest (#556141) traslúcido
            .hazeEffect(state = hazeState) {
                blurRadius = 40.dp
                noiseFactor = 0.02f
                backgroundColor = Cream
                tints = listOf(HazeTint(Forest.copy(alpha = 0.30f)))
            }
            // brillo de cristal más sutil, para que el verde y el blur se noten
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.0f))
                )
            )
            // canto del cristal: borde superior blanco, inferior oscuro sutil
            .drawBehind {
                val line = 1.dp.toPx()
                drawLine(
                    Color.White.copy(alpha = 0.9f),
                    Offset(0f, line / 2), Offset(size.width, line / 2), line
                )
                drawLine(
                    Color(0xFF4B4B4B).copy(alpha = 0.12f),
                    Offset(0f, size.height - line / 2), Offset(size.width, size.height - line / 2), line
                )
            }
            .statusBarsPadding() // el vidrio llega hasta arriba, el contenido queda bajo la hora/batería
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TerraLogo(
            scale = 0.8f,
            terraColor = Terra,
            galegaColor = Color(0xFF1A1A1A)
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nombre: solo texto, pegado a la derecha. Al tocarlo se despliega el menú
            Box(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { menuOpen = !menuOpen }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            role.uppercase(),
                            fontFamily = SourceSans,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.8.sp,
                            color = Terra,
                            textAlign = TextAlign.End
                        )
                        Text(
                            name,
                            fontFamily = SourceSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (menuOpen) "▴" else "▾", color = Terra, fontSize = 12.sp)
                }

                if (menuOpen) {
                    Popup(
                        popupPositionProvider = below,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            val card = RoundedCornerShape(22.dp)
                            Column(
                                modifier = Modifier
                                    .shadow(
                                        14.dp, card,
                                        ambientColor = Color.Black.copy(alpha = 0.15f),
                                        spotColor = Color.Black.copy(alpha = 0.15f)
                                    )
                                    .clip(card)
                                    .background(Color.White)
                                    .padding(vertical = 6.dp)
                            ) {
                                if (onCart != null) {
                                    val openCart = { menuOpen = false; onCart() }
                                    Row(
                                        modifier = Modifier
                                            .clickable(onClick = openCart)
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CartButton(count = cartCount ?: 0, onClick = openCart)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Carrito",
                                            fontFamily = SourceSans,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Charcoal,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                                val doLogout = { menuOpen = false; onLogout() }
                                Row(
                                    modifier = Modifier
                                        .clickable(onClick = doLogout)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, Burgundy, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LogoutIcon(
                                            bracket = Burgundy,
                                            arrow = Terra,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Salir",
                                        fontFamily = SourceSans,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Burgundy,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
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

// Barra inferior de pestañas con el mismo vidrio de la barra superior
@Composable
fun TerraBottomBar(
    hazeState: HazeState,
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .hazeEffect(state = hazeState) {
                blurRadius = 40.dp
                noiseFactor = 0.02f
                backgroundColor = Cream
                tints = listOf(HazeTint(Forest.copy(alpha = 0.30f)))
            }
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.0f), Color.White.copy(alpha = 0.22f))
                )
            )
            .drawBehind {
                val line = 1.dp.toPx()
                drawLine(
                    Color.White.copy(alpha = 0.9f),
                    Offset(0f, line / 2), Offset(size.width, line / 2), line
                )
            }.padding(bottom = navInset),   // <-- NUEVO: el vidrio se extiende hasta abajo
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label.uppercase(),
                    fontFamily = SourceSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    color = if (active) Terra else Charcoal.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(if (active) 22.dp else 0.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Terra)
                )
            }
        }
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
    onLogout: () -> Unit
) {
    val pill = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(selected == 0, "Carta", onMenu) {
            Image(
                painter = painterResource(
                    if (selected == 0) R.drawable.selected else R.drawable.unselected
                ),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
        NavItem(false, "Carrito", onCart, badge = cartCount) { NavIcon(1, it) }
        NavItem(selected == 1, "Órdenes", onOrders) { NavIcon(2, it) }
        NavItem(false, "Cerrar sesión", onLogout) {
            LogoutIcon(bracket = it, arrow = it, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    badge: Int = 0,
    icon: @Composable (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            icon(if (selected) Color.White else Color.White.copy(alpha = 0.8f))
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Terra),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badge > 9) "9+" else "$badge",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontFamily = SourceSans,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

// Íconos de la barra: 0 = carta, 1 = carrito, 2 = órdenes
@Composable
private fun NavIcon(kind: Int, color: Color) {
    Canvas(modifier = Modifier.size(26.dp)) {
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
