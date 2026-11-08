package com.example.terragalegamovile.ui.components

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
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(96.dp) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .hazeSource(hazeState)
        ) {
            content(PaddingValues(top = barHeight))
        }
        TerraTopBar(
            role = role,
            name = name,
            onLogout = onLogout,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
        )
    }
}

// Barra superior: réplica de .nav-bar--stayed (vidrio esmerilado con blur real)
@Composable
fun TerraTopBar(
    role: String,
    name: String,
    onLogout: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val pill = RoundedCornerShape(50)

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
            // Chip de usuario
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(pill)
                    .background(Color.White)
                    .border(1.dp, Color.White, pill)
                    .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        role.uppercase(),
                        fontFamily = SourceSans,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.8.sp,
                        color = Terra
                    )
                    Text(
                        name,
                        fontFamily = SourceSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Charcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Botón Salir (estilo "danger" de la web; al presionar se rellena)
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (pressed) Burgundy else Color.White)
                    .border(1.dp, Burgundy, CircleShape)
                    .semantics { contentDescription = "Cerrar sesión" }
                    .clickable(interactionSource = interaction, indication = null, onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                LogoutIcon(
                    bracket = if (pressed) Color.White else Burgundy,
                    arrow = if (pressed) Color.White else Terra,
                    modifier = Modifier.size(22.dp)
                )
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
