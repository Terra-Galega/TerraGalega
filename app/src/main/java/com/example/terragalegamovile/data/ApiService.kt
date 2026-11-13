package com.example.terragalegamovile.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // ---------- Cliente ----------
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
    // ---------- Menú ----------
    @GET("products/active")
    suspend fun products(): List<Product>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Int): ProductDetail

    @GET("addons/active/category/{categoryId}")
    suspend fun addOns(@Path("categoryId") categoryId: Int): List<AddOnOption>

    // ---------- Carrito compartido con la web (vive en Postgres) ----------
    @GET("cart/{clientId}")
    suspend fun cart(@Path("clientId") clientId: Int): List<CartItem>

    @POST("cart/{clientId}/items")
    suspend fun cartAdd(
        @Path("clientId") clientId: Int,
        @Body body: CheckoutItemRequest
    ): List<CartItem>

    @PUT("cart/{clientId}/items/{lineId}")
    suspend fun cartQuantity(
        @Path("clientId") clientId: Int,
        @Path("lineId") lineId: String,
        @Body body: QuantityRequest
    ): List<CartItem>

    @DELETE("cart/{clientId}/items/{lineId}")
    suspend fun cartRemove(
        @Path("clientId") clientId: Int,
        @Path("lineId") lineId: String
    ): List<CartItem>

    @DELETE("cart/{clientId}")
    suspend fun cartClear(@Path("clientId") clientId: Int): List<CartItem>

    // ---------- Direcciones, pedido y mis pedidos (los mismos endpoints de la web) ----------
    @GET("clients/{clientId}/addresses")
    suspend fun addresses(@Path("clientId") clientId: Int): List<Address>

    @POST("clients/{clientId}/addresses")
    suspend fun addAddress(
        @Path("clientId") clientId: Int,
        @Body body: AddressInput
    ): Address

    @POST("checkout")
    suspend fun checkout(
        @Query("clientId") clientId: Int,
        @Body body: CheckoutRequest
    ): ResponseBody

    @GET("orders/client/{clientId}")
    suspend fun myOrders(@Path("clientId") clientId: Int): List<Order>

    @PUT("orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: Int,
        @Query("clientId") clientId: Int
    ): ResponseBody

    @GET("categories")
    suspend fun categories(): List<Category>

    @GET("products/{id}/related")
    suspend fun relatedProducts(@Path("id") id: Int): List<Product>


}