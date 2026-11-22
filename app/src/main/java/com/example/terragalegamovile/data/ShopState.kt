package com.example.terragalegamovile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2 (igual que CheckoutDraftService de la web)
object CheckoutDraft {
    const val ADDRESS_MAX = 120
    const val NAME_MAX = 40

    var address by mutableStateOf("")
    var saveAddress by mutableStateOf(true)
    var addressName by mutableStateOf("")
    // "cash" | "card" | "". Con "card" los datos de la tarjeta se escriben en
    // la página de Stripe: la app nunca los ve.
    var method by mutableStateOf("")
    // Cupón aplicado en el paso 1 (null = ninguno). El descuento es informativo:
    // el back vuelve a validarlo al crear el pedido.
    var coupon by mutableStateOf<CouponDiscount?>(null)

    // Intento de pago con tarjeta. Mismo carrito = mismo id, así reintentar
    // tras un fallo de red no crea otro pedido (igual que la web).
    var paymentRequestId = ""
    var paymentRequestBody = ""
    // Pedido que se está pagando en Stripe (null = ninguno)
    var pendingOrderId by mutableStateOf<Int?>(null)

    fun reset() {
        address = ""; saveAddress = true; addressName = ""; method = ""
        coupon = null
        paymentRequestId = ""; paymentRequestBody = ""; pendingOrderId = null
    }
}

// Estado de la pantalla principal del cliente
object ClientUi {
    var tab by mutableStateOf(0) // 0 = Carta, 1 = Mis pedidos
    var paid by mutableStateOf(false) // aviso verde "¡Tu orden fue añadida con éxito!"

    var lastPaidOrderId by mutableStateOf<Int?>(null) // pedido pagado con tarjeta: muestra "Ver mi factura"

}

// Mismo texto que unavailableMessage() de la web
fun unavailableMessage(names: List<String>): String =
    if (names.size == 1) {
        t("El producto {} ya no está disponible y lo quitamos de tu carrito.", names[0])
    } else {
        val list = names.joinToString(", ") { "\"$it\"" }
        t("Estos productos ya no están disponibles y los quitamos de tu carrito: {}.", list)
    }

// Quita del carrito los productos que el admin desactivó y devuelve sus nombres
fun CartStore.removeUnavailable(): List<String> {
    val gone = items.filter { !it.product.active }
    gone.forEach { remove(it) }
    return gone.map { it.product.name }.distinct()
}