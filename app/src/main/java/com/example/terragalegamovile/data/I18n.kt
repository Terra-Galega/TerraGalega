package com.example.terragalegamovile.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

// Idiomas de la app: español (el de los textos del código), inglés y catalán.
// El texto en español es la clave: t("Mis pedidos") busca su traducción en
// assets/i18n/en.json o ca.json; si falta, se muestra en español.
// Los textos con datos usan {}: t("Pedido #{}", id).
object I18n {

    // Códigos y nombres del selector (cada idioma con su propio nombre)
    val languages = listOf("es" to "Español", "en" to "English", "ca" to "Català")

    // Al cambiarlo, todo lo que se dibujó con t() se vuelve a dibujar solo
    var lang by mutableStateOf("es")
        private set

    private var dictionaries: Map<String, Map<String, String>> = emptyMap()

    fun init(context: Context) {
        if (dictionaries.isEmpty()) {
            dictionaries = listOf("en", "ca").associateWith { load(context, it) }
        }
        // Lo elegido en la app; si nunca se eligió, el idioma del teléfono
        val saved = AppStorage.loadLanguage()
        val device = Locale.getDefault().language
        lang = saved ?: if (device in setOf("en", "ca")) device else "es"
    }

    fun select(code: String) {
        if (languages.none { it.first == code }) return
        lang = code
        AppStorage.saveLanguage(code)
    }

    internal fun translate(es: String): String =
        if (lang == "es") es else dictionaries[lang]?.get(es) ?: es

    private fun load(context: Context, code: String): Map<String, String> = try {
        context.assets.open("i18n/$code.json").bufferedReader(Charsets.UTF_8).use {
            Gson().fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

// Texto traducido al idioma elegido. Cada {} se cambia por el siguiente dato.
fun t(es: String, vararg args: Any?): String {
    var text = I18n.translate(es)
    for (arg in args) text = text.replaceFirst("{}", arg.toString())
    return text
}
