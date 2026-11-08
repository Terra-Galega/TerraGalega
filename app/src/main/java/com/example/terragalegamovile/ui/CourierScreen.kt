package com.example.terragalegamovile.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.CompleteRequest
import com.example.terragalegamovile.data.DeliveryJob
import com.example.terragalegamovile.data.LocationRequest
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.toUserMessage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationRequest as GmsLocationRequest
import com.example.terragalegamovile.data.LatLon
import com.example.terragalegamovile.data.MapService

@SuppressLint("MissingPermission")
@Composable
fun CourierScreen(user: LoginResponse, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pedido que lleva el repartidor
    var job by remember { mutableStateOf<DeliveryJob?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    // PIN
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    // Ubicación
    var myPos by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    // Destino y ruta para el mapa
    var destination by remember { mutableStateOf<LatLon?>(null) }
    var route by remember { mutableStateOf<List<LatLon>>(emptyList()) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasPermission = hasLocationPermission() }

    // Carga el pedido en camino de este repartidor
    fun loadJob() {
        scope.launch {
            loading = true
            loadError = null
            try {
                job = ApiClient.api.courierActive(user.id).firstOrNull()
            } catch (e: Exception) {
                loadError = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    // Valida el PIN con el backend y finaliza el pedido
    fun finishOrder() {
        val current = job ?: return
        scope.launch {
            sending = true
            pinError = null
            try {
                ApiClient.api.complete(user.id, current.orderId, CompleteRequest(pin)).close()
                message = "Pedido #${current.orderId} entregado ✓"
                pin = ""
                job = null
            } catch (e: Exception) {
                pinError = e.toUserMessage()
                // Actualiza los intentos que quedan
                try {
                    job = ApiClient.api.courierActive(user.id).firstOrNull()
                } catch (_: Exception) {
                }
            } finally {
                sending = false
            }
        }
    }

    // Al abrir la pantalla: carga el pedido y pide el permiso de ubicación
    LaunchedEffect(Unit) {
        loadJob()
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Mientras haya pedido y permiso: manda la ubicación al backend cada 5 segundos
    DisposableEffect(job?.orderId, hasPermission) {
        val current = job
        if (current == null || !hasPermission) {
            return@DisposableEffect onDispose { }
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = GmsLocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                myPos = loc.latitude to loc.longitude
                scope.launch {
                    try {
                        ApiClient.api.sendLocation(
                            user.id,
                            current.orderId,
                            LocationRequest(loc.latitude, loc.longitude)
                        )
                        locationError = null
                    } catch (e: Exception) {
                        locationError = e.toUserMessage()
                    }
                }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        onDispose { client.removeLocationUpdates(callback) }
    }

    val courierPos = myPos?.let { LatLon(it.first, it.second) }

    // La dirección del pedido se convierte en un punto del mapa
    LaunchedEffect(job?.address) {
        val address = job?.address
        if (address != null) destination = MapService.geocode(address)
    }

    // Recalcula la ruta cuando te mueves ~100 m
    val courierKey = courierPos?.let { "%.3f,%.3f".format(it.lat, it.lon) }
    LaunchedEffect(courierKey, destination) {
        val from = courierPos
        val to = destination
        if (from != null && to != null) route = MapService.route(from, to)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Repartidor", style = MaterialTheme.typography.titleMedium)
                Text("${user.name} ${user.lastName}", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = onLogout) { Text("Salir") }
        }

        Spacer(Modifier.height(16.dp))

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> Text("Cargando pedido...")

            loadError != null -> {
                Text(loadError!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { loadJob() }) { Text("Reintentar") }
            }

            job == null -> {
                Text("No tienes pedidos en camino.")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { loadJob() }) { Text("Actualizar") }
            }

            else -> {
                val current = job!!

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pedido #${current.orderId}", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Dirección: ${current.address ?: "-"}")
                        Text("Cliente: ${current.clientName ?: "-"}")
                        Text("Teléfono: ${current.clientPhone ?: "-"}")
                    }
                }

                Spacer(Modifier.height(16.dp))

                TrackingMap(
                    courier = courierPos,
                    destination = destination,
                    route = route,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
                Spacer(Modifier.height(16.dp))

                // Estado del GPS
                if (!hasPermission) {
                    Text(
                        "Falta el permiso de ubicación: el cliente no puede ver dónde vas.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) { Text("Dar permiso de ubicación") }
                } else {
                    val pos = myPos
                    Text(
                        if (pos == null) "Buscando tu ubicación..."
                        else "Enviando ubicación: %.5f, %.5f".format(pos.first, pos.second)
                    )
                }
                locationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(24.dp))

                // PIN que le dice el cliente
                Text("Finalizar entrega", style = MaterialTheme.typography.titleMedium)
                Text("Pídele al cliente su PIN de 4 dígitos.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN del cliente") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Intentos restantes: ${current.attemptsLeft}",
                    style = MaterialTheme.typography.bodySmall
                )
                pinError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = pin.length == 4 && !sending,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { finishOrder() }
                ) {
                    Text(if (sending) "Validando..." else "Entregar pedido")
                }
            }
        }
    }
}