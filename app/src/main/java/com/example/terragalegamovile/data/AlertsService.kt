package com.example.terragalegamovile.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.terragalegamovile.MainActivity
import com.example.terragalegamovile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// t("Avisos activos"): servicio en primer plano mientras hay una sesión de cliente o
// de mesero. Mantiene viva la app aunque se cierre, se quite de recientes o se
// apague la pantalla, así siguen llegando todas las notificaciones:
//  - Cliente: su bandeja (pedido confirmado, en cocina, reservas, cupones...) y
//    los avisos del domicilio ("salió", "ya va llegando").
//  - Mesero: los avisos de cocina de sus pedidos de mesa.
// Se ve como una notificación fija y discreta; al cerrar sesión desaparece.
// También vuelve a arrancar solo al reiniciar el teléfono (BootReceiver).
//
// Nota: un push real (Firebase Cloud Messaging) gastaría menos batería, pero
// necesita un proyecto de Firebase y su google-services.json.
class AlertsService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Si Android reinició el servicio con la app cerrada, se prepara todo aquí
        AppStorage.init(applicationContext)
        I18n.init(applicationContext)
        if (Session.user == null) Session.restore()
        NotificationStore.start(applicationContext)
        DeliveryNotifier.start(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val role = Session.user?.role
        if (!shouldRun(role)) {
            stopSelf()
            return START_NOT_STICKY
        }
        goForeground(role == "WAITER")
        holdWakeLock()

        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        // Termina al cerrar sesión (o si la sesión vence)
        scope.launch {
            Session.state.first { !shouldRun(it?.role) }
            stopSelf()
        }
        // Renueva el token antes de que venza, aunque la app no esté abierta
        scope.launch {
            while (isActive) {
                Session.renewIfNeeded()
                delay(RENEW_CHECK_MS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun goForeground(waiter: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(
                // Importancia mínima: no suena ni sale arriba, solo queda en la bandeja
                NotificationChannel(CHANNEL, t("Avisos activos"), NotificationManager.IMPORTANCE_MIN).apply {
                    description = t("Mantiene llegando tus notificaciones aunque cierres la app")
                    setShowBadge(false)
                }
            )
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setColor(0xFFB2571F.toInt())
            .setContentTitle("Terra Galega")
            .setContentText(
                Session.user?.name?.trim()?.takeIf { it.isNotEmpty() }
                    ?.let { t("Es un placer tenerte de vuelta, {}.", it) }
                    ?: t("Es un placer tenerte de vuelta.")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // Sin esto, con la pantalla apagada el teléfono duerme y los avisos llegan
    // tarde. Se suelta al cerrar sesión; el tiempo máximo evita dejarlo pegado.
    private fun holdWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TerraGalega:avisos").apply {
            setReferenceCounted(false)
            acquire(MAX_HOLD_MS)
        }
    }

    companion object {
        private const val CHANNEL = "alerts_service"
        private const val NOTIFICATION_ID = 300_001
        private const val RENEW_CHECK_MS = 10 * 60_000L
        private const val MAX_HOLD_MS = 12 * 60 * 60_000L

        // Roles que reciben notificaciones en la app
        private val ROLES = setOf("CLIENT", "WAITER")

        fun shouldRun(role: String?): Boolean = role in ROLES

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlertsService::class.java))
            } catch (_: Exception) {
                // Android no deja iniciarlo desde segundo plano: se intenta al volver a la app
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlertsService::class.java))
        }
    }
}

// Al reiniciar el teléfono (o al actualizar la app) vuelve a encender los avisos
// si quedó una sesión de cliente o mesero guardada.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        AppStorage.init(context.applicationContext)
        if (Session.user == null) Session.restore()
        if (AlertsService.shouldRun(Session.user?.role)) AlertsService.start(context)
    }
}
