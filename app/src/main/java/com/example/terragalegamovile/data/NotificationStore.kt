package com.example.terragalegamovile.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

// Bandeja de notificaciones del cliente: las guarda el back (las mismas de la web,
// menos los anuncios). Aquí se leen cada pocos segundos y se avisa de las nuevas.
object NotificationStore {

    private const val POLL_MILLIS = 8_000L
    private const val CHANNEL = "general"

    var items by mutableStateOf<List<AppNotification>>(emptyList())
        private set

    val unread: Int get() = items.count { !it.read }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    // Ids ya vistos, para avisar solo de las nuevas (y no de las viejas al abrir la app)
    private val known = HashSet<Int>()
    private var firstLoad = true

    // Se llama una vez al abrir la app. Es seguro llamarla varias veces.
    fun start(context: Context) {
        if (job != null) return
        val app = context.applicationContext
        createChannel(app)

        job = scope.launch {
            Session.state
                .map { user -> user?.takeIf { it.role == "CLIENT" }?.id }
                .distinctUntilChanged()
                .collectLatest { clientId ->
                    items = emptyList()
                    known.clear()
                    firstLoad = true
                    if (clientId == null) return@collectLatest

                    while (isActive) {
                        try {
                            receive(app, ApiClient.api.notifications())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Sin conexión: se reintenta en el siguiente ciclo
                        }
                        delay(POLL_MILLIS)
                    }
                }
        }
    }

    fun markRead(id: Int) {
        items = items.map { if (it.id == id) it.copy(read = true) else it }
        scope.launch {
            try {
                ApiClient.api.notificationRead(id).close()
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
                ApiClient.api.notificationsReadAll().close()
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

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_delivery)
            .setContentTitle(n.title)
            .setContentText(n.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            // 100000 + id para no chocar con los ids de DeliveryNotifier
            NotificationManagerCompat.from(context).notify(100_000 + n.id, notification)
        } catch (_: SecurityException) {
            // El permiso se retiró justo ahora
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Avisos de TerraGalega", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}