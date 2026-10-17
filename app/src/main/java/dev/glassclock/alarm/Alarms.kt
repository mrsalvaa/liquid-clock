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
import dev.glassclock.data.AlarmGroup
import dev.glassclock.data.Stores
import dev.glassclock.util.Notif
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.is24h
import dev.glassclock.util.nextOccurrenceOf
import dev.glassclock.util.relativeDay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Próxima ocurrencia regular, respetando la omitida. */
fun Alarm.nextOccurrence(nowMs: Long = System.currentTimeMillis()): ZonedDateTime {
    // En pausa ("activar para mañana"): se busca a partir del final de la pausa.
    val fromMs = pausedUntil?.takeIf { it > nowMs }?.minus(1) ?: nowMs
    val now = Instant.ofEpochMilli(fromMs).atZone(ZoneId.systemDefault())
    var next = nextOccurrenceOf(hour, minute, days, now)
    if (skippedOccurrence != null && next.toInstant().toEpochMilli() == skippedOccurrence) {
        next = nextOccurrenceOf(hour, minute, days, next)
    }
    return next
}

/** Apagada hoy y se reactiva sola mañana. */
fun Alarm.isPaused(nowMs: Long = System.currentTimeMillis()) = enabled && pausedUntil?.let { it > nowMs } == true

/** Su grupo, si tiene. */
val Alarm.group: AlarmGroup? get() = groupId?.let { id -> Stores.groups.value.firstOrNull { it.id == id } }

/** Activa y en un grupo activo (o sin grupo): solo entonces suena. */
val Alarm.isActive: Boolean get() = enabled && group?.enabled != false

/** Próximo disparo (pospuesta o regular), o null si está desactivada. */
fun Alarm.nextTrigger(nowMs: Long = System.currentTimeMillis()): Long? {
    if (!isActive) return null
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
        save(
            context,
            alarm.copy(enabled = enabled, skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0, pausedUntil = null),
        )
    }

    /** Apaga la alarma hoy; mañana vuelve a sonar en sus días como siempre. */
    fun pauseUntilTomorrow(context: Context, id: Int) {
        val alarm = get(id) ?: return
        val tomorrow = java.time.LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        save(
            context,
            alarm.copy(enabled = true, pausedUntil = tomorrow, skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0),
        )
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
        cancelUpcoming(context, id)
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

    // --- Grupos ---

    fun groupOf(id: Int): AlarmGroup? = Stores.groups.value.firstOrNull { it.id == id }

    fun createGroup(name: String): Int {
        var newId = 0
        Stores.groups.update { list ->
            newId = (list.maxOfOrNull { it.id } ?: 0) + 1
            list + AlarmGroup(newId, name)
        }
        return newId
    }

    fun renameGroup(id: Int, name: String) {
        Stores.groups.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
    }

    fun setGroupCollapsed(id: Int, collapsed: Boolean) {
        Stores.groups.update { list -> list.map { if (it.id == id) it.copy(collapsed = collapsed) else it } }
    }

    /** Enciende o apaga el grupo; cada alarma conserva su propio interruptor. */
    fun setGroupEnabled(context: Context, id: Int, enabled: Boolean) {
        Stores.groups.update { list -> list.map { if (it.id == id) it.copy(enabled = enabled) else it } }
        Stores.alarms.update { list ->
            list.map {
                // Al apagar el grupo se olvidan las pospuestas y omitidas de sus alarmas.
                if (it.groupId == id && !enabled) it.copy(snoozedUntil = null, snoozeCount = 0, skippedOccurrence = null)
                else it
            }
        }
        Stores.alarms.value.filter { it.groupId == id }.forEach {
            cancelSnoozeNotification(context, it.id)
            schedule(context, it)
        }
    }

    /** Elimina el grupo; sus alarmas se borran o pasan a "sin grupo". */
    fun deleteGroup(context: Context, id: Int, deleteAlarms: Boolean) {
        val members = Stores.alarms.value.filter { it.groupId == id }
        if (deleteAlarms) {
            members.forEach { delete(context, it.id) }
        } else {
            Stores.alarms.update { list -> list.map { if (it.groupId == id) it.copy(groupId = null) else it } }
        }
        Stores.groups.update { list -> list.filterNot { it.id == id } }
        // Sin el grupo apagado, las que quedan pueden volver a sonar.
        if (!deleteAlarms) members.forEach { get(it.id)?.let { a -> schedule(context, a) } }
    }

    fun moveToGroup(context: Context, alarmId: Int, groupId: Int?) {
        val alarm = get(alarmId) ?: return
        save(context, alarm.copy(groupId = groupId))
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
        cancelUpcoming(context, alarm.id)
        val trigger = alarm.nextTrigger() ?: return
        scheduleUpcoming(context, alarm, trigger)
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

    // --- Aviso 2 horas antes ---

    private const val UPCOMING_MS = 2 * 60 * 60_000L
    const val EXTRA_AT = "at"

    private fun scheduleUpcoming(context: Context, alarm: Alarm, trigger: Long) {
        if (!Stores.settings.value.upcomingNotice) return
        val now = System.currentTimeMillis()
        // Una alarma pospuesta ya tiene su propia notificación.
        if (alarm.snoozedUntil?.let { it > now } == true) return
        val at = trigger - UPCOMING_MS
        if (at <= now) {
            showUpcoming(context, alarm.id, trigger)
        } else {
            context.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, upcomingPendingIntent(context, alarm.id, trigger))
        }
    }

    /** Muestra el aviso si la alarma sigue programada para [trigger]. */
    fun showUpcoming(context: Context, id: Int, trigger: Long) {
        val alarm = get(id) ?: return
        if (alarm.nextTrigger() != trigger) return
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val skip = PendingIntent.getBroadcast(
            context, Notif.upcomingId(id),
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_SKIP_UPCOMING)
                .putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val time = formatInstantClock(trigger, is24h(context))
        val day = relativeDay(Instant.ofEpochMilli(trigger).atZone(ZoneId.systemDefault()))
        val n = Notif.builder(context, Notif.CH_UPCOMING)
            .setContentTitle("Próxima alarma: $time" + if (alarm.label.isNotBlank()) " · ${alarm.label}" else "")
            .setContentText(if (alarm.isRepeating) "$day. Descártala para saltar solo esta vez." else "$day.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setWhen(trigger)
            .addAction(0, "Descartar", skip)
            .build()
        try {
            nm.notify(Notif.upcomingId(id), n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones.
        }
    }

    private fun upcomingPendingIntent(context: Context, id: Int, trigger: Long = 0): PendingIntent =
        PendingIntent.getBroadcast(
            context, Notif.upcomingId(id),
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_UPCOMING)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_AT, trigger),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun cancelUpcoming(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(upcomingPendingIntent(context, id))
        NotificationManagerCompat.from(context).cancel(Notif.upcomingId(id))
    }

    private fun cancel(context: Context, id: Int) {
        cancelUpcoming(context, id)
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
        val n = Notif.builder(context, Notif.CH_SNOOZE)
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
