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

    // Backend desplegado en Railway.
    const val BASE_URL = "https://backend-production-211649e7.up.railway.app/"
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

    // El asistente puede tardar varios segundos en responder: espera hasta 60 s
    val chatApi: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp.newBuilder().readTimeout(60, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

// Código de error del backend
// Lee el cuerpo, así que se usa en lugar de toUserMessage(), no además.
fun Throwable.apiError(): ApiErrorBody? = (this as? HttpException)?.let {
    try {
        Gson().fromJson(it.response()?.errorBody()?.string(), ApiErrorBody::class.java)
    } catch (_: Exception) {
        null
    }
}

// Error con un texto ya pensado para el usuario (por ejemplo, al iniciar con Google)
class UserMessageException(message: String) : Exception(message)

// Convierte un error de red o del backend en un texto para mostrar en pantalla,
// igual que getErrorMessage de la web: nunca muestra textos técnicos ("failed to connect…").
// Si el backend mandó un mensaje (ej. "PIN incorrecto. Te quedan 3 intentos."), se usa ese.
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val fromServer = try {
            Gson().fromJson(response()?.errorBody()?.string(), ApiErrorBody::class.java)?.message
        } catch (_: Exception) {
            null
        }
        fromServer?.takeIf { it.isNotBlank() } ?: httpMessage(code())
    }
    is java.net.SocketTimeoutException ->
        t("El servidor tardó demasiado en responder. Inténtalo de nuevo.")
    is IOException ->
        t("No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.")
    is UserMessageException -> message ?: GENERIC_ERROR
    else -> GENERIC_ERROR
}

private val GENERIC_ERROR: String get() = t("Ocurrió un error inesperado. Inténtalo de nuevo.")

private fun httpMessage(code: Int): String = when {
    code == 401 -> t("Tu sesión expiró. Vuelve a iniciar sesión.")
    code == 403 -> t("No tienes permiso para hacer esto.")
    code == 404 -> t("No encontramos lo que buscabas.")
    code == 409 -> t("No se pudo completar porque algo cambió. Actualiza e inténtalo de nuevo.")
    code >= 500 -> t("El servidor tuvo un problema. Inténtalo de nuevo en un momento.")
    else -> GENERIC_ERROR
}