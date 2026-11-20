package com.example.terragalegamovile.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException

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

    // Dirección escrita -> coordenadas, con el MISMO geocodificador que la web:
    // el back (Google Geocoding, y Nominatim de respaldo). null si no la encuentra.
    // allowCityFallback ya no hace falta: Google entiende la nomenclatura colombiana.
    @Suppress("UNUSED_PARAMETER")
    suspend fun geocode(address: String, allowCityFallback: Boolean = false): LatLon? = withContext(Dispatchers.IO) {
        try {
            val body = ApiClient.api.mapSearch(address).use { it.string() }
            if (body.isBlank() || body == "null") return@withContext null
            val found = Gson().fromJson(body, MapAddress::class.java)
            LatLon(found.lat, found.lon)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
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

    // Coordenadas -> dirección escrita (lo contrario de geocode), también en el back
    suspend fun reverse(p: LatLon): String? = withContext(Dispatchers.IO) {
        try {
            ApiClient.api.mapReverse(p.lat, p.lon).label
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
