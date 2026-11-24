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

// GET /maps/search y /maps/reverse: texto de la dirección y su punto
data class MapAddress(val label: String, val lat: Double, val lon: Double)

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
    val route: List<LatLon>? = null,
    // Avance del recorrido de 0.0 a 1.0 (lo calcula el backend; null si aún no inicia)
    val progress: Double? = null
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

// ---------- Asistente virtual (POST clients/me/chat, el mismo de la web) ----------

// role: "user" o "model"
data class ChatMessage(val role: String, val text: String)

data class ChatRequest(val message: String, val history: List<ChatMessage>)

// Reserva que el asistente creó o consultó (para la tarjeta con el botón de pagar)
data class ChatReservation(
    val id: Int,
    val name: String,
    val tableId: Int,
    val floor: Int,
    // AAAA-MM-DD y HH:mm:ss
    val date: String,
    val time: String,
    val seats: Int,
    val depositAmount: Double,
    // PENDING, PAID...
    val paymentState: String?,
    val checkoutUrl: String?
)

// cartChanged: el asistente cambió el carrito y hay que recargarlo
data class ChatResponse(
    val reply: String,
    val cartChanged: Boolean,
    val reservation: ChatReservation?
)

// ---------- Chat con el restaurante (operador) sobre un pedido: /order-chat ----------

// senderRole: CLIENT u OPERATOR. content es null si es solo una foto; attachmentId null si es solo texto
data class OrderChatMessage(
    val id: Int,
    val senderRole: String,
    val senderName: String,
    val content: String?,
    val attachmentId: Int?,
    val createdAt: String
)

// open = false cuando el pedido ya terminó (entregado o cancelado)
data class OrderChat(
    val orderId: Int,
    val orderStatus: String,
    val open: Boolean,
    val messages: List<OrderChatMessage>
)

// Pedido activo que ya tiene mensajes
data class OrderChatSummary(
    val orderId: Int,
    val orderStatus: String,
    val lastMessageId: Int,
    val lastSenderRole: String,
    val lastMessageAt: String?
)

data class SendChatMessage(val content: String)

// ---------- Suscripción Premium y puntos (GET subscriptions/status, POST subscriptions/start) ----------
data class SubscriptionStatus(
    val subscribed: Boolean,
    val pointsBalance: Long,
    val pointValueCop: Double,
    // Vencimiento (ISO); null = nunca se ha suscrito
    val subscriptionEndsAt: String?,
    val daysLeft: Int,
    // Puede pagar ahora (no tiene un pago pendiente ni está suscrito sin poder renovar)
    val canSubscribe: Boolean,
    val priceCop: Double,
    val durationDays: Int,
    // PENDING, PAID o EXPIRED (null si nunca ha pagado)
    val paymentState: String?,
    // Página de Stripe mientras el pago está PENDING
    val checkoutUrl: String?
)

// Movimiento de puntos: EARNED, SPENT o RETURNED
data class PointsMovement(val orderId: Int?, val kind: String, val amount: Long, val createdAt: String?)

// POST auth/reset-password: token del enlace del correo y la contraseña nueva
data class ResetPasswordRequest(val token: String, val newPassword: String)
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

data class CheckoutRequest(
    val address: String,
    val items: List<CheckoutItemRequest>,
    val couponCode: String? = null
)

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
    val total: Double,
    // Operador que maneja el pedido (llega al empezar a prepararlo)
    val operatorId: Int? = null
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

// ---------- Notificaciones y cupones ----------

// GET /notifications/me?platform=mobile
data class AppNotification(
    val id: Int,
    // WELCOME | COUPON | ORDER_CONFIRMED | ORDER_PREPARING | ORDER_ON_THE_WAY | ORDER_NEAR
    val type: String,
    val title: String,
    val message: String,
    val orderId: Int?,
    val read: Boolean,
    val createdAt: String?,
    // Solo en los avisos del mesero (DISH_PREPARING | DISH_READY | ORDER_READY)
    val tableId: Int? = null
)

// GET /coupons/me (los que se pueden usar ahora, para el checkout)
data class Coupon(
    val id: Int,
    val code: String,
    val description: String?,
    val discountType: String, // "PERCENTAGE" | "FIXED_AMOUNT"
    val discountValue: Double,
    val maxDiscount: Double?,
    val minOrderAmount: Double?,
    val expiresAt: String?
)

