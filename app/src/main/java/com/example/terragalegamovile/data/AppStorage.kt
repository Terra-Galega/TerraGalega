package com.example.terragalegamovile.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

// Datos que la app recuerda aunque se cierre: sesión, direcciones, rutas y progreso del recorrido
object AppStorage {

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("terragalega", Context.MODE_PRIVATE)
    }

    // ---------- Sesión ----------

    fun saveSession(user: LoginResponse?) {
        val editor = prefs.edit()
        if (user == null) editor.remove("session") else editor.putString("session", gson.toJson(user))
        editor.apply()
    }

    fun loadSession(): LoginResponse? = try {
        gson.fromJson(prefs.getString("session", null), LoginResponse::class.java)
    } catch (e: Exception) {
        null
    }

    // ---------- Dirección -> coordenadas, y ruta ----------

    fun getDestination(address: String): LatLon? =
        prefs.getString("dest:$address", null)?.let { parsePoint(it) }

    fun saveDestination(address: String, point: LatLon) {
        prefs.edit().putString("dest:$address", "${point.lat},${point.lon}").apply()
    }

    // La clave incluye el restaurante: si cambias sus coordenadas, las rutas viejas no se reusan
    private fun routeKey(address: String) =
        "route:${Restaurant.location.lat},${Restaurant.location.lon}|$address"

    fun getRoute(address: String): List<LatLon>? {
        val raw = prefs.getString(routeKey(address), null) ?: return null
        return try {
            raw.split(";").map { parsePoint(it)!! }.takeIf { it.size > 1 }
        } catch (e: Exception) {
            null
        }
    }

    fun saveRoute(address: String, route: List<LatLon>) {
        prefs.edit()
            .putString(routeKey(address), route.joinToString(";") { "${it.lat},${it.lon}" })
            .apply()
    }

    private fun parsePoint(text: String): LatLon? {
        val parts = text.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return LatLon(lat, lon)
    }

    // ---------- Progreso del recorrido del repartidor ----------

    fun isTripStarted(orderId: Int) = prefs.getBoolean("trip_started_$orderId", false)

    fun setTripStarted(orderId: Int, started: Boolean) {
        prefs.edit().putBoolean("trip_started_$orderId", started).apply()
    }

    fun getTraveled(orderId: Int): Double = prefs.getFloat("trip_meters_$orderId", 0f).toDouble()

    fun saveTraveled(orderId: Int, meters: Double) {
        prefs.edit().putFloat("trip_meters_$orderId", meters.toFloat()).apply()
    }

    fun clearTrip(orderId: Int) {
        prefs.edit().remove("trip_started_$orderId").remove("trip_meters_$orderId").apply()
    }
}