package com.example.terragalegamovile.ui

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.foundation.layout.imePadding

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.CompleteRequest
import com.example.terragalegamovile.data.DeliveryJob
import com.example.terragalegamovile.data.LatLon
import com.example.terragalegamovile.data.LocationRequest
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.Restaurant
import com.example.terragalegamovile.data.RoutePath
import com.example.terragalegamovile.data.Simulation
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.InfoLine
import com.example.terragalegamovile.ui.components.StatusPill
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Gold
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationRequest as GmsLocationRequest
import android.content.Intent
import android.net.Uri
import com.example.terragalegamovile.ui.components.TerraScaffold
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import com.example.terragalegamovile.data.AppStorage
import com.example.terragalegamovile.ui.components.PinInput
import com.example.terragalegamovile.data.StartDeliveryRequest
@SuppressLint("MissingPermission")
@Composable
fun CourierScreen(user: LoginResponse, onLogout: () -> Unit) {
    val haptic = LocalHapticFeedback.current
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

    // Recorrido
    var started by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var arrived by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var myPos by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    // Destino y ruta (restaurante -> destino)
    var destination by remember { mutableStateOf<LatLon?>(null) }
    var geocodeDone by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var route by remember { mutableStateOf<List<LatLon>>(emptyList()) }
    var routeReady by remember { mutableStateOf(false) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasPermission = hasLocationPermission() }

    // Carga el pedido en camino y recupera el progreso del recorrido
    fun loadJob() {
        scope.launch {
            loading = true
            loadError = null
            try {
                val fetched = ApiClient.api.courierActive(user.id).firstOrNull()
                if (fetched != null) {
                    // El servidor decide si la entrega ya empezó.
                    started = fetched.startedAt != null

                    if (!started) {
                        AppStorage.clearTrip(fetched.orderId)
                    } else {
                        AppStorage.setTripStarted(fetched.orderId, true)
                    }
                } else {
                    started = false
                }

                job = fetched
            } catch (e: Exception) {
                loadError = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    // Botón "Iniciar entrega"
    fun startTrip() {
        val current = job ?: return
        val to = destination ?: return

        if (starting || started || !routeReady) return

        if (Simulation.USE_REAL_GPS && !hasPermission) {
            locationError = t("Autoriza la ubicación y vuelve a pulsar Iniciar entrega.")

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        val sharedRoute = if (route.size > 1) {
            route
        } else {
            listOf(Restaurant.location, to)
        }

        starting = true
        locationError = null

        scope.launch {
            try {
                val updated = ApiClient.api.startDelivery(
                    user.id,
                    current.orderId,
                    StartDeliveryRequest(sharedRoute)
                )

                // Conserva la ruta aceptada por el servidor.
                updated.route?.takeIf { it.size > 1 }?.let {
                    route = it
                }

                job = updated
                AppStorage.setTripStarted(current.orderId, true)

                // Los efectos de simulación/GPS arrancan después
                // de que el servidor confirme el inicio.
                started = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                locationError = e.toUserMessage()
            } finally {
                starting = false
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
                AppStorage.clearTrip(current.orderId)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                message = t("Pedido #{} entregado ✓", current.orderId)
                pin = ""
                job = null
                // Deja todo listo para el siguiente pedido
                started = false
                arrived = false
                progress = 0
                myPos = null
                destination = null
                geocodeDone = false
                route = emptyList()
                routeReady = false
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

    LaunchedEffect(Unit) { loadJob() }
    // Ubica el restaurante con el mismo geocodificador que los destinos
    // Pide el permiso de ubicación apenas el repartidor abre su pantalla
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(job?.orderId, job?.address, job?.route, retry) {
        val current = job ?: return@LaunchedEffect
        val savedRoute = current.route.orEmpty()

        destination = if (savedRoute.size > 1) {
            savedRoute.last()
        } else {
            current.address?.let { MapService.destinationFor(it) }
        }

        geocodeDone = true
    }

    LaunchedEffect(job?.orderId, destination, job?.route) {
        val to = destination
        val address = job?.address

        routeReady = false

        if (to != null && address != null) {
            val savedRoute = job?.route.orEmpty()

            route = if (savedRoute.size > 1) {
                savedRoute
            } else {
                MapService.routeFor(address, to)
            }

            routeReady = true
        }
    }
    // Si no se pudo calcular la ruta, se usa una línea recta
    val shownRoute = if (route.size > 1) route
    else (destination?.let { listOf(Restaurant.location, it) } ?: emptyList())

    // RECORRIDO SIMULADO: sale del restaurante y llega solo al destino.
    // Si la app se cierra, al volver sigue desde donde iba.
    LaunchedEffect(job?.orderId, started, routeReady) {
        if (Simulation.USE_REAL_GPS || !started || !routeReady) return@LaunchedEffect
        val current = job ?: return@LaunchedEffect
        val to = destination ?: return@LaunchedEffect

        val path = RoutePath(shownRoute.ifEmpty { listOf(Restaurant.location, to) })
        var traveled = AppStorage.getTraveled(current.orderId)

        while (true) {
            val pos = path.pointAt(traveled)
            myPos = pos.lat to pos.lon
            progress = if (path.totalMeters == 0.0) 100
            else (traveled / path.totalMeters * 100).toInt().coerceAtMost(100)
            AppStorage.saveTraveled(current.orderId, traveled)

            try {
                ApiClient.api.sendLocation(
                    user.id,
                    current.orderId,
                    LocationRequest(pos.lat, pos.lon)
                )
                locationError = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                locationError = e.toUserMessage()
            }

            if (traveled >= path.totalMeters) {
                arrived = true
                break
            }
            delay(Simulation.TICK_MILLIS)
            traveled += Simulation.SPEED_METERS_PER_SECOND * Simulation.TICK_MILLIS / 1000.0
        }
    }

    // GPS REAL: solo si Simulation.USE_REAL_GPS = true
    DisposableEffect(job?.orderId, started, hasPermission) {
        val current = job
        if (!Simulation.USE_REAL_GPS || current == null || !started || !hasPermission) {
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

    // Posición mostrada: la actual; si no hay, la última que conoce el backend; si no, el restaurante
    val serverPos =
        job?.let { j -> j.latitude?.let { la -> j.longitude?.let { lo -> LatLon(la, lo) } } }
    val courierPos = myPos?.let { LatLon(it.first, it.second) } ?: serverPos ?: Restaurant.location

    // El PIN solo se pide al llegar (en GPS real, una vez iniciada la entrega)
    val canFinish = if (Simulation.USE_REAL_GPS) started else arrived

    TerraScaffold(
        role = "Repartidor",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout
    ) { padding ->
        // Sin pedido no hay nada que desplazar: el aviso se centra en la pantalla
        val noJob = !loading && loadError == null && job == null
        val scroll = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (noJob) Modifier else Modifier.verticalScroll(scroll))
                .imePadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 16.dp,
                        bottom = 16.dp
                    )
            ) {
                message?.let {
                    StatusPill(it, Forest)
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    loading -> Text(t("Cargando pedido..."), color = TextSoft)

                    loadError != null -> {
                        ErrorNotice(loadError!!)
                        Spacer(Modifier.height(12.dp))
                        TerraButton(
                            t("Reintentar"),
                            onClick = { loadJob() },
                            style = ButtonStyle.Outline
                        )
                    }

                    job == null -> {
                        // Sin pedidos: tarjeta centrada en el espacio libre de la pantalla
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(CircleShape)
                                            .background(Terra.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(
                                                com.example.terragalegamovile.R.drawable.ic_stat_delivery
                                            ),
                                            contentDescription = null,
                                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Terra),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    Text(
                                        t("No tienes pedidos en camino"),
                                        fontFamily = com.example.terragalegamovile.ui.theme.Playfair,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.Center,
                                        color = com.example.terragalegamovile.ui.theme.Charcoal
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        t("Cuando el operador te asigne un pedido, aparecerá aquí."),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        color = TextSoft
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    ButtonStack {
                                        TerraButton(
                                            t("Actualizar"),
                                            onClick = { loadJob() },
                                            style = ButtonStyle.Outline,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        val current = job!!

                        // Datos del pedido
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    t("Pedido #{}", current.orderId),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                StatusPill(if (started) t("En camino") else t("Por iniciar"), Terra)
                            }
                            Spacer(Modifier.height(12.dp))
                            InfoLine(t("Sales de"), Restaurant.ADDRESS)
                            InfoLine(t("Dirección de entrega"), current.address ?: "-")
                            InfoLine(t("Cliente"), current.clientName ?: "-")
                            InfoLine(t("Teléfono"), current.clientPhone ?: "-")

                            // Llamar al cliente: abajo a la derecha de la tarjeta
                            current.clientPhone?.takeIf { it.isNotBlank() }?.let { phone ->
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    TerraButton(
                                        text = t("Llamar al cliente"),
                                        onClick = {
                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
                                                )
                                            } catch (_: Exception) {
                                                message = t("Este dispositivo no tiene app de teléfono")
                                            }
                                        },
                                        style = ButtonStyle.Sage,
                                        compact = true
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Mapa: restaurante, tu posición, destino y ruta
                        // Mapa: restaurante, tu posición, destino y ruta
                        val mapHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TrackingMap(
                                courier = courierPos,
                                destination = destination,
                                route = shownRoute,
                                restaurant = Restaurant.location,
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .height(mapHeight)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        if (!started) {
                            // Botón "Iniciar entrega"
                            ButtonStack {
                                TerraButton(
                                    text = when {
                                        starting -> t("Iniciando entrega...")
                                        !routeReady -> t("Preparando ruta...")
                                        else -> t("Iniciar entrega")
                                    },
                                    onClick = { startTrip() },
                                    enabled = routeReady && !starting,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (geocodeDone && destination == null) {
                                Spacer(Modifier.height(8.dp))
                                ErrorNotice(t("No se pudo ubicar la dirección en el mapa. Revisa tu conexión."))
                                Spacer(Modifier.height(8.dp))
                                TerraButton(
                                    t("Reintentar"),
                                    onClick = { geocodeDone = false; retry++ },
                                    style = ButtonStyle.Outline
                                )
                            }
                        } else {
                            // Estado del recorrido
                            val status = when {
                                Simulation.USE_REAL_GPS ->
                                    if (myPos == null) t("Buscando tu ubicación...") else t("Enviando tu ubicación en vivo")

                                arrived -> t("Llegaste al destino. Pídele su PIN al cliente.")
                                else -> t("En camino al destino: {} %", progress)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (arrived) Forest else Gold)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(status, fontSize = 14.sp, color = TextSoft)
                            }
                        }


                        locationError?.let {
                            Spacer(Modifier.height(4.dp))
                            ErrorNotice(it)
                        }

                        // PIN: solo aparece al llegar
                        if (canFinish) {
                            Spacer(Modifier.height(16.dp))
                            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                                Text(
                                    t("Finalizar entrega"),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(t("Pídele al cliente su PIN de 4 dígitos."), color = TextSoft)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    t("PIN DEL CLIENTE"),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = TextSoft
                                )
                                Spacer(Modifier.height(8.dp))
                                PinInput(value = pin, onValueChange = { pin = it })
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    t("Intentos restantes: {}", current.attemptsLeft),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = TextSoft
                                )
                                pinError?.let {
                                    Spacer(Modifier.height(8.dp))
                                    ErrorNotice(it)
                                }
                                Spacer(Modifier.height(16.dp))
                                ButtonStack {
                                    TerraButton(
                                        text = if (sending) t("Validando...") else t("Entregar pedido"),
                                        onClick = { finishOrder() },
                                        enabled = pin.length == 4 && !sending,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
