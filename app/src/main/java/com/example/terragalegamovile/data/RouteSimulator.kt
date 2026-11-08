package com.example.terragalegamovile.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Punto de partida de todos los domicilios (el "restaurante")
object Restaurant {
    const val ADDRESS = "Cl. 93 #15-47, Bogotá"

    // Coordenadas aproximadas de esa dirección. Para afinarlas: en Google Maps,
    // mantén pulsado el punto exacto y copia los dos números que aparecen arriba.
    val location = LatLon(4.6767, -74.0503)
}

// Qué tan rápido y con qué frecuencia se mueve la repartidora simulada
object Simulation {
    const val SPEED_METERS_PER_SECOND = 60.0 // velocidad del recorrido simulado
    const val TICK_MILLIS = 1000L // cada cuánto se manda una posición

    // false = el botón "Iniciar entrega" simula el recorrido solo.
    // true = usa el GPS real del teléfono.
    const val USE_REAL_GPS = false
}
fun distanceMeters(a: LatLon, b: LatLon): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
}

// Recorrido con la distancia acumulada de cada punto, para poder preguntar
// "¿dónde estoy a X metros del inicio?"
class RoutePath(private val points: List<LatLon>) {

    private val cumulative = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) {
            acc[i] = acc[i - 1] + distanceMeters(points[i - 1], points[i])
        }
    }

    val totalMeters: Double = cumulative.lastOrNull() ?: 0.0

    fun pointAt(meters: Double): LatLon {
        val d = meters.coerceIn(0.0, totalMeters)
        for (i in 1 until points.size) {
            if (cumulative[i] >= d) {
                val segment = cumulative[i] - cumulative[i - 1]
                val t = if (segment == 0.0) 0.0 else (d - cumulative[i - 1]) / segment
                val a = points[i - 1]
                val b = points[i]
                return LatLon(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
            }
        }
        return points.last()
    }
}