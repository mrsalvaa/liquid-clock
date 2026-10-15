package dev.glassclock

import android.app.Application
import dev.glassclock.data.Stores
import dev.glassclock.util.Notif

class GlassClockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Stores.init(this)
        Notif.createChannels(this)
    }
}
