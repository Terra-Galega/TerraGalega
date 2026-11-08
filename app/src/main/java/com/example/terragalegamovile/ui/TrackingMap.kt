package com.example.terragalegamovile.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.data.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Mapa con el repartidor, el destino y la ruta entre los dos
@Composable
fun TrackingMap(
    courier: LatLon?,
    destination: LatLon?,
    route: List<LatLon>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        // Obligatorio: OpenStreetMap bloquea las apps que no se identifican
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(4.65, -74.08)) // Bogotá, hasta tener posiciones
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Centra el mapa cuando aparece la primera posición
    LaunchedEffect(courier != null, destination != null) {
        val point = courier ?: destination
        if (point != null) {
            mapView.controller.setCenter(GeoPoint(point.lat, point.lon))
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            if (route.size > 1) {
                val line = Polyline().apply {
                    setPoints(route.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.strokeWidth = 8f
                }
                map.overlays.add(line)
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