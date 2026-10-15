package dev.glassclock.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.glassclock.MainActivity
import dev.glassclock.R
import dev.glassclock.alarm.nextTrigger
import dev.glassclock.data.Stores
import dev.glassclock.util.Notif
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.formatUntil
import dev.glassclock.util.is24h
import dev.glassclock.util.nextOccurrenceOf
import dev.glassclock.util.setExactCompat

object Bedtime {
    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        am.cancel(pi)
        val s = Stores.settings.value
        if (!s.bedtimeEnabled || s.bedtimeDays.isEmpty()) return
        val next = nextOccurrenceOf(s.bedtimeHour, s.bedtimeMinute, s.bedtimeDays)
        am.setExactCompat(next.toInstant().toEpochMilli(), pi)
    }

    /** Próxima alarma activa (epoch ms) o null. */
    fun nextAlarm(nowMs: Long = System.currentTimeMillis()): Long? =
        Stores.alarms.value.mapNotNull { it.nextTrigger(nowMs) }.minOrNull()

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, BedtimeReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        val text = Bedtime.nextAlarm(now)?.let {
            "Tu próxima alarma suena a las ${formatInstantClock(it, is24h(context))}. " +
                "Si te duermes ahora, dormirás ${formatUntil(it - now)}."
        } ?: "No tienes alarmas activas."
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, Notif.CH_BEDTIME)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de dormir")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(Notif.ID_BEDTIME, n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones.
        }
        Bedtime.schedule(context)
    }
}
