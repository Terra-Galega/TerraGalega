package com.example.terragalegamovile.ui

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

@Composable
private fun FieldError(text: String?) {
    if (text != null) Text(text, color = Burgundy, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
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
    var entry by rememberSaveable { mutableStateOf(0) }   // 0 = escribirla, 1 = ubicar en el mapa

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
        if (address.isEmpty()) e["address"] = "Escribe la dirección donde quieres recibir tu pedido."
        else if (address.length > CheckoutDraft.ADDRESS_MAX) e["address"] = "La dirección no puede superar 120 caracteres."

        val name = d.addressName.trim()
        if (d.saveAddress && address.isNotEmpty() && !isSaved(address)) {
            if (name.length > CheckoutDraft.NAME_MAX) e["addressName"] = "El nombre no puede superar 40 caracteres."
            else if (name.isNotEmpty() && addresses.any { (it.name ?: "").trim().equals(name, ignoreCase = true) })
                e["addressName"] = "Ya tienes una dirección con ese nombre."
        }
        if (d.method.isEmpty()) e["method"] = "Elige cómo quieres pagar."
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
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            PageHeader("Paso 1 de 2 · Pasarela de pago", "Entrega y pago")
            Spacer(Modifier.height(28.dp))

            if (CartStore.items.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 32.dp) {
                    Text("Tu carrito está vacío. Agrega algo de la carta antes de pagar.", color = TextSoft)
                    Spacer(Modifier.height(16.dp))
                    TerraButton("Explorar la carta", onClick = onExit, modifier = Modifier.fillMaxWidth())
                }
                return@TerraScaffold
            }

            // ---------- Dirección ----------
            StepTitle("Dirección de entrega", "¿Dónde quieres recibir tu pedido?")
            if (!addressesLoaded) {
                Text("Cargando tus direcciones...", fontSize = 14.sp, color = TextSoft)
            } else {
                addresses.forEach { a ->
                    val selected = !newMode && d.address.trim().equals(a.label.trim(), ignoreCase = true)
                    SelectCard(
                        selected = selected,
                        onClick = { newMode = false; d.address = a.label },
                        badge = if (a.defaultAddress == true) "Predeterminada" else null,
                        icon = { AddressIconBox(selected) }
                    ) {
                        Text(a.name ?: "Casa", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
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
                        Text("Usar otra dirección", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                    }
                }
                if (newMode) {
                    Spacer(Modifier.height(12.dp))
                    AddressEntryTabs(entry) { entry = it }
                    Spacer(Modifier.height(12.dp))
                    if (entry == 0) {
                        TerraTextField(
                            label = "Dirección",
                            value = d.address,
                            onValueChange = { if (it.length <= CheckoutDraft.ADDRESS_MAX) d.address = it },
                            placeholder = "Ej: Calle 123 #45-67, apto 8"
                        )
                        Text(
                            "${d.address.length}/120",
                            fontSize = 12.sp,
                            color = TextSoft,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        AddressMapPicker(
                            address = d.address,
                            onPicked = { d.address = it }
                        )
                        Text(
                            "¿Falta el apartamento o la torre? Pasa a \"Escribirla\" y agrégalo.",
                            fontSize = 12.sp,
                            color = TextSoft,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                FieldError(errors["address"])

                if (newMode && d.address.isNotBlank() && !isSaved(d.address)) {
                    Spacer(Modifier.height(8.dp))
                    CheckRow("Guardar esta dirección en mi perfil", d.saveAddress, accent = Terra) {
                        d.saveAddress = !d.saveAddress
                    }
                    if (d.saveAddress) {
                        TerraTextField(
                            label = "Nombre de la dirección",
                            value = d.addressName,
                            onValueChange = { if (it.length <= CheckoutDraft.NAME_MAX) d.addressName = it },
                            placeholder = "Casa"
                        )
                        FieldError(errors["addressName"])
                    }
                }
            }

            // ---------- Método de pago ----------
            Spacer(Modifier.height(36.dp))
            StepTitle("Método de pago", "Elige cómo quieres pagar tu pedido.")
            SelectCard(selected = d.method == "cash", onClick = { d.method = "cash" }) {
                Text("Contra entrega", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                Text("Pagas en efectivo cuando recibas tu pedido.", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(10.dp))
            SelectCard(selected = d.method == "card", onClick = { d.method = "card" }) {
                Text("Tarjeta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                Text("Crédito o débito, en la pasarela segura de Stripe.", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.6f))
            }
            FieldError(errors["method"])

            if (d.method == "cash") {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pagarás el total en efectivo cuando el domiciliario llegue con tu pedido.",
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.7f)
                )
            }

            if (d.method == "card") {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Al confirmar el pedido abriremos la pasarela segura de Stripe para que " +
                        "ingreses tu tarjeta. TerraGalega nunca ve ni guarda sus datos.",
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.7f)
                )
            }

            // ---------- Resumen ----------
            Spacer(Modifier.height(36.dp))
            OrderSummaryCard(items = CartStore.items, deliveryFee = 0.0) {
                Spacer(Modifier.height(24.dp))
                TerraButton("Continuar", onClick = { goNext() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text(
                    "Cancelar compra",
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
            title = "Cancelar compra",
            message = "¿Seguro que quieres cancelar la compra?\n\nTu carrito se conservará.",
            confirmLabel = "Sí, cancelar compra",
            cancelLabel = "Continuar comprando",
            danger = true,
            onConfirm = { showCancel = false; CheckoutDraft.reset(); onExit() },
            onCancel = { showCancel = false }
        )
    }
    notice?.let {
        TerraDialog("Actualizamos tu carrito", it, "Aceptar", onConfirm = { notice = null })
    }
}

@Composable
private fun AddressEntryTabs(selected: Int, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Charcoal.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        listOf("Escribirla", "Ubicar en el mapa").forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onChange(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) Terra else Charcoal.copy(alpha = 0.55f)
                )
            }
        }
    }
}