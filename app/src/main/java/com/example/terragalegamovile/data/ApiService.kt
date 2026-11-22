package com.example.terragalegamovile.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

// El usuario con sesión se identifica por el token que el interceptor de ApiClient
// agrega en cada petición, así que las rutas del propio usuario no llevan su id.
interface ApiService {

    // ---------- Sesión ----------
    // La respuesta trae el token, que se guarda junto con la sesión
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // Registra un cliente. NO inicia sesión: el back envía un correo y la cuenta
    // solo puede entrar después de confirmarlo (igual que en la web)
    @POST("auth/signup")
    suspend fun register(@Body body: ClientCreate): MessageResponse

    // Vuelve a enviar el correo de confirmación (responde igual exista o no la cuenta)
    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body body: EmailRequest): MessageResponse

    // Recuperar contraseña (igual que la web): envía el enlace al correo
    // (responde igual exista o no la cuenta) y cambia la contraseña con el token del enlace
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse

    // Asistente virtual del cliente (Gemini en el back). Se llama con ApiClient.chatApi,
    // que espera más tiempo la respuesta.
    @POST("clients/me/chat")
    suspend fun chat(@Body body: ChatRequest): ChatResponse

    // ---------- Premium y puntos (los mismos de la web) ----------
    @GET("subscriptions/status")
    suspend fun subscriptionStatus(): SubscriptionStatus

    @GET("subscriptions/points/history")
    suspend fun pointsHistory(): List<PointsMovement>

    // Empieza (o renueva) el pago; trae checkoutUrl de Stripe
    @POST("subscriptions/start")
    suspend fun subscriptionStart(): SubscriptionStatus

    // ---------- Chat con el restaurante (el mismo de la web) ----------
    // Pedidos en curso del cliente que ya tienen mensajes
    @GET("order-chat/active")
    suspend fun orderChatActive(): List<OrderChatSummary>

    // Mensajes de un pedido; con "after" solo llegan los nuevos
    @GET("order-chat/{orderId}/messages")
    suspend fun orderChatMessages(@Path("orderId") orderId: Int, @Query("after") after: Int?): OrderChat

    @POST("order-chat/{orderId}/messages")
    suspend fun sendOrderChat(@Path("orderId") orderId: Int, @Body body: SendChatMessage): OrderChatMessage

    // Foto (JPG) con comentario opcional. Se llama con ApiClient.chatApi (subir tarda más)
    @Multipart
    @POST("order-chat/{orderId}/attachments")
    suspend fun sendOrderChatPhoto(
        @Path("orderId") orderId: Int,
        @Part file: MultipartBody.Part,
        @Part("content") content: RequestBody?
    ): OrderChatMessage

    // "Continuar con Google": crea la cuenta o inicia sesión con el ID token de Google
    @POST("auth/google")
    suspend fun googleLogin(@Body body: GoogleLoginRequest): LoginResponse

    // Cuenta creada con Google: guarda apellido, teléfono y la primera dirección
    @PUT("clients/me/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileRequest): LoginResponse

    // Comprueba que el token y la cuenta siguen siendo válidos.
    @GET("auth/me")
    suspend fun checkSession(): ResponseBody

    // Token nuevo para la misma sesión (antes de que venza el actual)
    @POST("auth/refresh")
    suspend fun refreshToken(): LoginResponse
    // ---------- Seguimiento del cliente ----------
    @GET("delivery/client/{clientId}/active")
    suspend fun clientActive(@Path("clientId") clientId: Int): List<ClientDelivery>

    @GET("delivery/client/{clientId}/orders/{orderId}")
    suspend fun clientDelivery(
        @Path("clientId") clientId: Int,
        @Path("orderId") orderId: Int
    ): ClientDelivery

    // ---------- Repartidor ----------
    @GET("delivery/person/{id}/active")
    suspend fun courierActive(@Path("id") id: Int): List<DeliveryJob>

    @PUT("delivery/person/{id}/orders/{orderId}/location")
    suspend fun sendLocation(
        @Path("id") id: Int,
        @Path("orderId") orderId: Int,
        @Body body: LocationRequest
    ): DeliveryJob

    @POST("delivery/person/{id}/orders/{orderId}/complete")
    suspend fun complete(
        @Path("id") id: Int,
        @Path("orderId") orderId: Int,
        @Body body: CompleteRequest
    ): ResponseBody


    @POST("delivery/person/{id}/orders/{orderId}/start")
    suspend fun startDelivery(
        @Path("id") id: Int,
        @Path("orderId") orderId: Int,
        @Body body: StartDeliveryRequest
    ): DeliveryJob

    // ---------- Menú (público, no necesita sesión) ----------
    @GET("products/active")
    suspend fun products(): List<Product>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Int): ProductDetail

    @GET("products/{id}/related")
    suspend fun relatedProducts(@Path("id") id: Int): List<Product>

    @GET("categories")
    suspend fun categories(): List<Category>

    @GET("addons/active/category/{categoryId}")
    suspend fun addOns(@Path("categoryId") categoryId: Int): List<AddOnOption>

    // ---------- Carrito compartido con la web (vive en Postgres) ----------
    @GET("cart/me")
    suspend fun cart(): List<CartItem>

    @POST("cart/me/items")
    suspend fun cartAdd(@Body body: CheckoutItemRequest): List<CartItem>

    @PUT("cart/me/items/{lineId}")
    suspend fun cartQuantity(
        @Path("lineId") lineId: String,
        @Body body: QuantityRequest
    ): List<CartItem>

    @DELETE("cart/me/items/{lineId}")
    suspend fun cartRemove(@Path("lineId") lineId: String): List<CartItem>

    @DELETE("cart/me")
    suspend fun cartClear(): List<CartItem>

    // ---------- Direcciones, pedido y mis pedidos (los mismos endpoints de la web) ----------
    @GET("clients/me/addresses")
    suspend fun addresses(): List<Address>

    @POST("clients/me/addresses")
    suspend fun addAddress(@Body body: AddressInput): Address

    @POST("checkout")
    suspend fun checkout(@Body body: CheckoutRequest): ResponseBody

    // ---------- Pago con tarjeta (Stripe Checkout, los mismos endpoints de la web) ----------
    // Crea el pedido (oculto hasta que se pague) y la página de pago de Stripe
    @POST("payments/start")
    suspend fun startPayment(@Body body: StartPaymentRequest): Payment

    // Estado real: el back también consulta a Stripe
    @GET("payments/{orderId}")
    suspend fun paymentStatus(@Path("orderId") orderId: Int): Payment

    // Sin pagar: cierra la página de pago. Pagado y sin confirmar: reembolsa.
    @POST("payments/{orderId}/cancel")
    suspend fun cancelPayment(@Path("orderId") orderId: Int): Payment

    // Enlace firmado a la factura de un pedido pagado
    @GET("invoices/{orderId}/link")
    suspend fun invoiceLink(@Path("orderId") orderId: Int): InvoiceLink

    // ---------- Direcciones <-> coordenadas (Google Geocoding en el back, igual que la web) ----------
    // Si no encuentra la dirección el back responde 200 SIN cuerpo; Gson no
    // acepta un cuerpo vacío, así que se lee como texto (ver MapService.geocode)
    @GET("maps/search")
    suspend fun mapSearch(@Query("q") query: String): ResponseBody

    @GET("maps/reverse")
    suspend fun mapReverse(@Query("lat") lat: Double, @Query("lon") lon: Double): MapAddress

    // ---------- Pedidos de mesa (mesero) ----------
    // Bandeja del mesero: avisos de la cocina sobre sus pedidos
    @GET("table-orders/notifications")
    suspend fun waiterNotifications(): List<AppNotification>

    @PUT("table-orders/notifications/{id}/read")
    suspend fun waiterNotificationRead(@Path("id") id: Int): ResponseBody

    @PUT("table-orders/notifications/read-all")
    suspend fun waiterNotificationsReadAll(): ResponseBody

    @GET("table-orders/tables")
    suspend fun waiterTables(@Query("floor") floor: Int): List<WaiterTable>

    @GET("table-orders/tables/{tableId}")
    suspend fun tableOrders(@Path("tableId") tableId: Int): List<TableOrder>

    @POST("table-orders")
    suspend fun createTableOrder(@Body body: NewTableOrder): TableOrder

    // Solo cuando todos los platos están listos: el pedido queda entregado
    @POST("table-orders/{id}/deliver")
    suspend fun deliverTableOrder(@Path("id") id: Int): TableOrder

    // Cobra un pedido ya entregado; si a la mesa no le queda nada, queda libre
    @POST("table-orders/{id}/pay")
    suspend fun payTableOrder(@Path("id") id: Int, @Body body: PayRequest): TableOrder

    // "Cobrar mesa": todos los pedidos entregados y sin cobrar de la mesa a la vez
    @POST("table-orders/tables/{tableId}/pay")
    suspend fun payTable(@Path("tableId") tableId: Int, @Body body: PayRequest): List<TableOrder>

    // Solo mientras la cocina no haya empezado ningún plato
    @POST("table-orders/{id}/cancel")
    suspend fun cancelTableOrder(@Path("id") id: Int): TableOrder

    @GET("orders/me")
    suspend fun myOrders(): List<Order>

    // El back comprueba que el pedido sea del cliente con sesión
    @PUT("orders/{orderId}/cancel")
    suspend fun cancelOrder(@Path("orderId") orderId: Int): ResponseBody

    // ---------- Perfil del cliente (los mismos endpoints de la web) ----------
    @GET("clients/me")
    suspend fun getClient(): ClientProfile

    // Devuelve un token nuevo, por si el cliente cambió su correo
    @PUT("clients/me")
    suspend fun updateClient(@Body body: ClientUpdate): LoginResponse

    @DELETE("clients/me")
    suspend fun deleteClient(): ResponseBody

    @PUT("clients/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ResponseBody

    // ---------- Notificaciones (las guarda el back) ----------
    @GET("notifications/me?platform=mobile")
    suspend fun notifications(): List<AppNotification>

    @PUT("notifications/{id}/read")
    suspend fun notificationRead(@Path("id") id: Int): ResponseBody

    @PUT("notifications/me/read-all")
    suspend fun notificationsReadAll(): ResponseBody

    // ---------- Cupones (los mismos endpoints de la web) ----------
    @GET("coupons/me")
    suspend fun availableCoupons(): List<Coupon>

    @GET("coupons/me/all")
    suspend fun myCoupons(): List<ClientCoupon>

    @GET("coupons/validate")
    suspend fun validateCoupon(
        @Query("code") code: String,
        @Query("subtotal") subtotal: Double
    ): CouponDiscount

}