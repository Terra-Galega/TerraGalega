package com.example.terragalegamovile.ui

import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.terragalegamovile.data.CheckoutDraft
import com.example.terragalegamovile.data.LatLon
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.Restaurant
import com.example.terragalegamovile.ui.components.AddressIcon
import com.example.terragalegamovile.ui.theme.BorderSoft
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.CancellationException
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val PICKER_STYLE = "https://tiles.openfreemap.org/styles/liberty"

// Mapa con un pin fijo en el centro: mueves el mapa y la dirección se actualiza sola.
@Composable
fun AddressMapPicker(
    address: String,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var picked by remember { mutableStateOf<LatLon?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val currentOnPicked by rememberUpdatedState(onPicked)

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { m ->
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                m.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(Restaurant.location.lat, Restaurant.location.lon), 14.0
                    )
                )
                m.setStyle(PICKER_STYLE)

                // Solo cuenta cuando el usuario mueve el mapa con el dedo
                var byUser = false
                m.addOnCameraMoveStartedListener { reason ->
                    byUser = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
                }
                m.addOnCameraIdleListener {
                    if (byUser) {
                        byUser = false
                        m.cameraPosition.target?.let { picked = LatLon(it.latitude, it.longitude) }
                    }
                }
                map = m
            }

            // Mientras el dedo está sobre el mapa, la pantalla no puede quitarle el gesto
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    // Si ya había una dirección, el mapa abre centrado en ella
    LaunchedEffect(map) {
        val m = map ?: return@LaunchedEffect
        if (address.isNotBlank()) {
            MapService.geocode(address)?.let {
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lon), 16.0))
            }
        }
    }

    // Cada vez que sueltas el mapa, convierte el punto en una dirección
    LaunchedEffect(picked) {
        val p = picked ?: return@LaunchedEffect
        resolving = true
        failed = false
        val text = try {
            MapService.reverse(p)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        resolving = false
        if (text.isNullOrBlank()) failed = true
        else currentOnPicked(text.take(CheckoutDraft.ADDRESS_MAX))
    }

    // El MapView necesita seguir el ciclo de vida de la pantalla
    val lifecycle = (context as ComponentActivity).lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Column(modifier = modifier) {
        val shape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(shape)
                .border(1.dp, BorderSoft, shape)
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // Pin fijo: la punta queda justo en el centro del mapa
            AddressIcon(
                plus = false,
                color = Terra,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-18).dp)
                    .size(44.dp)
            )

            if (address.isBlank() && picked == null) {
                Text(
                    "Mueve el mapa hasta la puerta de tu casa",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Charcoal.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        when {
            resolving -> Text("Buscando la dirección...", fontSize = 13.sp, color = TextSoft)
            failed -> Text(
                "No pudimos leer esta dirección. Mueve un poco el mapa o escríbela.",
                fontSize = 13.sp,
                color = Burgundy
            )
            address.isNotBlank() -> Row(verticalAlignment = Alignment.CenterVertically) {
                AddressIcon(plus = false, color = Terra, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(address, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Charcoal)
            }
        }
    }
}