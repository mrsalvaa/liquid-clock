package dev.glassclock.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.glassclock.MainActivity
import dev.glassclock.R
import dev.glassclock.data.Stores
import dev.glassclock.data.TimerItem
import dev.glassclock.data.TimerPreset
import dev.glassclock.data.TimerState
import dev.glassclock.ring.RingService
import dev.glassclock.util.Notif
import dev.glassclock.util.formatTimer
import dev.glassclock.util.setExactCompat

object Timers {
    const val EXTRA_ID = "timer_id"

    fun get(id: Int): TimerItem? = Stores.timers.value.firstOrNull { it.id == id }

    fun start(context: Context, durationMs: Long, label: String) {
        val id = (Stores.timers.value.maxOfOrNull { it.id } ?: 0) + 1
        val item = TimerItem(
            id = id,
            label = label,
            durationMs = durationMs,
            state = TimerState.RUNNING,
            endAt = System.currentTimeMillis() + durationMs,
        )
        Stores.timers.update { it + item }
        arm(context, item)
    }

    fun pause(context: Context, id: Int) {
        val item = get(id)?.takeIf { it.state == TimerState.RUNNING } ?: return
        val paused = item.copy(state = TimerState.PAUSED, remainingMs = item.remaining(System.currentTimeMillis()))
        replace(paused)
        disarm(context, id)
        notify(context, paused)
    }

    fun resume(context: Context, id: Int) {
        val item = get(id)?.takeIf { it.state == TimerState.PAUSED } ?: return
        val running = item.copy(state = TimerState.RUNNING, endAt = System.currentTimeMillis() + item.remainingMs)
        replace(running)
        arm(context, running)
    }

    fun addTime(context: Context, id: Int, ms: Long) {
        val item = get(id) ?: return
        val updated = when (item.state) {
            TimerState.RUNNING -> item.copy(endAt = item.endAt + ms, durationMs = item.durationMs + ms)
            TimerState.PAUSED -> item.copy(remainingMs = item.remainingMs + ms, durationMs = item.durationMs + ms)
            TimerState.RINGING -> item.copy(
                state = TimerState.RUNNING,
                endAt = System.currentTimeMillis() + ms,
                durationMs = ms,
            )
        }
        replace(updated)
        if (updated.state == TimerState.RUNNING) arm(context, updated) else notify(context, updated)
    }

    fun remove(context: Context, id: Int) {
        Stores.timers.update { list -> list.filterNot { it.id == id } }
        disarm(context, id)
        NotificationManagerCompat.from(context).cancel(Notif.timerId(id))
    }

    /** El temporizador llegó a cero. */
    fun onFire(context: Context, id: Int) {
        val item = get(id) ?: return
        replace(item.copy(state = TimerState.RINGING))
        NotificationManagerCompat.from(context).cancel(Notif.timerId(id))
        RingService.fireTimer(context, id)
    }

    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        Stores.timers.value.forEach { item ->
            when {
                item.state == TimerState.RUNNING && item.endAt <= now -> onFire(context, item.id)
                item.state == TimerState.RUNNING -> arm(context, item)
                item.state == TimerState.PAUSED -> notify(context, item)
                // Uno que sonaba cuando murió el proceso ya no tiene servicio: lo quitamos.
                else -> remove(context, item.id)
            }
        }
    }

    fun savePreset(label: String, durationMs: Long) {
        Stores.presets.update { list ->
            val id = (list.maxOfOrNull { it.id } ?: 0) + 1
            list + TimerPreset(id, label, durationMs)
        }
    }

    fun deletePreset(id: Int) {
        Stores.presets.update { list -> list.filterNot { it.id == id } }
    }

    private fun replace(item: TimerItem) {
        Stores.timers.update { list -> list.map { if (it.id == item.id) item else it } }
    }

    private fun arm(context: Context, item: TimerItem) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setExactCompat(item.endAt, receiverIntent(context, TimerReceiver.ACTION_FIRE, item.id))
        notify(context, item)
    }

    private fun disarm(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(receiverIntent(context, TimerReceiver.ACTION_FIRE, id))
    }

    private fun receiverIntent(context: Context, action: String, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, TimerReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notify(context: Context, item: TimerItem) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, "timer"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = item.label.ifBlank { "Temporizador" }
        val builder = Notif.builder(context, Notif.CH_TIMER)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(open)
        if (item.state == TimerState.RUNNING) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(item.endAt)
                .setShowWhen(true)
                .setContentText("En curso")
                .addAction(0, "Pausar", receiverIntent(context, TimerReceiver.ACTION_PAUSE, item.id))
        } else {
            builder
                .setContentText("En pausa · quedan ${formatTimer(item.remainingMs)}")
                .setShowWhen(false)
                .addAction(0, "Reanudar", receiverIntent(context, TimerReceiver.ACTION_RESUME, item.id))
        }
        builder.addAction(0, "Cancelar", receiverIntent(context, TimerReceiver.ACTION_CANCEL, item.id))
        try {
            nm.notify(Notif.timerId(item.id), builder.build())
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones.
        }
    }
}
