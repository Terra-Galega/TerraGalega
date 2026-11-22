package com.example.terragalegamovile.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Avisa al cliente (notificación + vibración) de dos momentos de su domicilio:
//  1. El repartidor inició el recorrido.
//  2. El recorrido llegó al 85 %: "alístate, ya va llegando".
// Consulta al backend cada pocos segundos mientras haya un cliente con sesión.
// Cada aviso sale una sola vez por recorrido (se recuerda en AppStorage).
object DeliveryNotifier {

    // Avance a partir del cual se avisa "ya va llegando"
    const val NEAR_PROGRESS = 0.85

    // Extra del Intent: al tocar la notificación se abre la pantalla de seguimiento
    const val EXTRA_OPEN_TRACKING = "open_tracking"

    // MainActivity lo enciende al recibir el toque; AppNav navega y lo apaga
    val openTrackingRequest = MutableStateFlow(false)

    // Los canales son distintos porque la vibración se define en el canal
    // y no se puede cambiar después de crearlo.
    private const val CHANNEL_STARTED = "delivery_started"
    private const val CHANNEL_NEAR = "delivery_near"

    private const val POLL_MILLIS = 5_000L

    // Una pulsación corta al salir; tres largas cuando ya va llegando
    private val VIBRATION_STARTED = longArrayOf(0, 300)
    private val VIBRATION_NEAR = longArrayOf(0, 600, 250, 600, 250, 600)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    // Se llama una vez al abrir la app. Es seguro llamarla varias veces.
    @Synchronized
    fun start(context: Context) {
        if (job != null) return
        val app = context.applicationContext
        createChannels(app)

        job = scope.launch {
            // Solo los clientes reciben avisos; al cerrar sesión se detiene la consulta
            Session.state
                .map { user -> user?.takeIf { it.role == "CLIENT" }?.id }
                .distinctUntilChanged()
                .collectLatest { clientId ->
                    if (clientId == null) return@collectLatest

                    while (isActive) {
                        try {
                            checkDeliveries(app, ApiClient.api.clientActive(clientId))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            // Sin conexión o error del servidor: se reintenta en el siguiente ciclo
                        }
                        delay(POLL_MILLIS)
                    }
                }
        }
    }

    private fun checkDeliveries(context: Context, deliveries: List<ClientDelivery>) {
        val activeTrips = HashSet<String>()

        for (delivery in deliveries) {
            // Mientras el repartidor no inicie el recorrido no hay nada que avisar
            val startedAt = delivery.startedAt ?: continue
            val tripKey = "${delivery.orderId}:$startedAt"
            activeTrips.add(tripKey)

            val nearDue = (delivery.progress ?: 0.0) >= NEAR_PROGRESS &&
                !AppStorage.isAlerted(tripKey, "near")
            val startedDue = !AppStorage.isAlerted(tripKey, "started")

            if (nearDue) {
                // Si se llega aquí sin haber visto el inicio, solo importa el aviso de llegada
                AppStorage.markAlerted(tripKey, "near")
                AppStorage.markAlerted(tripKey, "started")
                post(
                    context,
                    id = delivery.orderId * 10 + 2,
                    channel = CHANNEL_NEAR,
                    title = "¡Ya va llegando! Alístate",
                    text = "Pedido #${delivery.orderId}: tu repartidor está a punto de llegar. " +
                        "Ten tu PIN a la mano.",
                )
            } else if (startedDue) {
                AppStorage.markAlerted(tripKey, "started")
                val who = delivery.deliveryPersonName ?: "El repartidor"
                post(
                    context,
                    id = delivery.orderId * 10 + 1,
                    channel = CHANNEL_STARTED,
                    title = "¡Tu pedido ya va en camino!",
                    text = "Pedido #${delivery.orderId}: $who inició el recorrido. " +
                        "Síguelo en el mapa.",
                )
            }
        }

        AppStorage.pruneAlerts(activeTrips)
    }

    private fun post(context: Context, id: Int, channel: String, title: String, text: String) {
        // Android 13+: sin permiso no se puede mostrar la notificación
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TRACKING, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val vibration = if (channel == CHANNEL_NEAR) VIBRATION_NEAR else VIBRATION_STARTED

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setColor(0xFFB2571F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            // Android 7 y 8 antes de los canales: la vibración va en la propia notificación
            .setVibrate(vibration)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // El permiso se retiró justo ahora: no hay nada que mostrar
        }
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun channel(id: String, name: String, description: String, pattern: LongArray) =
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                enableVibration(true)
                vibrationPattern = pattern
                setSound(sound, attributes)
            }

        manager.createNotificationChannel(
            channel(
                CHANNEL_STARTED,
                "Pedido en camino",
                "Avisa cuando el repartidor inicia el recorrido",
                VIBRATION_STARTED
            )
        )
        manager.createNotificationChannel(
            channel(
                CHANNEL_NEAR,
                "Tu pedido ya va llegando",
                "Vibra y avisa cuando el recorrido va por el 85 %",
                VIBRATION_NEAR
            )
        )
    }
}
