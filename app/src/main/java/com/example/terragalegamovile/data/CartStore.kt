package com.example.terragalegamovile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Carrito del cliente: es el MISMO que ve la web (vive en Postgres).
object CartStore {

    var items by mutableStateOf<List<CartItem>>(emptyList())
        private set

    private var clientId: Int? = null
    // Cambios nuestros en vuelo: mientras haya, no se pisa el carrito con una lectura vieja
    private var pending = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val count: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { unitPrice(it) * it.quantity }

    fun unitPrice(item: CartItem): Double = item.product.price + item.addOns.sumOf { it.price }

    // Al iniciar o cerrar sesión (null = sin cliente)
    fun bind(id: Int?) {
        if (clientId == id) return
        clientId = id
        items = emptyList()
        if (id != null) refresh()
    }

    // Vuelve a leer el carrito del servidor (se llama cada pocos segundos)
    fun refresh() {
        val id = clientId ?: return
        if (pending > 0) return
        scope.launch {
            try {
                val list = ApiClient.api.cart(id)
                if (clientId == id && pending == 0) items = list
            } catch (_: Exception) {
                // Sin conexión: se queda lo que ya se veía
            }
        }
    }

    // onDone recibe null si salió bien, o el mensaje de error
    fun add(
        product: Product,
        quantity: Int,
        addOns: List<AddOnOption>,
        onDone: (String?) -> Unit = {}
    ) {
        val lineId = lineId(product.id, addOns)
        val optimistic =
            if (items.any { it.id == lineId })
                items.map { if (it.id == lineId) it.copy(quantity = it.quantity + quantity) else it }
            else items + CartItem(lineId, product, quantity, addOns)

        call(optimistic, onDone) { id ->
            ApiClient.api.cartAdd(id, CheckoutItemRequest(product.id, quantity, addOns.map { it.id }))
        }
    }

    fun setQuantity(item: CartItem, quantity: Int) {
        val optimistic =
            if (quantity < 1) items.filter { it.id != item.id }
            else items.map { if (it.id == item.id) it.copy(quantity = quantity) else it }

        call(optimistic) { id -> ApiClient.api.cartQuantity(id, item.id, QuantityRequest(quantity)) }
    }

    fun remove(item: CartItem) {
        call(items.filter { it.id != item.id }) { id -> ApiClient.api.cartRemove(id, item.id) }
    }

    fun clear() {
        call(emptyList()) { id -> ApiClient.api.cartClear(id) }
    }

    // Misma regla que el servidor y que buildId() de la web
    private fun lineId(productId: Int, addOns: List<AddOnOption>): String =
        "$productId:${addOns.map { it.id }.sorted().joinToString("-")}"

    private fun call(
        optimistic: List<CartItem>?,
        onDone: (String?) -> Unit = {},
        request: suspend (Int) -> List<CartItem>
    ) {
        val id = clientId ?: return
        if (optimistic != null) items = optimistic
        pending++
        scope.launch {
            var error: String? = null
            try {
                val list = request(id)
                // Solo se acepta la respuesta si no hay otro cambio más reciente en vuelo
                if (pending == 1 && clientId == id) items = list
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                pending--
            }
            if (error != null) refresh()
            onDone(error)
        }
    }
}