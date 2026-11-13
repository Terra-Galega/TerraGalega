package com.example.terragalegamovile.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.*
import com.example.terragalegamovile.ui.components.*
import com.example.terragalegamovile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheckoutConfirmScreen(
    user: LoginResponse,
    onLogout: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onExit: () -> Unit
) {
    val d = CheckoutDraft
    val scope = rememberCoroutineScope()
    var paying by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf("") }
    var showCancel by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Si se llegó sin completar el paso 1, se vuelve a él
    LaunchedEffect(Unit) {
        if (CartStore.items.isNotEmpty() && (d.method.isEmpty() || d.address.isEmpty())) {
            onEdit()
            return@LaunchedEffect
        }
        val removed = CartStore.removeUnavailable()
        if (removed.isNotEmpty()) {
            errorTitle = "Actualizamos tu carrito"
            errorMessage = unavailableMessage(removed)
        }
    }

    // Mismo POST /checkout que usa la web: el pedido queda en H2 y se copia a Postgres
    fun confirm() {
        if (paying || CartStore.items.isEmpty()) return
        serverError = ""
        val request = CheckoutRequest(
            address = d.address,
            items = CartStore.items.map { item ->
                CheckoutItemRequest(item.product.id, item.quantity, item.addOns.map { it.id })
            }
        )
        val address = d.address
        val saveAddress = d.saveAddress
        val addressName = d.addressName
        val isCard = d.method == "card"
        paying = true

        scope.launch {
            try {
                if (isCard) delay(1500) // lo que tarda la pasarela simulada
                ApiClient.api.checkout(user.id, request).close()
                if (saveAddress) {
                    try {
                        ApiClient.api.addAddress(user.id, AddressInput(label = address, name = addressName.ifBlank { null }))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // El pedido ya está creado: si no se pudo guardar la dirección, no se avisa
                    }
                }
                CartStore.clear()
                CheckoutDraft.reset()
                ClientUi.tab = 1
                ClientUi.paid = true
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                paying = false
                val message = e.toUserMessage()
                val removed = CartStore.removeUnavailable()
                if (removed.isNotEmpty()) {
                    errorTitle = "Actualizamos tu carrito"
                    errorMessage = unavailableMessage(removed)
                } else {
                    errorTitle = "No se pudo procesar el pedido"
                    serverError = message
                    errorMessage = message
                }
            }
        }
    }

    TerraScaffold(role = "Cliente", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            PageHeader("Paso 2 de 2 · Pasarela de pago", "Revisa tu pedido")
            Spacer(Modifier.height(28.dp))

            if (CartStore.items.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 32.dp) {
                    Text("Tu carrito está vacío. Agrega algo de la carta antes de pagar.", color = TextSoft)
                    Spacer(Modifier.height(16.dp))
                    TerraButton("Explorar la carta", onClick = onExit, modifier = Modifier.fillMaxWidth())
                }
                return@TerraScaffold
            }

            // Entrega
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Entrega", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                        Text(
                            d.address,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (d.saveAddress) {
                            val named = if (d.addressName.isNotBlank()) " como «${d.addressName}»" else ""
                            Text(
                                "Esta dirección se guardará en tu perfil$named para tus próximos pedidos.",
                                fontSize = 14.sp,
                                color = Charcoal.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                    Text(
                        "Editar",
                        color = Terra,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onEdit).padding(start = 12.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Pago
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Método de pago", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                        if (d.method == "card") {
                            Text(
                                "Tarjeta •••• ${d.number.takeLast(4)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Charcoal,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                if (d.installments == 1) "Pago de contado" else "${d.installments} cuotas",
                                fontSize = 14.sp,
                                color = Charcoal.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                "Contra entrega",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Charcoal,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Pagas en efectivo cuando recibas tu pedido.",
                                fontSize = 14.sp,
                                color = Charcoal.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Text(
                        "Editar",
                        color = Terra,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onEdit).padding(start = 12.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Resumen con el domicilio incluido
            OrderSummaryCard(items = CartStore.items, deliveryFee = DELIVERY_FEE) {
                Spacer(Modifier.height(24.dp))
                TerraButton(
                    text = if (paying) {
                        if (d.method == "card") "Procesando pago..." else "Confirmando pedido..."
                    } else "Confirmar pedido",
                    onClick = { confirm() },
                    enabled = !paying,
                    modifier = Modifier.fillMaxWidth()
                )
                if (serverError.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        serverError,
                        color = Burgundy,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Cancelar compra",
                    color = Charcoal.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(enabled = !paying) { showCancel = true }
                        .padding(8.dp)
                )
            }
        }
    }

    if (showCancel) {
        TerraDialog(
            title = "Cancelar compra",
            message = "¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.",
            confirmLabel = "Sí, cancelar compra",
            cancelLabel = "Continuar comprando",
            danger = true,
            onConfirm = { showCancel = false; CheckoutDraft.reset(); onExit() },
            onCancel = { showCancel = false }
        )
    }
    errorMessage?.let {
        TerraDialog(errorTitle, it, "Aceptar", onConfirm = { errorMessage = null; serverError = "" })
    }
}