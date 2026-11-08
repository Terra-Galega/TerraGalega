package com.example.terragalegamovile.ui

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.data.LatLon
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

// Zoom de calle (más número = más cerca)
private const val ZOOM_DESTINATION = 17.0
private const val ZOOM_FOLLOW = 17.5

// Mapa con el repartidor, el destino y la ruta entre los dos.
// Primero muestra el destino ~3 s y luego sigue al repartidor todo el tiempo.

@Composable
fun TrackingMap(
    courier: LatLon?,
    destination: LatLon?,
    route: List<LatLon>,
    modifier: Modifier = Modifier,
    restaurant: LatLon? = null
) {
    val context = LocalContext.current

    var introDone by remember { mutableStateOf(false) }
    var followStarted by remember { mutableStateOf(false) }

    val mapView = remember {
        val config = Configuration.getInstance()
        config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        config.userAgentValue = "TerraGalegaMovil/1.0 (proyecto universitario Javeriana)"
        config.osmdroidBasePath = File(context.filesDir, "osmdroid")
        config.osmdroidTileCache = File(context.cacheDir, "osm-tiles")

        MapView(context).apply {
            setTileSource(EsriStreetMap)
            setMultiTouchControls(true)
            controller.setZoom(ZOOM_DESTINATION)
            controller.setCenter(GeoPoint(4.65, -74.08)) // Bogotá, hasta tener posiciones

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

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // 1) Cuando se conoce el destino: lo enfoca y espera 3 segundos
    LaunchedEffect(destination) {
        if (destination != null && !introDone) {
            mapView.controller.animateTo(
                GeoPoint(destination.lat, destination.lon),
                ZOOM_DESTINATION,
                800L
            )
            delay(3000)
            introDone = true
        }
    }

    // Si no se logra ubicar el destino, no se queda esperando
    LaunchedEffect(Unit) {
        delay(8000)
        introDone = true
    }

    // 2) Después sigue al repartidor cada vez que se mueve
    LaunchedEffect(courier, introDone) {
        if (introDone && courier != null) {
            val point = GeoPoint(courier.lat, courier.lon)
            if (!followStarted) {
                // La primera vez acerca el zoom; después solo lo sigue sin tocar tu zoom
                mapView.controller.animateTo(point, ZOOM_FOLLOW, 800L)
                followStarted = true
            } else {
                mapView.controller.animateTo(point)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            map.overlays.add(CopyrightOverlay(context))

            if (route.size > 1) {
                val line = Polyline().apply {
                    setPoints(route.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = android.graphics.Color.parseColor("#B2571F") // terracota
                    outlinePaint.strokeWidth = 10f
                }
                map.overlays.add(line)
            }

            restaurant?.let {
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(it.lat, it.lon)
                    title = "Restaurante Terra Galega"
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }

            destination?.let {
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(it.lat, it.lon)
                    title = "Destino"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            courier?.let {
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(it.lat, it.lon)
                    title = "Repartidor"
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }

            map.invalidate()
        }
    )
}

// Mapa de calles de Esri (no pide clave). Esri pide las imágenes en orden zoom/y/x.
private val EsriStreetMap = object : OnlineTileSourceBase(
    "EsriWorldStreet", 0, 19, 256, ".png",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    "Tiles © Esri, © OpenStreetMap contributors"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
    }
}