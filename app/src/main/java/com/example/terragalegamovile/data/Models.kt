package com.example.terragalegamovile.data

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token : String,
    val id: Int,
    val name: String,
    val lastName: String,
    val address: String? = null,
    val role: String,
    // false = cuenta creada con Google a la que le faltan teléfono, apellido o
    // dirección (se muestra "Completa tu perfil"). null en sesiones antiguas = completa.
    val profileComplete: Boolean? = null
)

// POST /auth/google
data class GoogleLoginRequest(val idToken: String)

// PUT /clients/me/complete-profile. address solo si aún no tiene ninguna.
data class CompleteProfileRequest(
    val name: String,
    val lastName: String,
    val phone: String,
    val address: NewClientAddress?
)

// Lo que ve el cliente: su PIN y dónde va el repartidor
data class ClientDelivery(
    val orderId: Int,
    val status: String,
    val address: String?,
    val pin: String?,
    val deliveryPersonName: String?,
    val deliveryPersonPhone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationUpdatedAt: String?,
    val startedAt: String? = null,
    val route: List<LatLon>? = null
)

// Lo que ve el repartidor (nunca trae el PIN)
data class DeliveryJob(
    val orderId: Int,
    val status: String,
    val address: String?,
    val clientName: String?,
    val clientPhone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationUpdatedAt: String?,
    val attemptsLeft: Int,
    val startedAt: String? = null,
    val route: List<LatLon>? = null
)

data class LocationRequest(val latitude: Double, val longitude: Double)

data class CompleteRequest(val pin: String)

// Forma en que el backend devuelve los errores
// code: p. ej. "EMAIL_NOT_VERIFIED" cuando la contraseña es correcta pero falta confirmar el correo
data class ApiErrorBody(val message: String?, val code: String? = null)

// Respuesta de /auth/signup y /auth/resend-verification: solo un texto
data class MessageResponse(val message: String?)

// POST /auth/resend-verification
data class EmailRequest(val email: String)
// ---------- Menú, carrito y pedidos (mismos datos que la web) ----------

data class CategoryRef(val id: Int, val name: String)

data class Category(val id: Int, val name: String, val description: String?)

data class StartDeliveryRequest(
    val route: List<LatLon>
)

data class Product(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val category: CategoryRef?,
    val imageUrl: String?,
    val active: Boolean,
    val popular: Boolean,
    val vegetarian: Boolean,
    val spicyMild: Boolean,
    val spicyHot: Boolean,
    val containsNuts: Boolean,
    val containsSeafood: Boolean,
    val containsGluten: Boolean
)

// Detalle de un producto: la categoría llega con su descripción
data class ProductDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val category: Category?,
    val imageUrl: String?,
    val active: Boolean,
    val popular: Boolean,
    val vegetarian: Boolean,
    val spicyMild: Boolean,
    val spicyHot: Boolean,
    val containsNuts: Boolean,
    val containsSeafood: Boolean,
    val containsGluten: Boolean
)

fun ProductDetail.toProduct() = Product(
    id, name, description, price,
    category?.let { CategoryRef(it.id, it.name) },
    imageUrl, active, popular, vegetarian, spicyMild, spicyHot,
    containsNuts, containsSeafood, containsGluten
)

data class AddOnOption(val id: Int, val name: String, val price: Double)

// Una línea del carrito compartido (id = "productId:addOnId-addOnId", igual que en la web)
data class CartItem(
    val id: String,
    val product: Product,
    val quantity: Int,
    val addOns: List<AddOnOption>
)

data class CheckoutItemRequest(val productId: Int, val quantity: Int, val addOnIds: List<Int>)

data class QuantityRequest(val quantity: Int)

data class CheckoutRequest(val address: String, val items: List<CheckoutItemRequest>)

// POST /payments/start. channel = "app" hace que Stripe vuelva a la app.
data class StartPaymentRequest(
    val requestId: String,
    val checkout: CheckoutRequest,
    val channel: String = "app"
)

// Respuesta de /payments/*. state: PENDING | PAID | EXPIRED |
// REFUND_PENDING | REFUNDED | REFUND_FAILED
data class Payment(
    val orderId: Int,
    val state: String,
    // Página de Stripe donde se paga; solo viene mientras está PENDING
    val checkoutUrl: String?,
    val amountInCents: Long,
    val currency: String
)

data class InvoiceLink(val previewUrl: String, val qrPngBase64: String)

data class Address(
    val id: Int,
    val name: String?,
    val label: String,
    val defaultAddress: Boolean?
)

data class AddressInput(val label: String, val name: String? = null)

data class OrderAddOn(val id: Int, val name: String, val price: Double)

data class OrderProduct(val id: Int, val name: String, val imageUrl: String?)

data class OrderLine(
    val id: Int,
    val product: OrderProduct,
    val quantity: Int,
    val unitPrice: Double,
    val addOns: List<OrderAddOn>
)

data class Order(
    val id: Int,
    val status: String,
    val createdAt: String?,
    val deliveredAt: String?,
    val address: String?,
    val details: List<OrderLine>,
    val total: Double
)

// Perfil del cliente (GET/PUT /clients/{id})
data class ClientProfile(
    val id: Int,
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String
)

data class ClientUpdate(
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)


// Registro de un cliente nuevo (POST /clients), igual que la web
data class NewClientAddress(val label: String)

data class ClientCreate(
    val name: String,
    val lastName: String,
    val email: String,
    val password: String,
    val phone: String,
    val role: String = "CLIENT",
    val addresses: List<NewClientAddress>
)