package com.example.terragalegamovile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.AddOnOption
import com.example.terragalegamovile.data.CartItem
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.Product
import com.example.terragalegamovile.ui.theme.BorderSoft
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Cream
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel
import com.example.terragalegamovile.ui.theme.TextSoft
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Composable
fun terraFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.7f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
    focusedBorderColor = Terra,
    unfocusedBorderColor = BorderSoft,
    focusedTextColor = Charcoal,
    unfocusedTextColor = Charcoal,
    cursorColor = Terra
)

// Encabezado de página: eyebrow terracota + título serif (como app-page-header)
@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 34.sp      // <-- NUEVO
) {
    Column(modifier = modifier) {
        Text(
            eyebrow.uppercase(),
            color = Terra,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(title, fontFamily = Playfair, fontWeight = FontWeight.Bold, fontSize = titleSize, color = Charcoal)
    }
}

// "← Volver a la carta"
@Composable
fun BackLink(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("←", color = Charcoal.copy(alpha = 0.7f), fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Charcoal.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Ventana de confirmación o de error (como app-modal)
@Composable
fun TerraDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String? = null,
    onCancel: () -> Unit = onConfirm,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Cream,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Charcoal) },
        text = { Text(message, color = TextSoft) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) Burgundy else Terra, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (cancelLabel != null) {
                TextButton(onClick = onCancel) { Text(cancelLabel, color = TextSoft) }
            }
        }
    )
}

// Aviso flotante ("Añadido al carrito")
@Composable
fun ToastPill(title: String, text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .shadow(12.dp, shape)
            .clip(shape)
            .background(Charcoal)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}

// Botón de categoría del menú
@Composable
fun PillChip(text: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (active) Terra else Color.White.copy(alpha = 0.7f))
            .border(1.dp, if (active) Terra else BorderSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = if (active) Color.White else Charcoal.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    accent: Color = Forest,
    icon: String? = null,
    modifier: Modifier = Modifier,      // <-- NUEVO
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))        // sombreado más redondeado (antes 10.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // El ícono es la casilla: gris = sin marcar, verde = marcada
            FeatureBadge(label = icon, active = checked, size = 30.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) accent else Color.Transparent)
                    .border(2.dp, if (checked) accent else Charcoal.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            color = if (checked) Charcoal else Charcoal.copy(alpha = 0.6f)
        )
    }
}

// (texto de la etiqueta del plato, texto en el panel de filtros), igual que FEATURES de la web
val FEATURE_FILTERS = listOf(
    "Vegetariano" to "Vegetariano",
    "Picante ligero" to "Picante ligero",
    "Picante intenso" to "Picante intenso",
    "Nueces" to "Contiene nueces",
    "Mariscos" to "Mariscos",
    "Gluten" to "Contiene gluten"
)

@Composable
private fun FilterIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = h * 0.08f
        drawLine(color, Offset(w * 0.10f, h * 0.25f), Offset(w * 0.90f, h * 0.25f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.30f, h * 0.75f), Offset(w * 0.70f, h * 0.75f), sw, StrokeCap.Round)
    }
}


