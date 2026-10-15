package dev.glassclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.glassclock.ring.RingService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Alarms.EXTRA_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_FIRE -> RingService.fireAlarm(context, id)
            ACTION_DISMISS_SNOOZED -> Alarms.dismiss(context, id)
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.glassclock.action.ALARM_FIRE"
        const val ACTION_DISMISS_SNOOZED = "dev.glassclock.action.ALARM_DISMISS_SNOOZED"
    }
}
