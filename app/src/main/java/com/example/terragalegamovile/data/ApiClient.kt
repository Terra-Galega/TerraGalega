package com.example.terragalegamovile.data

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    // Emulador de Android: 10.0.2.2 es tu PC (donde corre el backend).
    // Celular real: pon la IP de tu PC en el wifi, ej. "http://192.168.1.20:8080/"
    const val BASE_URL = "http://10.195.23.83:8080/"

    private val okHttp = OkHttpClient.Builder()
        //Agrega el JWT a cada peticion si hay sesion
        .addInterceptor{ chain ->
            val token = Session.user?.token
            val request = if (token != null)
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            else chain.request()
            val response = chain.proceed(request)
            // Token vencido o cuenta eliminada/desactivada: se cierra la sesión
            if (response.code == 401 && token != null) Session.user = null
            response
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

// Convierte un error de red o del backend en un texto para mostrar en pantalla.
// Si el backend mandó un mensaje (ej. "PIN incorrecto. Te quedan 3 intentos."), se usa ese.
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> try {
        Gson().fromJson(response()?.errorBody()?.string(), ApiErrorBody::class.java)?.message
            ?: "Error ${code()}"
    } catch (e: Exception) {
        "Error ${code()}"
    }
    is IOException -> "No se pudo conectar: ${this.message}"
    else -> message ?: "Error inesperado."
}