// Indica si el panel de filtros está abierto (la pantalla lo usa para desenfocar el fondo)
object FilterUi {
    var open by mutableStateOf(false)
}
// Botón redondo del filtro de características con su panel
@Composable
fun FeatureFilterButton(active: Set<String>, onApply: (Set<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(active) }
    val has = active.isNotEmpty()
    DisposableEffect(open) {
        FilterUi.open = open
        onDispose { FilterUi.open = false }
    }
    Box {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (has) Terra else Color.White.copy(alpha = 0.7f))
                .border(1.dp, if (has) Terra else BorderSoft, CircleShape)
                .clickable { draft = active; open = !open },
            contentAlignment = Alignment.Center
        ) {
            FilterIcon(
                color = if (has) Color.White else Charcoal.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.width(250.dp).padding(horizontal = 20.dp, vertical = 12.dp)) {
                SectionLabel("Filtrar por")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Características", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                    Text(
                        "Limpiar",
                        fontSize = 12.sp,
                        color = TextLabel,
                        modifier = Modifier.clickable { draft = emptySet() }
                    )
                }
                Spacer(Modifier.height(6.dp))
                FEATURE_FILTERS.forEach { (key, label) ->
                    CheckRow(
                        label = label,
                        checked = key in draft,
                        icon = key,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        draft = if (key in draft) draft - key else draft + key
                    }
                }
                Spacer(Modifier.height(12.dp))
                TerraButton(
                    text = "Aplicar",
                    onClick = { onApply(draft); open = false },
                    style = ButtonStyle.Sage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Tarjeta de plato (menú y "También te puede gustar")

// Trazados de los íconos (los mismos de feature-icon de la web, cuadrícula de 24)
private val FEATURE_PATHS: Map<String, List<String>> = mapOf(
    "Vegetariano" to listOf(
        "M5 19c0-8 5-14 15-14 0 10-6 15-14 15",
        "M5 19c3-5 6-8 10-10"
    ),
    "Picante ligero" to listOf(
        "M12 3c1 3.5 5 5.5 5 10a5 5 0 01-10 0c0-2 1-3.2 2-4.2 0 1.5.7 2.3 1.5 2.7C10.3 8.5 10.8 5.5 12 3z"
    ),
    "Picante intenso" to listOf(
        "M9 3c.8 2.8 4 4.4 4 8a4 4 0 01-8 0c0-1.6.8-2.6 1.6-3.4 0 1.2.6 1.8 1.2 2.1C7.6 7.2 8.2 5.2 9 3z",
        "M16 9c.6 2 3 3.2 3 5.8a3 3 0 01-6 0c0-1.2.6-1.9 1.2-2.5 0 .9.5 1.4.9 1.6-.2-1.8.3-3.3.9-4.9z"
    ),
    "Nueces" to listOf(
        "M6 10h12c0 5-2.5 9-6 10-3.5-1-6-5-6-10z",
        "M5 10c0-2.2 3-4 7-4s7 1.8 7 4",
        "M12 6V3"
    ),
    "Mariscos" to listOf(
        "M3 12c3-4.5 8-5.5 12-3l6-3v12l-6-3c-4 2.5-9 1.5-12-3z"
    ),
    "Gluten" to listOf(
        "M12 21V8",
        "M12 8c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z",
        "M12 8c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z",
        "M12 14c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z",
        "M12 14c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z",
        "M12 20c-2.5 0-4-1.5-4-4 2.5 0 4 1.5 4 4z",
        "M12 20c2.5 0 4-1.5 4-4-2.5 0-4 1.5-4 4z"
    )
)

@Composable
// Círculo con el ícono de una característica. active = verde, si no = gris
fun FeatureBadge(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    size: Dp = 24.dp
) {
    val paths = remember(label) {
        FEATURE_PATHS[label].orEmpty().map { PathParser().parsePathString(it).toPath() }
    }
    if (paths.isEmpty()) return

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) Forest.copy(alpha = 0.92f) else Charcoal.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.58f)) {
            scale(this.size.width / 24f, this.size.width / 24f, pivot = Offset.Zero) {
                val stroke = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                paths.forEach { drawPath(it, Cream, style = stroke) }
                if (label == "Mariscos") drawCircle(Cream, 0.8f, Offset(8f, 11f)) // ojo del pez
            }
        }
    }
}
// Tarjeta de plato (menú y "También te puede gustar")
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 26.dp,
    imageHeight: Dp = 180.dp,
    showCategory: Boolean = true
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
            .shadow(
                10.dp, shape,
                ambientColor = Charcoal.copy(alpha = 0.08f),
                spotColor = Charcoal.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
            ProductImage(product.imageUrl, Modifier.fillMaxWidth().height(imageHeight))
            if (product.popular) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Cream.copy(alpha = 0.88f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("★", color = Terra, fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("Popular", color = Terra, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Arriba a la derecha: íconos de características y, a su derecha, la categoría
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                product.featureLabels().forEach { FeatureBadge(it) }
                product.category?.takeIf { showCategory }?.let { category ->                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Charcoal.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(category.name, color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Parte blanca: solo nombre y precio
        // Parte blanca: nombre y, debajo, el precio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                product.name,
                fontFamily = Playfair,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Charcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(Format.money(product.price), color = Terra, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// Fila de un adicional con su selector de cantidad (0 = sin elegir)
@Composable
fun AddOnRow(addOn: AddOnOption, quantity: Int, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val selected = quantity > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Terra.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f))
            .border(1.dp, if (selected) Terra else BorderSoft, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(addOn.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Charcoal.copy(alpha = 0.75f))
            Text("+${Format.money(addOn.price)}", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.45f))
        }
        QuantityStepper(value = quantity, onChange = onChange, min = 0, compact = true)
    }
}

// Barra de progreso del pedido (réplica de app-order-progress)
@Composable
fun OrderProgress(status: String) {
    if (status == "CANCELLED") {
        StatusPill("✕ Pedido cancelado", Burgundy)
        return
    }
    val flow = listOf("PENDING", "CONFIRMED", "PREPARING", "ON_THE_WAY", "DELIVERED")
    val current = flow.indexOf(status)
    // 0 = completado, 1 = actual, 2 = pendiente
    fun stateOf(i: Int) = if (i < current || status == "DELIVERED") 0 else if (i == current) 1 else 2
    fun lineColor(i: Int) = if (stateOf(i) == 2) Charcoal.copy(alpha = 0.12f) else Forest

    Row(modifier = Modifier.fillMaxWidth()) {
        flow.forEachIndexed { i, step ->
            val state = stateOf(i)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                // Línea hacia el paso anterior (mitad izquierda) y hacia el siguiente (mitad derecha)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(2.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (i > 0) lineColor(i) else Color.Transparent)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (i < flow.lastIndex) lineColor(i + 1) else Color.Transparent)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .then(
                                if (state == 1) Modifier.drawBehind {
                                    drawCircle(Terra.copy(alpha = 0.18f), radius = size.minDimension / 2 + 4.dp.toPx())
                                } else Modifier
                            )
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    0 -> Forest
                                    1 -> Terra
                                    else -> Color(0xFFECE9E1)
                                }
                            )
                            .then(
                                if (state == 2) Modifier.border(1.5.dp, Charcoal.copy(alpha = 0.18f), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            0 -> Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            1 -> Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                    Text(
                        Format.statusLabel(step),
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (state == 1) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (state == 2) Charcoal.copy(alpha = 0.4f) else Charcoal
                    )
                }
            }
        }
    }
}

