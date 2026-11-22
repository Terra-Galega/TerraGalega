package com.example.terragalegamovile.data

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            (it.role == "CLIENT" || it.role == "DELIVERY_PERSON" || it.role == "WAITER") &&
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

    // Si al token le queda menos de esto, se pide uno nuevo (como la web)
    private const val RENEW_BEFORE_SECONDS = 60 * 60L
    private val renewing = Mutex()

    // Segundos que le quedan al token actual (0 si no hay o ya venció)
    fun secondsLeft(): Long {
        val token = _state.value?.token ?: return 0
        return (expiresAt(token) - System.currentTimeMillis() / 1000L).coerceAtLeast(0)
    }

    // Pide un token nuevo cuando al actual le queda poco, sin cerrar la sesión.
    // Así un mesero con la app abierta todo el turno no pierde la sesión (ni el
    // pedido que estaba armando) a las 24 h. Un fallo de red no cierra la sesión.
    suspend fun renewIfNeeded() {
        renewing.withLock {
            val current = _state.value ?: return
            val left = secondsLeft()
            if (left <= 0 || left > RENEW_BEFORE_SECONDS) return
            try {
                val fresh = ApiClient.api.refreshToken()
                synchronized(lock) {
                    // Solo si nadie cerró o cambió la sesión mientras tanto
                    if (_state.value?.token == current.token) {
                        user = current.copy(token = fresh.token)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Se reintenta en la siguiente revisión
            }
        }
    }

    // Esta lectura solo sirve para detectar el vencimiento.
    // La firma y los permisos siempre los comprueba el backend.
    private fun isTokenCurrent(token: String?): Boolean =
        !token.isNullOrBlank() && expiresAt(token) > System.currentTimeMillis() / 1000L

    // Fecha de vencimiento del token (segundos epoch); 0 si no se puede leer
    private fun expiresAt(token: String): Long {
        return try {
            val parts = token.split('.')

            if (parts.size != 3) {
                0L
            } else {
                val decoded = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )

                val payload = JSONObject(
                    String(decoded, Charsets.UTF_8)
                )

                payload.optLong("exp", 0L)
            }
        } catch (_: Exception) {
            0L
        }
    }
}