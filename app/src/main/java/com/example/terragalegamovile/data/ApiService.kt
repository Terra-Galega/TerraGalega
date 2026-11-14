package com.example.terragalegamovile.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE

// El usuario con sesión se identifica por el token que el interceptor de ApiClient
// agrega en cada petición, así que las rutas del propio usuario no llevan su id.
interface ApiService {

    // ---------- Sesión ----------
    // La respuesta trae el token, que se guarda junto con la sesión
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // Registra un cliente y lo deja con la sesión iniciada (también devuelve el token)
    @POST("auth/signup")
    suspend fun register(@Body body: ClientCreate): LoginResponse

    // Comprueba que el token y la cuenta siguen siendo válidos.
    @GET("auth/me")
    suspend fun checkSession(): ResponseBody
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
}