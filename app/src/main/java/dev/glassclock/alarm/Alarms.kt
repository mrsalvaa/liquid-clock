package dev.glassclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.glassclock.MainActivity
import dev.glassclock.R
import dev.glassclock.data.Alarm
import dev.glassclock.data.Stores
import dev.glassclock.util.Notif
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.is24h
import dev.glassclock.util.nextOccurrenceOf
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Próxima ocurrencia regular, respetando la omitida. */
fun Alarm.nextOccurrence(nowMs: Long = System.currentTimeMillis()): ZonedDateTime {
    val now = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
    var next = nextOccurrenceOf(hour, minute, days, now)
    if (skippedOccurrence != null && next.toInstant().toEpochMilli() == skippedOccurrence) {
        next = nextOccurrenceOf(hour, minute, days, next)
    }
    return next
}

/** Próximo disparo (pospuesta o regular), o null si está desactivada. */
fun Alarm.nextTrigger(nowMs: Long = System.currentTimeMillis()): Long? {
    if (!enabled) return null
    snoozedUntil?.let { if (it > nowMs) return it }
    return nextOccurrence(nowMs).toInstant().toEpochMilli()
}

val Alarm.isRepeating get() = days.isNotEmpty()

object Alarms {
    const val EXTRA_ID = "alarm_id"

    fun get(id: Int): Alarm? = Stores.alarms.value.firstOrNull { it.id == id }

    fun newId(): Int = (Stores.alarms.value.maxOfOrNull { it.id } ?: 0) + 1

    fun save(context: Context, alarm: Alarm) {
        Stores.alarms.update { list ->
            if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it }
            else list + alarm
        }
        cancelSnoozeNotification(context, alarm.id)
        schedule(context, alarm)
    }

    fun delete(context: Context, id: Int) {
        Stores.alarms.update { list -> list.filterNot { it.id == id } }
        cancel(context, id)
        cancelSnoozeNotification(context, id)
    }

    fun duplicate(context: Context, id: Int) {
        val alarm = get(id) ?: return
        save(context, alarm.copy(id = newId(), skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0))
    }

    fun setEnabled(context: Context, id: Int, enabled: Boolean) {
        val alarm = get(id) ?: return
        save(context, alarm.copy(enabled = enabled, skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0))
    }

    /** Omite la próxima ocurrencia; una alarma única se desactiva. */
    fun skipNext(context: Context, id: Int) {
        val alarm = get(id) ?: return
        val updated = if (alarm.isRepeating) {
            val base = alarm.copy(skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0)
            base.copy(skippedOccurrence = base.nextOccurrence().toInstant().toEpochMilli())
        } else {
            alarm.copy(enabled = false, snoozedUntil = null, snoozeCount = 0)
        }
        save(context, updated)
    }

    fun unskip(context: Context, id: Int) {
        val alarm = get(id) ?: return
        save(context, alarm.copy(skippedOccurrence = null))
    }

    /** La alarma acaba de sonar: limpia el estado pospuesto y prepara la siguiente repetición. */
    fun onFired(context: Context, id: Int): Alarm? {
        val alarm = get(id) ?: return null
        val updated = alarm.copy(snoozedUntil = null)
        update(updated)
        cancelSnoozeNotification(context, id)
        if (updated.isRepeating) schedule(context, updated)
        return updated
    }

    fun snooze(context: Context, id: Int, minutes: Int) {
        val alarm = get(id) ?: return
        val until = System.currentTimeMillis() + minutes * 60_000L
        val updated = alarm.copy(snoozedUntil = until, snoozeCount = alarm.snoozeCount + 1)
        update(updated)
        schedule(context, updated)
        showSnoozeNotification(context, updated, until)
    }

    fun dismiss(context: Context, id: Int) {
        val alarm = get(id) ?: return
        val updated = alarm.copy(
            snoozedUntil = null,
            snoozeCount = 0,
            enabled = alarm.enabled && alarm.isRepeating,
        )
        update(updated)
        cancelSnoozeNotification(context, id)
        schedule(context, updated)
    }

    fun rescheduleAll(context: Context) {
        Stores.alarms.value.forEach { schedule(context, it) }
    }

    private fun update(alarm: Alarm) {
        Stores.alarms.update { list -> list.map { if (it.id == alarm.id) alarm else it } }
    }

    private fun schedule(context: Context, alarm: Alarm) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = firePendingIntent(context, alarm.id)
        am.cancel(pi)
        val trigger = alarm.nextTrigger() ?: return
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun cancel(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(context, id))
    }

    private fun firePendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun showSnoozeNotification(context: Context, alarm: Alarm, until: Long) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val dismiss = PendingIntent.getBroadcast(
            context, Notif.snoozeId(alarm.id),
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_DISMISS_SNOOZED)
                .putExtra(EXTRA_ID, alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, Notif.CH_SNOOZE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarm.label.ifBlank { "Alarma" } + " pospuesta")
            .setContentText("Sonará a las ${formatInstantClock(until, is24h(context))}")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Descartar", dismiss)
            .build()
        try {
            nm.notify(Notif.snoozeId(alarm.id), n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones.
        }
    }

    private fun cancelSnoozeNotification(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(Notif.snoozeId(id))
    }
}
