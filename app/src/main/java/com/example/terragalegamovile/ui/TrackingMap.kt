package com.example.terragalegamovile.ui

import android.view.MotionEvent
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.terragalegamovile.data.LatLon
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// Estilo gratis y sin clave de OpenFreeMap. Otros: "bright" (más sobrio), "positron" (claro y minimalista)
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

// Zoom de calle (en MapLibre es ~1 punto menos que en osmdroid)
private const val ZOOM_DESTINATION = 16.0
private const val ZOOM_FOLLOW = 16.5

private const val ROUTE_SRC = "route-src"
private const val ROUTE_LAYER = "route-layer"
private const val POINTS_SRC = "points-src"
private const val POINTS_LAYER = "points-layer"

private fun pointFeature(p: LatLon, color: String, radius: Float): Feature =
    Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply {
        addStringProperty("color", color)
        addNumberProperty("r", radius)
    }

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

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var introDone by remember { mutableStateOf(false) }
    var followStarted by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { m ->
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(4.65, -74.08), 11.0))
                m.setStyle(STYLE_URL) { style ->
                    style.addSource(GeoJsonSource(ROUTE_SRC))
                    style.addSource(GeoJsonSource(POINTS_SRC))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER, ROUTE_SRC).withProperties(
                            lineColor("#B2571F"), // terracota
                            lineWidth(5f),
                            lineCap(Property.LINE_CAP_ROUND),
                            lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )
                    style.addLayer(
                        CircleLayer(POINTS_LAYER, POINTS_SRC).withProperties(
                            circleColor(Expression.toColor(Expression.get("color"))),
                            circleRadius(Expression.get("r")),
                            circleStrokeColor("#FFFFFF"),
                            circleStrokeWidth(3f)
                        )
                    )
                    map = m
                    styleReady = true
                }
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

    // Dibuja ruta y puntos cada vez que cambian
    LaunchedEffect(styleReady, route, courier, destination, restaurant) {
        if (!styleReady) return@LaunchedEffect
        val style = map?.style ?: return@LaunchedEffect

        val routeFeatures =
            if (route.size > 1) listOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(route.map { Point.fromLngLat(it.lon, it.lat) })
                )
            ) else emptyList()
        style.getSourceAs<GeoJsonSource>(ROUTE_SRC)
            ?.setGeoJson(FeatureCollection.fromFeatures(routeFeatures))

        val pointFeatures = buildList {
            restaurant?.let { add(pointFeature(it, "#B2571F", 9f)) } // restaurante: terracota
            destination?.let { add(pointFeature(it, "#C62828", 10f)) } // destino: rojo
            courier?.let { add(pointFeature(it, "#1565C0", 9f)) } // repartidor: azul
        }
        style.getSourceAs<GeoJsonSource>(POINTS_SRC)
            ?.setGeoJson(FeatureCollection.fromFeatures(pointFeatures))
    }

    // 1) Cuando se conoce el destino: lo enfoca y espera 3 segundos
    LaunchedEffect(destination, map) {
        val m = map
        if (m != null && destination != null && !introDone) {
            m.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(destination.lat, destination.lon),
                    ZOOM_DESTINATION
                ),
                800
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
    LaunchedEffect(courier, introDone, map) {
        val m = map ?: return@LaunchedEffect
        if (introDone && courier != null) {
            val point = LatLng(courier.lat, courier.lon)
            if (!followStarted) {
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(point, ZOOM_FOLLOW), 800)
                followStarted = true
            } else {
                m.animateCamera(CameraUpdateFactory.newLatLng(point), 800)
            }
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}