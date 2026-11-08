package com.example.terragalegamovile.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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
}