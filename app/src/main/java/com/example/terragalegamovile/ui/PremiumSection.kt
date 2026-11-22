package com.example.terragalegamovile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.PointsMovement
import com.example.terragalegamovile.data.SubscriptionStatus
import com.example.terragalegamovile.data.t
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.ButtonStack
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.ErrorNotice
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.ForestText
import com.example.terragalegamovile.ui.theme.Gold
import com.example.terragalegamovile.ui.theme.Paper
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.TerraText
import com.example.terragalegamovile.ui.theme.ThemeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Puntos Premium y suscripción en el perfil, como en "Mi cuenta" de la web:
// saldo y valor de los puntos, sus movimientos, y el botón Premium con su ventana.
@Composable
fun PremiumSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SubscriptionStatus?>(null) }
    var reload by remember { mutableStateOf(0) }
    var showModal by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        try {
            status = ApiClient.api.subscriptionStatus()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin datos de Premium: la sección no se muestra
        }
    }

    // Al volver de la pasarela de pago se consulta otra vez (el pago puede tardar en confirmarse)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reload++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val s = status ?: return

    // ---------- Puntos ----------
    val goldText = if (ThemeState.dark) Color(0xFFF2C260) else Color(0xFF8A5A00)
    val cardShape = RoundedCornerShape(24.dp)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (s.subscribed) Modifier
                    .clip(cardShape)
                    .border(1.5.dp, Gold.copy(alpha = 0.55f), cardShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Gold.copy(alpha = 0.14f), Color.Transparent)
                        )
                    )
                else Modifier
            )
    ) {
        if (s.subscribed) {
            PremiumBadge(goldText)
            Spacer(Modifier.height(8.dp))
        }
        Text(t("Mis puntos Premium"), fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Charcoal)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%,d".format(s.pointsBalance).replace(',', '.'),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Playfair,
                color = if (s.subscribed) goldText else Charcoal
            )
            Spacer(Modifier.width(6.dp))
            Text(
                t("puntos") + " · " + Format.money(s.pointsBalance * s.pointValueCop) + " COP",
                fontSize = 14.sp,
                color = Charcoal.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (s.subscribed) t("Ganas 1 punto por cada $1.000 en platos y adicionales después de descuentos. Se acreditan al recibir el pedido.")
            else t("Tu saldo se conserva. Activa Premium para acumular y pagar con puntos."),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Charcoal.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(12.dp))
        PointsHistory()
    }

    Spacer(Modifier.height(16.dp))

    // ---------- Botón Premium ----------
    val label = when {
        s.subscribed && s.canSubscribe -> t("Renovar Premium")
        s.subscribed -> t("Premium activo")
        else -> t("Suscribirme a Premium")
    }
    val days = if (s.daysLeft == 1) t("1 día") else t("{} días", s.daysLeft)
    val hint = when {
        !s.subscribed -> t("Domicilios gratis por {} días.", s.durationDays)
        s.canSubscribe -> t("Te quedan {} (vence el {}). Ya puedes renovar.", days, Format.dateTime(s.subscriptionEndsAt).substringBefore(','))
        else -> t("Vence el {}. Te quedan {}.", Format.dateTime(s.subscriptionEndsAt).substringBefore(','), days)
    }
    if (s.subscribed && !s.canSubscribe) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Gold.copy(alpha = 0.16f))
                    .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("✓", color = goldTextOutside(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(label, color = goldTextOutside(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    } else {
        ButtonStack {
            TerraButton(
                label,
                onClick = { startError = null; showModal = true },
                style = ButtonStyle.Gold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Text(
        hint,
        fontSize = 12.sp,
        color = Charcoal.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    if (showModal) {
        SubscribeDialog(
            status = s,
            starting = starting,
            error = startError,
            onCancel = { if (!starting) showModal = false },
            onPay = {
                starting = true
                startError = null
                scope.launch {
                    try {
                        val started = ApiClient.api.subscriptionStart()
                        val url = started.checkoutUrl
                        if (url.isNullOrBlank()) {
                            startError = t("No recibimos el enlace de pago. Inténtalo de nuevo.")
                        } else {
                            showModal = false
                            openStripe(context, url)
                        }
                        status = started
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        startError = e.toUserMessage()
                    } finally {
                        starting = false
                    }
                }
            }
        )
    }
}

// "Ver movimientos" de puntos: se cargan al abrirlo
@Composable
private fun PointsHistory() {
    var open by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<PointsMovement>?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(open) {
        if (!open || items != null) return@LaunchedEffect
        try {
            items = ApiClient.api.pointsHistory()
            failed = false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }

    Text(
        (if (open) "▾ " else "▸ ") + t("Ver movimientos"),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TerraText,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { open = !open }
            .padding(vertical = 8.dp, horizontal = 2.dp)
    )
    AnimatedVisibility(open) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            val list = items
            when {
                failed -> ErrorNotice(t("No se pudieron cargar los movimientos."))
                list == null -> Text(t("Cargando movimientos…"), fontSize = 13.sp, color = Charcoal.copy(alpha = 0.55f))
                list.isEmpty() -> Text(t("Todavía no tienes movimientos."), fontSize = 13.sp, color = Charcoal.copy(alpha = 0.55f))
                else -> list.forEach { m ->
                    val gained = m.kind != "SPENT"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (m.kind) {
                                "EARNED" -> t("Puntos ganados")
                                "SPENT" -> t("Pago con puntos")
                                else -> t("Puntos devueltos")
                            } + (m.orderId?.let { " · #$it" } ?: "") + " · " + Format.dateTime(m.createdAt).substringBefore(','),
                            fontSize = 13.sp,
                            color = Charcoal.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            (if (gained) "+" else "−") + m.amount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (gained) ForestText else TerraText
                        )
                    }
                }
            }
        }
    }
}

// Ventana "Hazte Premium" / "Renueva tu Premium" (la misma de la web)
@Composable
private fun SubscribeDialog(
    status: SubscriptionStatus,
    starting: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    val renewing = status.subscribed
    val price = Format.money(status.priceCop)
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Paper,
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Etiqueta "Premium"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Gold))
                    Spacer(Modifier.width(6.dp))
                    Text("Premium", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (ThemeState.dark) Gold else Color(0xFF8A5A00))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (renewing) t("Renueva tu Premium") else t("Hazte Premium"),
                    fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = Charcoal
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (renewing) t("Te quedan {} días. Al renovar se suman {} días a tu vencimiento, no pierdes los que te quedan.", status.daysLeft, status.durationDays)
                    else t("Pide sin pagar domicilio durante {} días.", status.durationDays),
                    fontSize = 14.sp, color = Charcoal.copy(alpha = 0.65f)
                )

                // Precio
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Forest.copy(alpha = 0.10f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(price, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, color = Charcoal)
                        Text(" COP", fontSize = 13.sp, color = Charcoal.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Text(
                        t("Pago único por {} días.", status.durationDays) + " " + t("Sin renovación automática."),
                        fontSize = 13.sp, color = Charcoal.copy(alpha = 0.6f)
                    )
                }

                // Beneficios
                Spacer(Modifier.height(14.dp))
                Benefit(t("Domicilios gratis"), t("En todos tus pedidos."))
                Benefit(t("Cupones exclusivos"), t("Descuentos pensados para suscriptores."))
                Benefit(
                    t("Puntos por tus compras"),
                    t("Gana 1 punto por cada $1.000 en platos y adicionales después de descuentos. Cada punto vale $100 COP para pagar un pedido completo.")
                )
                Benefit(t("Prioridad en reservas de mesa"), t("Tu reserva pasa primero.") + " (" + t("Próximamente") + ")", soon = true)

                // Sin reembolso
                Spacer(Modifier.height(8.dp))
                Text(
                    t("No hay reembolsos: una vez realizado el pago no se devuelve el dinero."),
                    fontSize = 13.sp,
                    color = if (ThemeState.dark) Gold else Color(0xFF8A5A00),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gold.copy(alpha = 0.12f))
                        .padding(12.dp)
                )
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    ErrorNotice(it)
                }
                Spacer(Modifier.height(16.dp))
                ButtonStack {
                    TerraButton(
                        if (starting) t("Abriendo el pago…") else t("Pagar {}", price),
                        onClick = onPay,
                        enabled = !starting,
                        style = ButtonStyle.Gold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    TerraButton(
                        t("Ahora no"),
                        onClick = onCancel,
                        enabled = !starting,
                        style = ButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun Benefit(title: String, text: String, soon: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        // Visto verde (hueco si todavía no está activo)
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            if (soon) drawCircle(Forest, radius = w / 2 - 1f, style = Stroke(width = w * 0.08f))
            else drawCircle(Forest, radius = w / 2)
            val c = if (soon) Forest else Color.White
            drawLine(c, Offset(w * 0.28f, w * 0.52f), Offset(w * 0.43f, w * 0.67f), w * 0.09f, StrokeCap.Round)
            drawLine(c, Offset(w * 0.43f, w * 0.67f), Offset(w * 0.73f, w * 0.35f), w * 0.09f, StrokeCap.Round)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
            Text(text, fontSize = 13.sp, color = Charcoal.copy(alpha = 0.6f))
        }
    }
}

private fun goldTextOutside(): Color = if (ThemeState.dark) Color(0xFFF2C260) else Color(0xFF8A5A00)

// Etiqueta "Premium" con punto dorado (como en la web)
@Composable
private fun PremiumBadge(textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Gold.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Gold))
        Spacer(Modifier.width(6.dp))
        Text("Premium", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}
