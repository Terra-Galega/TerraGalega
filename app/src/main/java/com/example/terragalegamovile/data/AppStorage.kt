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

    // ---------- Avisos de domicilio ya mostrados ----------
    // Cada elemento es "pedido:horaDeInicio|tipo" (tipo = started o near), para
    // que cada aviso salga una sola vez por recorrido aunque se cierre la app.

    @Synchronized
    fun isAlerted(tripKey: String, type: String): Boolean =
        prefs.getStringSet("delivery_alerts", emptySet())?.contains("$tripKey|$type") == true

    @Synchronized
    fun markAlerted(tripKey: String, type: String) {
        val updated = HashSet(prefs.getStringSet("delivery_alerts", emptySet()) ?: emptySet())
        updated.add("$tripKey|$type")
        prefs.edit().putStringSet("delivery_alerts", updated).apply()
    }

    // Olvida los recorridos que ya terminaron
    @Synchronized
    fun pruneAlerts(activeTripKeys: Set<String>) {
        val current = prefs.getStringSet("delivery_alerts", emptySet()) ?: return
        val kept = current.filterTo(HashSet()) { activeTripKeys.contains(it.substringBefore('|')) }
        if (kept.size != current.size) {
            prefs.edit().putStringSet("delivery_alerts", kept).apply()
        }
    }

    // ---------- Borrador del pedido de mesa (mesero) ----------
    // Se guarda en cada cambio: si se cierra la app, se cae la red o vence la
    // sesión, al volver a la mesa el pedido sigue ahí. La clave lleva el mesero.

    // Chat con el restaurante: último mensaje visto de cada pedido (para el puntito de nuevos)
    fun loadChatSeen(clientId: Int): Map<Int, Int> = try {
        (prefs.getString("order_chat_seen_$clientId", "") ?: "")
            .split(',')
            .mapNotNull { pair -> pair.split(':').takeIf { it.size == 2 }?.let { it[0].toInt() to it[1].toInt() } }
            .toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    fun saveChatSeen(clientId: Int, seen: Map<Int, Int>) {
        prefs.edit().putString(
            "order_chat_seen_$clientId",
            seen.entries.toList().takeLast(50).joinToString(",") { "${it.key}:${it.value}" }
        ).apply()
    }

    // Modo claro/oscuro elegido en la app (null = como el teléfono)
    fun loadTheme(): String? = prefs.getString("app_theme", null)

    fun saveTheme(mode: String?) {
        prefs.edit().putString("app_theme", mode).apply()
    }

    // Idioma elegido en la app (null = nunca se eligió)
    fun loadLanguage(): String? = prefs.getString("app_language", null)

    fun saveLanguage(code: String) {
        prefs.edit().putString("app_language", code).apply()
    }

    fun saveWaiterDraft(waiterId: Int, tableId: Int, draft: WaiterDraft?) {
        val key = "waiterDraft:$waiterId:$tableId"
        val editor = prefs.edit()
        if (draft == null || draft.lines.isEmpty()) editor.remove(key) else editor.putString(key, gson.toJson(draft))
        editor.apply()
    }

    fun loadWaiterDraft(waiterId: Int, tableId: Int): WaiterDraft? = try {
        gson.fromJson(prefs.getString("waiterDraft:$waiterId:$tableId", null), WaiterDraft::class.java)
            ?.takeIf { it.lines.isNotEmpty() && it.requestId.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

// Pedido de mesa que se está armando. requestId se mantiene en los reintentos y
// cambia solo cuando el pedido llega a cocina.
data class WaiterDraft(
    val requestId: String,
    val forReservation: Boolean?,
    val lines: List<WaiterDraftLine>
)

data class WaiterDraftLine(
    val product: Product,
    val quantity: Int,
    val notes: String,
    val addOns: List<AddOnOption>
)
