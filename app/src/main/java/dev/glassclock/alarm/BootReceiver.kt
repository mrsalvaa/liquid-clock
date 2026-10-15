package dev.glassclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.glassclock.bedtime.Bedtime
import dev.glassclock.timer.Timers

/** Reprograma todo tras reiniciar, actualizar la app o cambiar la hora del sistema. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarms.rescheduleAll(context)
        Timers.rescheduleAll(context)
        Bedtime.schedule(context)
    }
}
