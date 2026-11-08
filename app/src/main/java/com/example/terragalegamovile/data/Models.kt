package com.example.terragalegamovile.data

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val id: Int,
    val name: String,
    val lastName: String,
    val address: String? = null,
    val role: String
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
    val locationUpdatedAt: String?
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
    val attemptsLeft: Int
)

data class LocationRequest(val latitude: Double, val longitude: Double)

data class CompleteRequest(val pin: String)

// Forma en que el backend devuelve los errores
data class ApiErrorBody(val message: String?)