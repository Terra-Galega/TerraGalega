package com.example.terragalegamovile.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.MainActivity
import com.example.terragalegamovile.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Bandeja de notificaciones: las guarda el back y aquí se leen cada pocos segundos.
// - Cliente: las mismas de la web (menos los anuncios).
// - Mesero: los avisos de la cocina sobre sus pedidos de mesa; cada aviso nuevo
//   hace vibrar el teléfono y sale en la bandeja de Android.
object NotificationStore {

    private const val POLL_MILLIS = 8_000L
    // El mesero necesita enterarse rápido de lo que pasa en cocina
    private const val WAITER_POLL_MILLIS = 5_000L
    private const val CHANNEL = "general"
    private const val CHANNEL_KITCHEN = "kitchen"

    // Vibración: corta para un plato, más larga cuando todo el pedido está listo
    private val VIBRATION_DISH = longArrayOf(0, 200, 120, 200)
    private val VIBRATION_ORDER_READY = longArrayOf(0, 400, 150, 400, 150, 400)

    private val waiterTypes = setOf("DISH_PREPARING", "DISH_READY", "ORDER_READY")

    // ¿La sesión actual es de mesero? (decide de dónde se leen y marcan los avisos)
    private val isWaiter: Boolean get() = Session.user?.role == "WAITER"

    var items by mutableStateOf<List<AppNotification>>(emptyList())
        private set

    val unread: Int get() = items.count { !it.read }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var appContext: Context? = null

    // "Desliza para actualizar" de la bandeja
    suspend fun refreshNow() {
        val context = appContext ?: return
        val role = Session.user?.role ?: return
        try {
            receive(
                context,
                if (role == "WAITER") ApiClient.api.waiterNotifications() else ApiClient.api.notifications()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    // Ids ya vistos, para avisar solo de las nuevas (y no de las viejas al abrir la app)
    private val known = HashSet<Int>()
    private var firstLoad = true

    // Se llama una vez al abrir la app. Es seguro llamarla varias veces.
    fun start(context: Context) {
        if (job != null) return
        val app = context.applicationContext
        appContext = app
        createChannel(app)

        job = scope.launch {
            Session.state
                // Clientes y meseros tienen bandeja; cada uno lee la suya
                .map { user -> user?.takeIf { it.role == "CLIENT" || it.role == "WAITER" }?.let { it.role to it.id } }
                .distinctUntilChanged()
                .collectLatest { account ->
                    items = emptyList()
                    known.clear()
                    firstLoad = true
                    if (account == null) return@collectLatest
                    val waiter = account.first == "WAITER"

                    while (isActive) {
                        try {
                            receive(
                                app,
                                if (waiter) ApiClient.api.waiterNotifications() else ApiClient.api.notifications()
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Sin conexión: se reintenta en el siguiente ciclo
                        }
                        delay(if (waiter) WAITER_POLL_MILLIS else POLL_MILLIS)
                    }
                }
        }
    }

    fun markRead(id: Int) {
        items = items.map { if (it.id == id) it.copy(read = true) else it }
        scope.launch {
            try {
                (if (isWaiter) ApiClient.api.waiterNotificationRead(id) else ApiClient.api.notificationRead(id)).close()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun markAllRead() {
        items = items.map { it.copy(read = true) }
        scope.launch {
            try {
                (if (isWaiter) ApiClient.api.waiterNotificationsReadAll() else ApiClient.api.notificationsReadAll()).close()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private fun receive(context: Context, list: List<AppNotification>) {
        if (!firstLoad) {
            for (n in list) {
                // "Salió" y "ya va llegando" ya los avisa DeliveryNotifier (con vibración)
                val handledElsewhere = n.type == "ORDER_ON_THE_WAY" || n.type == "ORDER_NEAR"
                if (!n.read && n.id !in known && !handledElsewhere) post(context, n)
            }
        }
        known.addAll(list.map { it.id })
        firstLoad = false
        items = list
    }

    private fun post(context: Context, n: AppNotification) {
        val kitchen = n.type in waiterTypes
        // El mesero siente el aviso aunque no haya dado permiso de notificaciones
        if (kitchen) vibrate(context, if (n.type == "ORDER_READY") VIBRATION_ORDER_READY else VIBRATION_DISH)

        // Android 13+: sin permiso no se puede mostrar la notificación
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            n.id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, if (kitchen) CHANNEL_KITCHEN else CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setColor(0xFFB2571F.toInt())
            .setContentTitle(n.title)
            .setContentText(n.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.message))
            .setPriority(if (kitchen) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            // 100000 / 200000 + id para no chocar con los ids de DeliveryNotifier
            NotificationManagerCompat.from(context).notify((if (kitchen) 200_000 else 100_000) + n.id, notification)
        } catch (_: SecurityException) {
            // El permiso se retiró justo ahora
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, t("Avisos de TerraGalega"), NotificationManager.IMPORTANCE_DEFAULT)
        )
        // Avisos de cocina para el mesero: salen arriba en la pantalla. La vibración
        // la hace vibrate() (así no vibra dos veces)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_KITCHEN, t("Avisos de cocina (meseros)"), NotificationManager.IMPORTANCE_HIGH).apply {
                description = t("La cocina empezó o terminó un plato de tus mesas")
                enableVibration(false)
            }
        )
    }

    // Vibra con el patrón dado (en ms: espera, vibra, espera, vibra...)
    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}