// Pin de ubicación (o "+" para "Usar otra dirección")
@Composable
fun AddressIcon(plus: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        if (plus) {
            drawLine(color, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), sw, StrokeCap.Round)
            drawLine(color, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), sw, StrokeCap.Round)
        } else {
            val pin = Path().apply {
                moveTo(w * 0.5f, h * 0.92f)
                cubicTo(w * 0.5f, h * 0.92f, w * 0.16f, h * 0.62f, w * 0.16f, h * 0.40f)
                arcTo(Rect(w * 0.16f, h * 0.06f, w * 0.84f, h * 0.74f), 180f, 180f, false)
                cubicTo(w * 0.84f, h * 0.62f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
            }
            drawPath(pin, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.40f))
        }
    }
}

// Cuadrito con el ícono de la dirección (como en la lista del checkout)
@Composable
fun AddressIconBox(selected: Boolean, plus: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Terra.copy(alpha = 0.12f) else Charcoal.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        AddressIcon(
            plus = plus,
            color = if (selected) Terra else Charcoal.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun TrashIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        drawLine(color, Offset(w * 0.12f, h * 0.22f), Offset(w * 0.88f, h * 0.22f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.12f), Offset(w * 0.62f, h * 0.12f), sw, StrokeCap.Round)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.22f, h * 0.30f),
            size = Size(w * 0.56f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(width = sw)
        )
        drawLine(color, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.42f, h * 0.74f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, h * 0.45f), Offset(w * 0.58f, h * 0.74f), sw, StrokeCap.Round)
    }
}

// Tarjeta seleccionable con círculo (direcciones y métodos de pago)
@Composable
fun SelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Terra.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.85f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Terra else BorderSoft, shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) Terra else Color.Transparent)
                    .border(2.dp, if (selected) Terra else Charcoal.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            if (icon != null) {
                icon()
                Spacer(Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f).padding(end = if (badge != null) 90.dp else 0.dp),
                content = content
            )
        }
        if (badge != null) {
            Text(
                badge,
                fontSize = 11.sp,
                color = Forest,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(Forest.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// Resumen del pedido (paso 1 y 2). El contenido extra (botones) va abajo.
@Composable
fun OrderSummaryCard(
    items: List<CartItem>,
    deliveryFee: Double,
    discount: Double = 0.0,
    couponCode: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val subtotal = items.sumOf { CartStore.unitPrice(it) * it.quantity }
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Text(
            "Resumen del pedido",
            fontFamily = Playfair,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Charcoal
        )
        Spacer(Modifier.height(16.dp))
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${item.quantity}× ${item.product.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Charcoal
                    )
                    if (item.addOns.isNotEmpty()) {
                        Text(
                            Format.group(item.addOns.map { it.name })
                                .joinToString(", ") { "x${it.second} ${it.first}" },
                            fontSize = 12.sp,
                            color = Charcoal.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    Format.money(CartStore.unitPrice(item) * item.quantity),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Charcoal.copy(alpha = 0.8f)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.1f)))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.7f))
            Text(Format.money(subtotal), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.7f))
        }
        if (discount > 0) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (couponCode != null) "Descuento ($couponCode)" else "Descuento",
                    fontSize = 14.sp,
                    color = Forest
                )
                Text("-" + Format.money(discount), fontSize = 14.sp, color = Forest)
            }
        }

        if (deliveryFee > 0) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Domicilio", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.7f))
                Text(Format.money(deliveryFee), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total", fontWeight = FontWeight.Medium, color = Charcoal)
            Text(Format.money(subtotal - discount + deliveryFee), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Terra)        }
        content()
    }
}