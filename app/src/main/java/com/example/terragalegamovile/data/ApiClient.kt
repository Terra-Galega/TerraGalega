package com.example.terragalegamovile.data

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl

object ApiClient {

    // Emulador de Android: 10.0.2.2 es tu PC (donde corre el backend).
    // Celular real: pon la IP de tu PC en el wifi, ej. "http://192.168.1.20:8080/"
    const val BASE_URL = "http://10.195.23.83:8080/"
    private val backendUrl = BASE_URL.toHttpUrl()
    private val okHttp = OkHttpClient.Builder()
        //Agrega el JWT a cada peticion si hay sesion
        .addInterceptor { chain ->
            val original = chain.request()
            val url = original.url

            val isBackend =
                url.scheme == backendUrl.scheme &&
                        url.host == backendUrl.host &&
                        url.port == backendUrl.port

            val isAuthRequest =
                url.encodedPath == "/auth/login" ||
                        url.encodedPath == "/auth/signup" ||
                        url.encodedPath == "/auth/google"

            val token = if (isBackend && !isAuthRequest) {
                Session.validToken()
            } else {
                null
            }

            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }

            val response = chain.proceed(request)

            if (response.code == 401 && token != null) {
                Session.clearIfTokenMatches(token)
            }

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

// Código de error del backend (ej. "EMAIL_NOT_VERIFIED"), o null si no trae.
// Lee el cuerpo, así que se usa en lugar de toUserMessage(), no además.
fun Throwable.apiError(): ApiErrorBody? = (this as? HttpException)?.let {
    try {
        Gson().fromJson(it.response()?.errorBody()?.string(), ApiErrorBody::class.java)
    } catch (_: Exception) {
        null
    }
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