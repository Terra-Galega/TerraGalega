package com.example.terragalegamovile.ui

import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.semantics.semantics

import com.example.terragalegamovile.ui.components.RefreshBox

import com.example.terragalegamovile.ui.theme.ForestText

import com.example.terragalegamovile.ui.theme.TerraText

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.Order
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.AddressIcon
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.OrderProgress
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraDialog
import com.example.terragalegamovile.ui.theme.BorderSoft
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Cream
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Pestaña "Mis pedidos": los mismos pedidos que ve el cliente en la web
@Composable
fun OrdersTab(
    user: LoginResponse,
    padding: PaddingValues,
    onTrack: () -> Unit,
    onGoMenu: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var toCancel by remember { mutableStateOf<Order?>(null) }
    var cancelError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // El aviso verde de "pedido creado" se apaga al salir de esta pestaña
    DisposableEffect(Unit) { onDispose { ClientUi.paid = false; ClientUi.lastPaidOrderId = null } }

    // Carga al abrir y refresca cada 6 s: si el operador cambia el estado, se ve solo
    LaunchedEffect(reload) {
        while (true) {
            try {
                orders = ApiClient.api.myOrders()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (loading) error = e.toUserMessage()
            }
            loading = false
            refreshing = false
            delay(6_000)
        }
    }

    val active = orders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" }
    val past = orders.filter { it.status == "DELIVERED" || it.status == "CANCELLED" }

    RefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; reload++ }, topPadding = padding.calculateTopPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (ClientUi.paid) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Forest.copy(alpha = 0.08f))
                            .border(1.dp, Forest.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Column {
                            Text(
                                t("✓  ¡Tu orden fue añadida con éxito! Ya quedó registrada y está pendiente."),
                                color = ForestText,
                                fontSize = 14.sp
                            )
                            ClientUi.lastPaidOrderId?.let { id ->
                                Text(
                                    t("Descargar mi factura"),
                                    color = ForestText,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .clickable {
                                            scope.launch {
                                                try {
                                                    openStripe(context, ApiClient.api.invoiceLink(id).previewUrl)
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // El título cede espacio; el botón nunca se aplasta (en catalán es más largo)
                        PageHeader(
                            t("Tu historial"), t("Mis pedidos"), titleSize = 28.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (orders.isNotEmpty()) {
                            Spacer(Modifier.width(12.dp))
                            TerraButton(t("Pedir de nuevo"), onClick = onGoMenu, compact = true)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            when {
                loading -> item { Text(t("Cargando tus pedidos..."), color = TextSoft) }

                error != null && orders.isEmpty() -> item { ErrorNotice(error ?: "", onRetry = { error = null; loading = true; reload++ }) }

                orders.isEmpty() -> item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 32.dp) {
                        Text(
                            t("Todavía no has hecho ningún pedido."),
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t("Cuando hagas tu primer pedido aparecerá aquí."),
                            color = TextSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                        ButtonStack {
                            TerraButton(t("Explorar la carta"), onClick = onGoMenu, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                else -> {
                    item { SectionTitle(t("Pedidos activos")) }
                    if (active.isEmpty()) {
                        item {
                            Text(
                                t("No tienes pedidos activos en este momento."),
                                fontSize = 14.sp,
                                color = Charcoal.copy(alpha = 0.55f)
                            )
                        }
                    } else {
                        items(active.size) { i ->
                            OrderCard(active[i], onCancel = { toCancel = active[i] }, onTrack = onTrack)
                        }
                    }
                    if (past.isNotEmpty()) {
                        item { SectionTitle(t("Pedidos pasados")) }
                        items(past.size) { i ->
                            OrderCard(past[i], onCancel = null, onTrack = onTrack)
                        }
                    }
                }
            }
        }
    }

    toCancel?.let { order ->
        TerraDialog(
            title = t("Cancelar pedido"),
            message = t("¿Estás seguro de que deseas cancelar este pedido? Esta acción no se puede deshacer."),
            confirmLabel = t("Sí, cancelar pedido"),
            cancelLabel = t("Volver"),
            danger = true,
            onConfirm = {
                toCancel = null
                scope.launch {
                    try {
                        ApiClient.api.cancelOrder(order.id).close()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        cancelError = e.toUserMessage()
                    }
                    reload++ // recarga la lista de inmediato (también si falló, por si el estado cambió)
                }
            },
            onCancel = { toCancel = null }
        )
    }

    cancelError?.let {
        TerraDialog(
            title = t("No se pudo cancelar el pedido"),
            message = it,
            confirmLabel = t("Aceptar"),
            onConfirm = { cancelError = null }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = Charcoal)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(order: Order, onCancel: (() -> Unit)?, onTrack: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper)
            .border(1.dp, Charcoal.copy(alpha = 0.08f), shape)
    ) {
        // Número, fecha y total
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(t("Pedido #{}", order.id), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                Text(Format.dateTime(order.createdAt), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                order.operatorId?.let {
                    Text(t("Operador #{}", it), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(t("Total"), fontSize = 12.sp, color = Charcoal.copy(alpha = 0.5f))
                Text(Format.money(order.total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TerraText)
            }
        }

        Box(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) { OrderProgress(order.status) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.08f)))

        // Productos
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            order.details.forEach { line ->
                val lineTotal = (line.unitPrice + line.addOns.sumOf { it.price }) * line.quantity
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text("${line.quantity}×", color = TerraText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(line.product.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Charcoal)
                        }
                        if (line.addOns.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Format.group(line.addOns.map { t(it.name) }).forEach { (name, count) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Forest.copy(alpha = 0.10f))
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("x$count $name", color = ForestText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        Format.money(lineTotal),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Charcoal.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Dirección
        if (!order.address.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Cream).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                AddressIcon(plus = false, color = ForestText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t("Entrega en"), fontSize = 12.sp, color = Charcoal.copy(alpha = 0.5f))
                    Text(order.address, fontSize = 15.sp, color = Charcoal)
                }
            }
        }

        // Chat con el restaurante sobre este pedido (como en la web), mientras siga en curso
        if (order.status != "DELIVERED" && order.status != "CANCELLED") {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.08f)))
            val unread = order.id in SupportStore.unreadIds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { SupportStore.openChat(order.id) }
                    .semantics {
                        contentDescription = t("¿Problemas con tu pedido?, escríbenos") +
                            if (unread) ". " + t("Tienes mensajes nuevos del restaurante") else ""
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                HeadsetIcon(color = TerraText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    t("¿Problemas con tu pedido?, escríbenos"),
                    color = TerraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (unread) {
                    UnreadDot()
                    Spacer(Modifier.width(6.dp))
                    Text(t("Nuevo"), fontSize = 12.sp, color = TerraText, fontWeight = FontWeight.SemiBold)
                }
                Text("  ›", color = TerraText, fontSize = 18.sp)
            }
        }

        // Seguimiento con mapa (solo cuando el domiciliario ya salió)
        if (order.status == "ON_THE_WAY") {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.08f)))
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TerraButton(
                        t("Seguir mi pedido"),
                        onClick = onTrack,
                        style = ButtonStyle.Sage,
                        compact = true
                    )
                }
            }
        }

        // Cancelar: solo mientras está PENDIENTE
        if (order.status == "PENDING" && onCancel != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.08f)))
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    t("Puedes cancelarlo mientras el restaurante no lo confirme."),
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
                ButtonStack {
                    TerraButton(
                        t("Cancelar pedido"),
                        onClick = onCancel,
                        style = ButtonStyle.Danger,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}