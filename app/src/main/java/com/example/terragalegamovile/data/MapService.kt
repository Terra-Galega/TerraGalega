package com.example.terragalegamovile.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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

    // Dirección escrita -> coordenadas. Si la dirección exacta no se encuentra,
    // prueba versiones más simples hasta quedarse solo con la ciudad.
    suspend fun geocode(address: String): LatLon? = withContext(Dispatchers.IO) {
        val candidates = listOf(
            address,
            address.replace("#", " "),
            address.substringAfterLast(",").trim()
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
}