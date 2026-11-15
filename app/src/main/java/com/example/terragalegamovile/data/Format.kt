package com.example.terragalegamovile.data

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

// Mismo valor que DELIVERY_FEE de la web y del backend
const val DELIVERY_FEE = 6000.0

object Format {
    private val numbers = NumberFormat.getIntegerInstance(Locale.US)

    // $12,500 (igual que number:'1.0-0' en la web)
    fun money(value: Double): String = "$" + numbers.format(value.roundToLong())

    private val months = listOf(
        "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
    )

    // "2026-11-10T20:22:33" -> "10 nov 2026, 20:22"
    fun dateTime(iso: String?): String {
        if (iso == null || iso.length < 16) return ""
        val month = iso.substring(5, 7).toIntOrNull() ?: return iso
        if (month !in 1..12) return iso
        return "${iso.substring(8, 10)} ${months[month - 1]} ${iso.substring(0, 4)}, ${iso.substring(11, 16)}"
    }

    fun statusLabel(status: String): String = when (status) {
        "PENDING" -> "Pendiente"
        "CONFIRMED" -> "Confirmado"
        "PREPARING" -> "Preparando"
        "ON_THE_WAY" -> "En camino"
        "DELIVERED" -> "Entregado"
        "CANCELLED" -> "Cancelado"
        else -> status
    }

    // [Queso, Queso, Salsa] -> [("Queso", 2), ("Salsa", 1)]
    fun group(names: List<String>): List<Pair<String, Int>> =
        names.groupingBy { it }.eachCount().toList()

    // "10 %" o "$5,000"
    fun couponValue(type: String, value: Double): String =
        if (type == "PERCENTAGE") "${value.toInt()} %" else money(value)
}