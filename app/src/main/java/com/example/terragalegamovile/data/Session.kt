package com.example.terragalegamovile.data

object Session {
    // Cada vez que cambia, se guarda en el teléfono (null = sesión cerrada)
    var user: LoginResponse? = null
        set(value) {
            field = value
            AppStorage.saveSession(value)
        }

    // Al abrir la app: recupera la sesión guardada
    fun restore() {
        user = AppStorage.loadSession()
    }
}