package com.example.terragalegamovile.ui.components

import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.semantics.semantics

import androidx.compose.material3.minimumInteractiveComponentSize

import com.example.terragalegamovile.data.t

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.terragalegamovile.data.Product
import com.example.terragalegamovile.data.ProductDetail
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel

// Foto del plato (la misma URL que usa la web); gris suave mientras carga o si falla
@Composable
fun ProductImage(url: String?, modifier: Modifier = Modifier, description: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = modifier.background(Charcoal.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) {
            DishPlaceholder()
        } else {
            coil.compose.SubcomposeAsyncImage(
                model = coil.request.ImageRequest.Builder(context).data(url).crossfade(250).build(),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                error = { DishPlaceholder() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Plato y cubiertos en gris: cuando el plato no tiene foto o no cargó
@Composable
private fun DishPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(36.dp)) {
            val w = size.width
            val c = Charcoal.copy(alpha = 0.22f)
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f)
            drawCircle(c, radius = w * 0.3f, center = center, style = stroke)
            drawCircle(c, radius = w * 0.18f, center = center, style = stroke)
            drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.06f, w * 0.2f), androidx.compose.ui.geometry.Offset(w * 0.06f, w * 0.8f), w * 0.06f)
            drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.94f, w * 0.2f), androidx.compose.ui.geometry.Offset(w * 0.94f, w * 0.8f), w * 0.06f)
        }
    }
}

fun featureLabels(
    vegetarian: Boolean,
    spicyMild: Boolean,
    spicyHot: Boolean,
    nuts: Boolean,
    seafood: Boolean,
    gluten: Boolean
): List<String> = buildList {
    if (vegetarian) add(t("Vegetariano"))
    if (spicyMild) add(t("Picante ligero"))
    if (spicyHot) add(t("Picante intenso"))
    if (nuts) add(t("Nueces"))
    if (seafood) add(t("Mariscos"))
    if (gluten) add(t("Gluten"))
}

fun Product.featureLabels() =
    featureLabels(vegetarian, spicyMild, spicyHot, containsNuts, containsSeafood, containsGluten)

fun ProductDetail.featureLabels() =
    featureLabels(vegetarian, spicyMild, spicyHot, containsNuts, containsSeafood, containsGluten)

// Etiquetas de características (vegetariano, picante, alérgenos)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatureChips(labels: List<String>, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { label ->
            val color = when (label) {
                "Vegetariano" -> Forest
                "Picante ligero" -> Terra
                "Picante intenso" -> Burgundy
                else -> Charcoal.copy(alpha = 0.7f)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Selector de cantidad: píldora gris con − y +, como el de la web
@Composable
fun QuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    compact: Boolean = false
) {
    val button: Dp = if (compact) 30.dp else 40.dp
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Charcoal.copy(alpha = 0.06f))
            .padding(if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton("−", value > min, button) { onChange(value - 1) }
        Text(
            "$value",
            modifier = Modifier.width(if (compact) 28.dp else 44.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 14.sp else 16.sp,
            color = Charcoal
        )
        StepButton("+", value < max, button) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // Se ve del mismo tamaño, pero se puede tocar en 48 dp
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = if (symbol == "+") t("Agregar uno") else t("Quitar uno") },
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            fontSize = 20.sp,
            color = Charcoal.copy(alpha = if (enabled) 0.7f else 0.25f)
        )
    }
}

// Etiqueta pequeña en mayúsculas (como "ADICIONALES OPCIONALES" en la web)
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.8.sp,
        color = TextLabel
    )
}