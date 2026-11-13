package com.example.terragalegamovile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Lo que el cliente elige en el paso 1 y se muestra en el paso 2 (igual que CheckoutDraftService de la web)
object CheckoutDraft {
    const val ADDRESS_MAX = 120
    const val NAME_MAX = 40
    val INSTALLMENTS = listOf(1, 3, 6, 12, 24, 36)

    var address by mutableStateOf("")
    var saveAddress by mutableStateOf(true)
    var addressName by mutableStateOf("")
    var method by mutableStateOf("") // "cash" | "card" | ""
    var holder by mutableStateOf("")
    var documentType by mutableStateOf("CC")
    var documentNumber by mutableStateOf("")
    var number by mutableStateOf("")
    var expiry by mutableStateOf("")
    var cvv by mutableStateOf("")
    var installments by mutableStateOf(1)

    fun reset() {
        address = ""; saveAddress = true; addressName = ""; method = ""
        holder = ""; documentType = "CC"; documentNumber = ""
        number = ""; expiry = ""; cvv = ""; installments = 1
    }
}

// Estado de la pantalla principal del cliente
object ClientUi {
    var tab by mutableStateOf(0) // 0 = Carta, 1 = Mis pedidos
    var paid by mutableStateOf(false) // aviso verde "¡Tu orden fue añadida con éxito!"
}

// Mismo texto que unavailableMessage() de la web
fun unavailableMessage(names: List<String>): String =
    if (names.size == 1) {
        "El producto ${names[0]} ya no está disponible y lo quitamos de tu carrito."
    } else {
        val list = names.joinToString(", ") { "\"$it\"" }
        "Estos productos ya no están disponibles y los quitamos de tu carrito: $list."
    }

// Quita del carrito los productos que el admin desactivó y devuelve sus nombres
fun CartStore.removeUnavailable(): List<String> {
    val gone = items.filter { !it.product.active }
    gone.forEach { remove(it) }
    return gone.map { it.product.name }.distinct()
}