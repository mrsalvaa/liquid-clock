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
            ACTION_UPCOMING -> Alarms.showUpcoming(context, id, intent.getLongExtra(Alarms.EXTRA_AT, 0))
            ACTION_SKIP_UPCOMING -> Alarms.skipNext(context, id)
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.glassclock.action.ALARM_FIRE"
        const val ACTION_DISMISS_SNOOZED = "dev.glassclock.action.ALARM_DISMISS_SNOOZED"
        const val ACTION_UPCOMING = "dev.glassclock.action.ALARM_UPCOMING"
        const val ACTION_SKIP_UPCOMING = "dev.glassclock.action.ALARM_SKIP_UPCOMING"
    }
}
