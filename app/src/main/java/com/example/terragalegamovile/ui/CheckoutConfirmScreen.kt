package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.terragalegamovile.data.*
import com.example.terragalegamovile.ui.components.*
import com.example.terragalegamovile.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// Cuántas veces (cada 2 s) se pregunta por el pago al volver de Stripe antes
// de ofrecerle al cliente volver a pagar o cancelar
private const val PAYMENT_CHECKS = 4

// Abre la página de pago de Stripe en una pestaña del navegador dentro de la
// app (Custom Tab). Al terminar, Stripe pasa por /payments/app-return del back,
// que reabre la app con terragalega://pago.
fun openStripe(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var paying by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf("") }
    var showCancel by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pago con tarjeta en curso: último estado conocido y si se está consultando
    var payment by remember { mutableStateOf<Payment?>(null) }
    var checking by remember { mutableStateOf(false) }

    // Si se llegó sin completar el paso 1, se vuelve a él
    LaunchedEffect(Unit) {
        if (d.pendingOrderId == null && CartStore.items.isNotEmpty() && (d.method.isEmpty() || d.address.isEmpty())) {
            onEdit()
            return@LaunchedEffect
        }
        val removed = CartStore.removeUnavailable()
        if (removed.isNotEmpty()) {
            errorTitle = t("Actualizamos tu carrito")
            errorMessage = unavailableMessage(removed)
        }
    }

    // El cupón pudo dejar de servir desde el paso 1 (venció, ya se usó...): se vuelve a validar
    LaunchedEffect(Unit) {
        val current = d.coupon ?: return@LaunchedEffect
        try {
            d.coupon = ApiClient.api.validateCoupon(current.code, CartStore.subtotal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            d.coupon = null
            errorTitle = t("Quitamos tu cupón")
            errorMessage = t("Quitamos el cupón {}. {}", current.code, e.toUserMessage())
        }
    }

    // Lo mismo que hace el checkout en efectivo al terminar
    suspend fun finish(address: String, saveAddress: Boolean, addressName: String) {

        ClientUi.lastPaidOrderId = null
        if (saveAddress) {
            try {
                ApiClient.api.addAddress(AddressInput(label = address, name = addressName.ifBlank { null }))
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
    }

    fun showError(e: Exception) {
        paying = false
        val message = e.toUserMessage()
        scope.launch {
            val removed = CartStore.removeUnavailable()
            if (removed.isNotEmpty()) {
                errorTitle = t("Actualizamos tu carrito")
                errorMessage = unavailableMessage(removed)
            } else {
                errorTitle = t("No se pudo procesar el pedido")
                serverError = message
                errorMessage = message
            }
        }
    }

    // Aplica el estado del pago. Devuelve true si ya no hay que seguir preguntando.
    suspend fun applyPayment(result: Payment): Boolean {
        payment = result
        return when (result.state) {
            "PENDING" -> false
            "PAID" -> {
                finish(d.address, d.saveAddress, d.addressName)
                ClientUi.lastPaidOrderId = result.orderId
                true
            }
            "EXPIRED" -> {
                // Caducó o se canceló: no hubo cobro y el carrito sigue igual
                d.pendingOrderId = null
                d.paymentRequestId = ""
                d.paymentRequestBody = ""
                payment = null
                paying = false
                errorTitle = t("El pago se canceló")
                errorMessage = t("No se hizo ningún cobro. Tu carrito sigue intacto para que lo intentes de nuevo.")
                true
            }
            else -> {
                // Pedido pagado y luego cancelado: reembolso
                d.pendingOrderId = null
                CartStore.refresh()
                errorTitle = t("Pedido cancelado")
                errorMessage = if (result.state == "REFUNDED") t("Te devolvimos el dinero a tu tarjeta.")
                else t("Estamos procesando el reembolso a tu tarjeta.")
                ClientUi.tab = 1
                onDone()
                true
            }
        }
    }

    // Pregunta por el pago unas cuantas veces (el back consulta a Stripe directamente,
    // así que no hace falta esperar al webhook)
    fun checkPayment() {
        val orderId = d.pendingOrderId ?: return
        if (checking) return
        checking = true
        scope.launch {
            try {
                repeat(PAYMENT_CHECKS) { attempt ->
                    if (attempt > 0) delay(2_000)
                    if (applyPayment(ApiClient.api.paymentStatus(orderId))) return@launch
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorTitle = t("No pudimos consultar tu pago")
                errorMessage = e.toUserMessage()
            } finally {
                checking = false
            }
        }
    }

    // Al volver a la app (desde Stripe o desde otra app) se consulta el pago
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && d.pendingOrderId != null) checkPayment()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Tarjeta: el back crea el pedido (oculto hasta que se pague) y la página
    // de pago de Stripe, que se abre en el navegador
    fun payWithCard(request: CheckoutRequest) {
        val body = Gson().toJson(request)
        if (d.paymentRequestId.isEmpty() || d.paymentRequestBody != body) {
            d.paymentRequestId = UUID.randomUUID().toString()
            d.paymentRequestBody = body
        }
        scope.launch {
            try {
                val result = ApiClient.api.startPayment(StartPaymentRequest(d.paymentRequestId, request))
                d.pendingOrderId = result.orderId
                payment = result
                val url = result.checkoutUrl
                when {
                    result.state == "PENDING" && url != null -> {
                        paying = false
                        openStripe(context, url)
                    }
                    else -> applyPayment(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    // Botón principal: efectivo crea el pedido ya; tarjeta va a Stripe
    fun confirm() {
        if (paying || CartStore.items.isEmpty()) return
        serverError = ""
        val request = CheckoutRequest(
            address = d.address,
            items = CartStore.items.map { item ->
                CheckoutItemRequest(item.product.id, item.quantity, item.addOns.map { it.id })
            },
            couponCode = d.coupon?.code
        )
        paying = true

        if (d.method == "card") {
            payWithCard(request)
            return
        }

        // Mismo POST /checkout que usa la web: el pedido queda en H2 y se copia a Postgres
        val address = d.address
        val saveAddress = d.saveAddress
        val addressName = d.addressName
        scope.launch {
            try {
                ApiClient.api.checkout(request).close()
                finish(address, saveAddress, addressName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    // Cancela el pedido sin pagar; el carrito se conserva
    fun cancelPayment() {
        val orderId = d.pendingOrderId ?: return
        if (checking) return
        checking = true
        scope.launch {
            try {
                applyPayment(ApiClient.api.cancelPayment(orderId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorTitle = t("No pudimos cancelar el pago")
                errorMessage = e.toUserMessage()
            } finally {
                checking = false
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
            PageHeader(t("Paso 2 de 2 · Pasarela de pago"), t("Revisa tu pedido"))
            Spacer(Modifier.height(28.dp))

            // Volvió de Stripe y el pago sigue pendiente: consultar, volver a pagar o cancelar
            if (d.pendingOrderId != null) {
                PendingPaymentCard(
                    checking = checking,
                    canPayAgain = payment?.checkoutUrl != null,
                    onCheck = { checkPayment() },
                    onPayAgain = { payment?.checkoutUrl?.let { openStripe(context, it) } },
                    onCancel = { cancelPayment() }
                )
                return@TerraScaffold
            }

            if (CartStore.items.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 32.dp) {
                    Text(t("Tu carrito está vacío. Agrega algo de la carta antes de pagar."), color = TextSoft)
                    Spacer(Modifier.height(16.dp))
                    ButtonStack {
                        TerraButton(t("Explorar la carta"), onClick = onExit, modifier = Modifier.fillMaxWidth())
                    }
                }
                return@TerraScaffold
            }

            // Entrega
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t("Entrega"), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                        Text(
                            d.address,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (d.saveAddress) {
                            val named = if (d.addressName.isNotBlank()) t(" como «{}»", d.addressName) else ""
                            Text(
                                t("Esta dirección se guardará en tu perfil{} para tus próximos pedidos.", named),
                                fontSize = 14.sp,
                                color = Charcoal.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                    Text(
                        t("Editar"),
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
                        Text(t("Método de pago"), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.55f))
                        Text(
                            if (d.method == "card") t("Tarjeta de crédito o débito") else t("Contra entrega"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Charcoal,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            if (d.method == "card") t("Ingresarás los datos en la pasarela segura de Stripe.")
                            else t("Pagas en efectivo cuando recibas tu pedido."),
                            fontSize = 14.sp,
                            color = Charcoal.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        t("Editar"),
                        color = Terra,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onEdit).padding(start = 12.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Resumen con el domicilio incluido
            OrderSummaryCard(
                items = CartStore.items,
                deliveryFee = DELIVERY_FEE,
                discount = d.coupon?.discount ?: 0.0,
                couponCode = d.coupon?.code
            ) {
                Spacer(Modifier.height(24.dp))
                ButtonStack {
                    TerraButton(
                        text = when {
                            d.method == "card" && paying -> t("Abriendo la pasarela...")
                            d.method == "card" -> t("Pagar con tarjeta")
                            paying -> t("Confirmando pedido...")
                            else -> t("Confirmar pedido")
                        },
                        onClick = { confirm() },
                        enabled = !paying,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (serverError.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ErrorNotice(serverError)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    t("Cancelar compra"),
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
            title = t("Cancelar compra"),
            message = t("¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará."),
            confirmLabel = t("Sí, cancelar compra"),
            cancelLabel = t("Continuar comprando"),
            danger = true,
            onConfirm = { showCancel = false; CheckoutDraft.reset(); onExit() },
            onCancel = { showCancel = false }
        )
    }
    errorMessage?.let {
        TerraDialog(errorTitle, it, t("Aceptar"), onConfirm = { errorMessage = null; serverError = "" })
    }
}

// Tarjeta que se muestra mientras hay un pago abierto en Stripe
@Composable
private fun PendingPaymentCard(
    checking: Boolean,
    canPayAgain: Boolean,
    onCheck: () -> Unit,
    onPayAgain: () -> Unit,
    onCancel: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
        Text(
            if (checking) t("Confirmando tu pago con Stripe…") else t("Tu pago aún no está confirmado"),
            fontFamily = Playfair,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Charcoal
        )
        Text(
            if (checking) t("No cierres la app.")
            else t("Si ya pagaste, la confirmación puede tardar un poco. Si no terminaste, ") +
                t("puedes volver a la pasarela o cancelar el pedido; tu carrito se conserva."),
            fontSize = 15.sp,
            color = Charcoal.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(20.dp))
        ButtonStack {
            TerraButton(
                if (checking) t("Consultando...") else t("Ya pagué, consultar"),
                onClick = onCheck,
                enabled = !checking,
                modifier = Modifier.fillMaxWidth()
            )
            if (canPayAgain) {
                Spacer(Modifier.height(10.dp))
                TerraButton(
                    t("Volver a la pasarela"),
                    onClick = onPayAgain,
                    enabled = !checking,
                    style = ButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            TerraButton(
                t("Cancelar pedido"),
                onClick = onCancel,
                enabled = !checking,
                style = ButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
