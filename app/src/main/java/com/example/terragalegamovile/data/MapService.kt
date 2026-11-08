package com.example.terragalegamovile.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import android.content.Context
import android.location.Geocoder
import java.util.Locale

data class LatLon(val lat: Double, val lon: Double)

object MapService {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TerraGalegaMovil/1.0 (proyecto universitario)")
            .build()
        http.newCall(request).execute().use { response ->
            return if (response.isSuccessful) response.body?.string() else null
        }
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Geocodificador de Google que trae Android (sin clave). Primero busca solo dentro
    // de Bogotá y, si no encuentra nada, sin restricción.
    @Suppress("DEPRECATION")
    private fun geocodeWithGoogle(address: String): LatLon? {
        val context = appContext ?: return null
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("es", "CO"))
        for (query in listOf(address, normalize(address)).distinct()) {
            try {
                val found = geocoder.getFromLocationName(query, 1, 4.45, -74.25, 4.85, -73.99)
                    ?.firstOrNull()
                    ?: geocoder.getFromLocationName(query, 1)?.firstOrNull()
                if (found != null) return LatLon(found.latitude, found.longitude)
            } catch (_: Exception) {
            }
        }
        return null
    }

    // "Cl. 93 #15-47" -> "Calle 93 15-47", que Nominatim entiende mejor
    private fun normalize(address: String): String = address
        .replace(Regex("\\b(Cl|Cll)\\.?\\s", RegexOption.IGNORE_CASE), "Calle ")
        .replace(Regex("\\b(Cra|Kr|Cr)\\.?\\s", RegexOption.IGNORE_CASE), "Carrera ")
        .replace("#", " ")
        .replace(Regex("\\s+"), " ")

    // Dirección escrita -> coordenadas. Con allowCityFallback = true, si nada funciona
    // termina usando solo la ciudad.
    suspend fun geocode(address: String, allowCityFallback: Boolean = false): LatLon? = withContext(Dispatchers.IO) {
        geocodeWithGoogle(address)?.let { return@withContext it }   // <-- NUEVA
        val candidates = listOfNotNull(
            address,
            normalize(address),
            address.replace("#", " "),
            address.substringAfterLast(",").trim().takeIf { allowCityFallback }
        ).filter { it.isNotBlank() }.map { "$it, Colombia" }.distinct()

        for ((index, query) in candidates.withIndex()) {
            if (index > 0) delay(1100) // Nominatim pide máximo 1 petición por segundo
            try {
                val url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
                        URLEncoder.encode(query, "UTF-8")
                val body = get(url) ?: continue
                val results = JsonParser.parseString(body).asJsonArray
                if (results.size() > 0) {
                    val first = results[0].asJsonObject
                    return@withContext LatLon(
                        first.get("lat").asString.toDouble(),
                        first.get("lon").asString.toDouble()
                    )
                }
            } catch (_: Exception) {
            }
        }
        null
    }

    // Puntos de la ruta en carro entre dos lugares (lista vacía si falla)
    suspend fun route(from: LatLon, to: LatLon): List<LatLon> = withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "${from.lon},${from.lat};${to.lon},${to.lat}?overview=full&geometries=geojson"
            val body = get(url) ?: return@withContext emptyList()
            val coordinates = JsonParser.parseString(body).asJsonObject
                .getAsJsonArray("routes")[0].asJsonObject
                .getAsJsonObject("geometry")
                .getAsJsonArray("coordinates")
            coordinates.map {
                val point = it.asJsonArray
                LatLon(point[1].asDouble, point[0].asDouble)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Dirección -> coordenadas, con memoria y reintentos
    suspend fun destinationFor(address: String): LatLon? {
        AppStorage.getDestination(address)?.let { return it }
        repeat(4) {
            val result = geocode(address)
            if (result != null) {
                AppStorage.saveDestination(address, result)
                return result
            }
            delay(2500)
        }
        return null
    }

    // Ruta restaurante -> destino, con memoria y reintentos
    suspend fun routeFor(address: String, to: LatLon): List<LatLon> {
        AppStorage.getRoute(address)?.let { return it }
        repeat(4) {
            val result = route(Restaurant.location, to)
            if (result.size > 1) {
                AppStorage.saveRoute(address, result)
                return result
            }
            delay(2500)
        }
        return emptyList()
    }
}