package com.example.terragalegamovile.data

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object Session {

    private val lock = Any()

    private val _state = MutableStateFlow<LoginResponse?>(null)

    // La navegación observa este estado.
    val state: StateFlow<LoginResponse?> = _state.asStateFlow()

    // Conservamos Session.user para que el resto del código
    // siga leyendo y guardando la sesión como hasta ahora.
    var user: LoginResponse?
        get() = _state.value
        set(value) {
            synchronized(lock) {
                AppStorage.saveSession(value)
                _state.value = value
            }
        }

    // Recupera únicamente sesiones con token vigente
    // y con un rol admitido por la app móvil.
    fun restore() {
        val saved = AppStorage.loadSession()

        user = saved?.takeIf {
            (it.role == "CLIENT" || it.role == "DELIVERY_PERSON") &&
                    isTokenCurrent(it.token)
        }
    }

    // Si el token venció, elimina la sesión y notifica a la interfaz.
    fun validToken(): String? = synchronized(lock) {
        val current = _state.value ?: return@synchronized null

        if (isTokenCurrent(current.token)) {
            current.token
        } else {
            user = null
            null
        }
    }

    // Evita que una respuesta tardía con un token anterior
    // borre una sesión que ya tiene un token diferente.
    fun clearIfTokenMatches(token: String) {
        synchronized(lock) {
            if (_state.value?.token == token) {
                user = null
            }
        }
    }

    // Esta lectura solo sirve para detectar el vencimiento.
    // La firma y los permisos siempre los comprueba el backend.
    private fun isTokenCurrent(token: String?): Boolean {
        if (token.isNullOrBlank()) return false

        return try {
            val parts = token.split('.')

            if (parts.size != 3) {
                false
            } else {
                val decoded = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )

                val payload = JSONObject(
                    String(decoded, Charsets.UTF_8)
                )

                val expiresAt = payload.optLong("exp", 0L)
                val now = System.currentTimeMillis() / 1000L

                expiresAt > now
            }
        } catch (_: Exception) {
            false
        }
    }
}