package dev.glassclock.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.glassclock.R

object Notif {
    const val CH_RING = "ring"
    const val CH_TIMER = "timers"
    const val CH_SNOOZE = "snoozed"
    const val CH_BEDTIME = "bedtime"
    const val CH_UPCOMING = "upcoming"

    const val ID_RING = 1
    const val ID_BEDTIME = 2
    fun snoozeId(alarmId: Int) = 10_000 + alarmId
    fun timerId(timerId: Int) = 20_000 + timerId
    fun upcomingId(alarmId: Int) = 30_000 + alarmId

    /**
     * Notificación con el icono pequeño de la app y el azul del tema. One UI muestra a la izquierda
     * el icono base de la app (Samsung), que no cambia aunque se elija otro en Ajustes.
     */
    fun builder(context: Context, channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1A73E8.toInt())

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_RING, "Alarmas sonando", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alarmas y temporizadores mientras suenan"
                    setSound(null, null)
                    enableVibration(false)
                    setBypassDnd(true)
                },
                NotificationChannel(CH_TIMER, "Temporizadores", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Cuenta atrás de temporizadores en curso"
                },
                NotificationChannel(CH_SNOOZE, "Alarmas pospuestas", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_UPCOMING, "Próximas alarmas", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Aviso 2 horas antes para poder descartar la alarma"
                },
                NotificationChannel(CH_BEDTIME, "Modo sueño", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Recordatorio de la hora de dormir"
                },
            ),
        )
    }
}
