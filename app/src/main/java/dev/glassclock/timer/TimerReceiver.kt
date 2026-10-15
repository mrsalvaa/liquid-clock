package dev.glassclock.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Timers.EXTRA_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_FIRE -> Timers.onFire(context, id)
            ACTION_PAUSE -> Timers.pause(context, id)
            ACTION_RESUME -> Timers.resume(context, id)
            ACTION_CANCEL -> Timers.remove(context, id)
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.glassclock.action.TIMER_FIRE"
        const val ACTION_PAUSE = "dev.glassclock.action.TIMER_PAUSE"
        const val ACTION_RESUME = "dev.glassclock.action.TIMER_RESUME"
        const val ACTION_CANCEL = "dev.glassclock.action.TIMER_CANCEL"
    }
}
