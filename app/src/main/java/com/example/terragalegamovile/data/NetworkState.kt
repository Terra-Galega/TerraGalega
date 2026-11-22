package com.example.terragalegamovile.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ¿Hay internet? La barra de arriba muestra "Sin conexión" mientras no haya.
object NetworkState {
    var online by mutableStateOf(true)
        private set

    private var registered = false

    fun start(context: Context) {
        if (registered) return
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        online = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        try {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { online = true }
                override fun onLost(network: Network) { online = false }
            })
            registered = true
        } catch (_: Exception) {
            // Si no se puede escuchar, no se muestra el aviso
            online = true
        }
    }
}
