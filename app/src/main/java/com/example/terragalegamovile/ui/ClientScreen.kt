package com.example.terragalegamovile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.ClientDelivery
import com.example.terragalegamovile.data.LatLon
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.toUserMessage
import kotlinx.coroutines.delay

@Composable
fun ClientScreen(user: LoginResponse, onLogout: () -> Unit) {
    var delivery by remember { mutableStateOf<ClientDelivery?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var delivered by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var destination by remember { mutableStateOf<LatLon?>(null) }
    var route by remember { mutableStateOf<List<LatLon>>(emptyList()) }

    // Consulta al backend cada 5 segundos: PIN y ubicación del repartidor
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val first = ApiClient.api.clientActive(user.id).firstOrNull()
                if (first == null && delivery != null) delivered = true
                if (first != null) delivered = false
                delivery = first
                error = null
            } catch (e: Exception) {
                error = e.toUserMessage()
            }
            loaded = true
            delay(2000)
        }
    }

    // La dirección escrita se convierte en un punto del mapa (una sola vez)
    LaunchedEffect(delivery?.address) {
        val address = delivery?.address
        if (address != null) destination = MapService.geocode(address)
    }

    val courier = delivery?.let { d ->
        d.latitude?.let { lat -> d.longitude?.let { lon -> LatLon(lat, lon) } }
    }

    // Recalcula la ruta solo cuando el repartidor se mueve ~100 m
    val courierKey = courier?.let { "%.3f,%.3f".format(it.lat, it.lon) }
    LaunchedEffect(courierKey, destination) {
        val from = courier
        val to = destination
        if (from != null && to != null) route = MapService.route(from, to)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cliente", style = MaterialTheme.typography.titleMedium)
                Text("${user.name} ${user.lastName}", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = onLogout) { Text("Salir") }
        }

        val current = delivery

        if (current == null) {
            Column(Modifier.padding(20.dp)) {
                when {
                    !loaded -> Text("Cargando...")
                    delivered -> Text(
                        "Tu pedido fue entregado ✓",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> Text("No tienes pedidos en camino.")
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            TrackingMap(
                courier = courier,
                destination = destination,
                route = route,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pedido #${current.orderId} en camino", style = MaterialTheme.typography.titleMedium)
                    Text("Repartidor: ${current.deliveryPersonName ?: "-"}")
                    Text("Teléfono: ${current.deliveryPersonPhone ?: "-"}")
                    Text(
                        if (courier == null) "Esperando la ubicación del repartidor..."
                        else "Última actualización: ${current.locationUpdatedAt?.substringAfter('T')?.take(8) ?: "-"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Tu PIN de entrega", style = MaterialTheme.typography.labelLarge)
                    Text(
                        current.pin ?: "----",
                        style = MaterialTheme.typography.displayMedium,
                        letterSpacing = 8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Díselo al repartidor solo cuando tengas tu pedido en la mano.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}