// GET /coupons/me/all ("Mis cupones", con su estado)
data class ClientCoupon(
    val id: Int,
    val code: String,
    val description: String?,
    val discountType: String,
    val discountValue: Double,
    val maxDiscount: Double?,
    val minOrderAmount: Double?,
    val expiresAt: String?,
    val general: Boolean,
    val status: String // ACTIVE | EXPIRED | USED | EXHAUSTED
)

// GET /coupons/validate: cuánto descontaría el cupón sobre el subtotal
data class CouponDiscount(val code: String, val description: String?, val discount: Double)
// ---------- Pedidos de mesa (portal del mesero) ----------

// Una mesa del piso elegido (GET /table-orders/tables?floor=)
// Reserva de hoy de una mesa (filtro "Reservadas hoy"). time = "19:30:00"
data class TodayReservation(
    val id: Int,
    val time: String,
    val name: String?,
    val clientName: String?,
    val seats: Int?,
    val paid: Boolean,
    // Ya llegaron (se tomó su pedido)
    val seated: Boolean
)

data class WaiterTable(
    val id: Int,
    val floor: Int,
    val seats: Int,
    // true = ya se le entregó todo
    val attended: Boolean,
    // Pedidos en curso
    val activeOrders: Int,
    // Algún pedido está listo esperando en cocina
    val readyToServe: Boolean,
    // Pedidos entregados que faltan por cobrar. Libre = activeOrders y awaitingPayment en 0.
    val awaitingPayment: Int = 0,
    // Ocupada desde que el mesero toma el pedido hasta que se cobra todo
    val occupied: Boolean = false,
    // Reserva de hoy en las próximas 2 horas ("19:30:00"), o null
    val upcomingReservation: String?,
    val upcomingReservationName: String?,
    // Reserva cuyo turno está en curso ahora (null si no hay)
    val currentReservation: String? = null,
    val currentReservationName: String? = null,
    // Mesero que atiende la mesa ocupada (null si está libre)
    val attendingWaiterId: Int? = null,
    val attendingWaiterName: String? = null,
    // true = la atiendo yo; ocupada y false = la atiende otro (no se puede entrar)
    val mine: Boolean = false,
    // Reserva a tener en cuenta al sentar a alguien (en curso sin llegar, o que
    // empieza antes de que la mesa quede libre). null = ninguna.
    val reservationId: Int? = null,
    val reservationTime: String? = null,
    val reservationName: String? = null,
    val reservationPaid: Boolean = false,
    // Pasó el margen de espera y no llegaron
    val reservationNoShow: Boolean = false,
    // ¿Se puede sentar aquí a clientes sin reserva?
    val walkInAllowed: Boolean = true,
    // Nombre y apellido de quien hizo la reserva de arriba
    val reservationClientName: String? = null,
    // Todas las reservas de hoy de la mesa
    val todayReservations: List<TodayReservation> = emptyList()
)

// Estado de cada plato: PENDING (en espera) | PREPARING | READY
data class TableOrderItem(
    val id: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val notes: String?,
    // Nombres de los adicionales del plato
    val addOns: List<String> = emptyList(),
    val status: String
)

// Estado del pedido: PENDING | IN_PROGRESS | DONE | CANCELLED
data class TableOrder(
    val id: Int,
    val tableId: Int,
    val floor: Int,
    val waiterId: Int,
    val waiterName: String,
    val startTime: String,
    val endTime: String?,
    val status: String,
    // Todo listo: esperando en cocina a que el mesero lo recoja
    val ready: Boolean,
    // Aún se puede cancelar (la cocina no empezó ningún plato)
    val cancellable: Boolean,
    // Entregado y aún sin cobrar: el mesero elige efectivo o tarjeta
    val awaitingPayment: Boolean = false,
    // CASH | CARD (null mientras no se cobra)
    val paymentMethod: String? = null,
    val paidAt: String? = null,
    val items: List<TableOrderItem>
)

// addOnIds: adicionales de la categoría del plato (opcional)
data class NewOrderItem(val productId: Int, val quantity: Int, val notes: String?, val addOnIds: List<Int> = emptyList())

// POST /table-orders/{id}/pay: "CASH" (efectivo) o "CARD" (tarjeta)
data class PayRequest(val method: String)

// POST /table-orders (el mesero va en el token)
// clientRequestId: id del pedido generado en la app. Si se reintenta el envío
// (se cortó la red), el back devuelve el mismo pedido y no manda otro a cocina.
// forReservation: true = son los clientes de la reserva de la mesa.
data class NewTableOrder(
    val tableId: Int,
    val items: List<NewOrderItem>,
    val clientRequestId: String? = null,
    val forReservation: Boolean? = null
)
