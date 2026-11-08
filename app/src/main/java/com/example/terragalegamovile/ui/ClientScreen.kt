package com.example.terragalegamovile.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.ClientDelivery
import com.example.terragalegamovile.data.LatLon
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.PinDigits
import com.example.terragalegamovile.ui.components.StatusPill
import com.example.terragalegamovile.ui.components.TerraTopBar
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.delay
import com.example.terragalegamovile.data.Restaurant
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraScaffold
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration

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
            delay(3000)
        }
    }

    // Ubica el restaurante con el mismo geocodificador que los destinos
    // La dirección escrita se convierte en un punto del mapa (una sola vez)
    LaunchedEffect(delivery?.address) {
        val address = delivery?.address
        if (address != null) destination = MapService.destinationFor(address)
    }

    val serverCourier = delivery?.let { d ->
        d.latitude?.let { lat -> d.longitude?.let { lon -> LatLon(lat, lon) } }
    }
    // Mientras el repartidor no reporte posición, se asume que sigue en el restaurante
    val courier = serverCourier ?: Restaurant.location

    // Ruta fija: del restaurante al destino
    LaunchedEffect(destination) {
        val to = destination
        val address = delivery?.address
        if (to != null && address != null) route = MapService.routeFor(address, to)
    }
    // Si no se pudo calcular la ruta, se dibuja una línea recta (mejor que nada)
    val shownRoute = if (route.size > 1) route
    else (destination?.let { listOf(Restaurant.location, it) } ?: emptyList())

    TerraScaffold(
        role = "Cliente",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
        ) {
            val current = delivery

            if (current == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        !loaded -> Text("Cargando...", color = TextSoft)

                        delivered -> {
                            StatusPill("Entregado ✓", Forest)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tu pedido llegó. ¡Buen provecho!",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }

                        else -> {
                            Text(
                                "No tienes pedidos en camino",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cuando un repartidor salga con tu pedido, lo verás aquí en el mapa.",
                                color = TextSoft,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Burgundy)
                    }
                }
            } else {
                val mapHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
                TrackingMap(
                    courier = courier,
                    destination = destination,
                    route = route,
                    restaurant = Restaurant.location,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    contentPadding = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pedido #${current.orderId}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        StatusPill("En camino", Terra)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${current.deliveryPersonName ?: "-"} · ${current.deliveryPersonPhone ?: "-"}",
                        color = TextSoft
                    )
                    Text(
                        if (courier == null) "Esperando la ubicación del repartidor..."
                        else "Última actualización: ${
                            current.locationUpdatedAt?.substringAfter('T')?.take(8) ?: "-"
                        }",
                        fontSize = 12.sp,
                        color = TextLabel
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "TU PIN DE ENTREGA",
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = TextLabel
                    )
                    Spacer(Modifier.height(8.dp))
                    PinDigits(current.pin ?: "")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Díselo al repartidor solo cuando tengas tu pedido en la mano.",
                        fontSize = 12.sp,
                        color = TextSoft
                    )
                }

                // Quejas, reclamos o comentarios: abre el correo con el asunto ya escrito
                val context = LocalContext.current
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    contentPadding = 20.dp
                ) {
                    Text("¿Algún problema con tu pedido?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Déjanoslo saber.", color = TextSoft)
                    Spacer(Modifier.height(14.dp))
                    TerraButton(
                        text = "Escribenos",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("contacto@terragalega.co"))
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Quejas, Reclamos o Comentarios del pedido #${current.orderId}"
                                )
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "No hay una app de correo instalada",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}