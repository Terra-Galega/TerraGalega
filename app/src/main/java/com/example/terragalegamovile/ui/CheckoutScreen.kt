package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.ForestText

import com.example.terragalegamovile.ui.theme.TerraText

import androidx.compose.foundation.layout.imePadding

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.*
import com.example.terragalegamovile.ui.components.*
import com.example.terragalegamovile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun FieldError(text: String?) {
    if (text != null) Text(text, color = ErrorText, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Charcoal)
    Text(subtitle, fontSize = 15.sp, color = Charcoal.copy(alpha = 0.65f), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
}

@Composable
fun CheckoutScreen(
    user: LoginResponse,
    onLogout: () -> Unit,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    val d = CheckoutDraft
    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var addressesLoaded by remember { mutableStateOf(false) }
    var newMode by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun isSaved(text: String) = addresses.any { it.label.trim().equals(text.trim(), ignoreCase = true) }

    // Direcciones guardadas del cliente (las mismas de la web) y limpieza de productos no disponibles
    LaunchedEffect(Unit) {
        try {
            addresses = ApiClient.api.addresses()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        addressesLoaded = true
        if (d.address.isBlank() && addresses.isNotEmpty()) {
            d.address = (addresses.firstOrNull { it.defaultAddress == true } ?: addresses.first()).label
        }
        newMode = addresses.isEmpty() || (d.address.isNotBlank() && !isSaved(d.address))
        val removed = CartStore.removeUnavailable()
        if (removed.isNotEmpty()) notice = unavailableMessage(removed)
    }

    // Mismas reglas de validación que la web
    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        val address = d.address.trim()
        if (address.isEmpty()) e["address"] = t("Escribe la dirección donde quieres recibir tu pedido.")
        else if (address.length > CheckoutDraft.ADDRESS_MAX) e["address"] = t("La dirección no puede superar 120 caracteres.")

        val name = d.addressName.trim()
        if (d.saveAddress && address.isNotEmpty() && !isSaved(address)) {
            if (name.length > CheckoutDraft.NAME_MAX) e["addressName"] = t("El nombre no puede superar 40 caracteres.")
            else if (name.isNotEmpty() && addresses.any { (it.name ?: "").trim().equals(name, ignoreCase = true) })
                e["addressName"] = t("Ya tienes una dirección con ese nombre.")
        }
        if (d.method.isEmpty()) e["method"] = t("Elige cómo quieres pagar.")
        // La tarjeta no se valida aquí: se escribe en la página de Stripe
        return e
    }

    val errors = if (submitted) validate() else emptyMap()

    fun goNext() {
        submitted = true
        if (validate().isNotEmpty() || CartStore.items.isEmpty()) return
        d.address = d.address.trim()
        d.addressName = d.addressName.trim()
        d.saveAddress = d.saveAddress && !isSaved(d.address)
        onContinue()
    }

    TerraScaffold(role = "Cliente", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            PageHeader(t("Paso 1 de 2 · Pasarela de pago"), t("Entrega y pago"))
            Spacer(Modifier.height(28.dp))

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

            // ---------- Dirección ----------
            StepTitle(t("Dirección de entrega"), t("¿Dónde quieres recibir tu pedido?"))
            if (!addressesLoaded) {
                Text(t("Cargando tus direcciones..."), fontSize = 14.sp, color = TextSoft)
            } else {
                addresses.forEach { a ->
                    val selected = !newMode && d.address.trim().equals(a.label.trim(), ignoreCase = true)
                    SelectCard(
                        selected = selected,
                        onClick = { newMode = false; d.address = a.label },
                        badge = if (a.defaultAddress == true) "Predeterminada" else null,
                        icon = { AddressIconBox(selected) }
                    ) {
                        Text(a.name ?: t("Casa"), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                        if (selected) Text(a.label, fontSize = 14.sp, color = TextSoft)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (addresses.isNotEmpty()) {
                    SelectCard(
                        selected = newMode,
                        onClick = { newMode = true; d.address = ""; d.addressName = ""; d.saveAddress = true },
                        icon = { AddressIconBox(newMode, plus = true) }
                    ) {
                        Text(t("Usar otra dirección"), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                    }
                }
                // Dirección nueva: texto arriba y mapa debajo, sincronizados.
                // Escribir mueve el mapa; mover el mapa (o tocarlo) actualiza el texto.
                if (newMode) {
                    Spacer(Modifier.height(12.dp))
                    TerraTextField(
                        label = t("Dirección"),
                        value = d.address,
                        onValueChange = { if (it.length <= CheckoutDraft.ADDRESS_MAX) d.address = it },
                        placeholder = t("Ej: Calle 123 #45-67, apto 8")
                    )
                    Text(
                        "${d.address.length}/120",
                        fontSize = 12.sp,
                        color = TextSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    AddressMapPicker(
                        address = d.address,
                        onPicked = { d.address = it }
                    )
                }
                FieldError(errors["address"])

                if (newMode && d.address.isNotBlank() && !isSaved(d.address)) {
                    Spacer(Modifier.height(8.dp))
                    CheckRow(t("Guardar esta dirección en mi perfil"), d.saveAddress, accent = Terra) {
                        d.saveAddress = !d.saveAddress
                    }
                    if (d.saveAddress) {
                        TerraTextField(
                            label = t("Nombre de la dirección"),
                            value = d.addressName,
                            onValueChange = { if (it.length <= CheckoutDraft.NAME_MAX) d.addressName = it },
                            placeholder = t("Casa")
                        )
                        FieldError(errors["addressName"])
                    }
                }
            }

            // ---------- Método de pago ----------
            Spacer(Modifier.height(36.dp))
            StepTitle(t("Método de pago"), t("Elige cómo quieres pagar tu pedido."))
            SelectCard(selected = d.method == "cash", onClick = { d.method = "cash" }) {
                Text(t("Contra entrega"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                Text(t("Pagas en efectivo cuando recibas tu pedido."), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(10.dp))
            SelectCard(selected = d.method == "card", onClick = { d.method = "card" }) {
                Text(t("Tarjeta"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                Text(t("Crédito o débito, en la pasarela segura de Stripe."), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.6f))
            }
            FieldError(errors["method"])

            if (d.method == "cash") {
                Spacer(Modifier.height(16.dp))
                Text(
                    t("Pagarás el total en efectivo cuando el domiciliario llegue con tu pedido."),
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.7f)
                )
            }

            if (d.method == "card") {
                Spacer(Modifier.height(16.dp))
                Text(
                    t("Al confirmar el pedido abriremos la pasarela segura de Stripe para que ") +
                        t("ingreses tu tarjeta. TerraGalega nunca ve ni guarda sus datos."),
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.7f)
                )
            }

            // ---------- Cupón ----------
            Spacer(Modifier.height(36.dp))
            CouponBox()


            // ---------- Resumen ----------
            Spacer(Modifier.height(36.dp))
            OrderSummaryCard( items = CartStore.items,
                deliveryFee = 0.0,
                discount = d.coupon?.discount ?: 0.0,
                couponCode = d.coupon?.code) {
                Spacer(Modifier.height(24.dp))
                ButtonStack {
                    TerraButton(t("Continuar"), onClick = { goNext() }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    t("Cancelar compra"),
                    color = Charcoal.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterHorizontally)
                        .clickable { showCancel = true }
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
    notice?.let {
        TerraDialog(t("Actualizamos tu carrito"), it, t("Aceptar"), onConfirm = { notice = null })
    }
}

// Cupón del pedido: escribir el código o elegir uno de los disponibles.
// El descuento mostrado es informativo: el back vuelve a validarlo al crear el pedido.
@Composable
private fun CouponBox() {
    val d = CheckoutDraft
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var applying by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<List<Coupon>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            available = ApiClient.api.availableCoupons()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Si falla, igual puede escribir el código a mano
        }
    }

    // Si cambia el subtotal, el descuento (y hasta la validez del cupón) puede cambiar
    LaunchedEffect(CartStore.subtotal) {
        val current = d.coupon ?: return@LaunchedEffect
        try {
            d.coupon = ApiClient.api.validateCoupon(current.code, CartStore.subtotal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            d.coupon = null
            error = t("Quitamos el cupón {}. {}", current.code, e.toUserMessage())
        }
    }

    fun apply(text: String) {
        val value = text.trim()
        if (value.isEmpty()) {
            error = t("Escribe el código de tu cupón.")
            return
        }
        if (applying) return
        applying = true
        error = ""
        scope.launch {
            try {
                d.coupon = ApiClient.api.validateCoupon(value, CartStore.subtotal)
                code = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toUserMessage()
            }
            applying = false
        }
    }

    StepTitle(t("Cupón de descuento"), t("¿Tienes un cupón? Escribe el código o elige uno."))

    val applied = d.coupon
    if (applied != null) {
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(t("Cupón {}", applied.code), fontWeight = FontWeight.SemiBold, color = Charcoal)
                    Text(t("Descuento: -{}", Format.money(applied.discount)), fontSize = 14.sp, color = ForestText)
                }
                Text(
                    t("Quitar"),
                    color = TerraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { d.coupon = null; error = "" }.padding(8.dp)
                )
            }
        }
    } else {
        TerraTextField(
            label = t("Código del cupón"),
            value = code,
            onValueChange = { code = it },
            placeholder = t("Ej: BIENVENIDA-1"),
            onSubmit = { if (!applying && code.isNotBlank()) apply(code) }
        )
        Spacer(Modifier.height(10.dp))
        ButtonStack {
            TerraButton(
                if (applying) t("Aplicando...") else t("Aplicar"),
                onClick = { apply(code) },
                enabled = !applying,
                style = ButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
        available.forEach { c ->
            Spacer(Modifier.height(10.dp))
            SelectCard(selected = false, onClick = { apply(c.code) }) {
                Text(c.code, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                Text(
                    t("{} de descuento", Format.couponValue(c.discountType, c.discountValue)) +
                            (c.description?.let { " · $it" } ?: ""),
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.6f)
                )
            }
        }
    }
    if (error.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        ErrorNotice(error)
    }
}