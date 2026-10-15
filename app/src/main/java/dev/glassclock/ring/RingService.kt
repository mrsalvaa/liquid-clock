package dev.glassclock.ring

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.glassclock.R
import dev.glassclock.alarm.Alarms
import dev.glassclock.alarm.isActive
import dev.glassclock.data.Stores
import dev.glassclock.timer.Timers
import dev.glassclock.util.Notif
import dev.glassclock.util.formatDurationShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lo que está sonando ahora, observado por RingActivity. */
data class RingState(
    val kind: Kind,
    val id: Int,
    val label: String,
    val snoozeAllowed: Boolean,
    val snoozeDefault: Int,
    /** null = ilimitado. */
    val snoozesLeft: Int?,
) {
    enum class Kind { ALARM, TIMER }
}

class RingService : Service() {

    companion object {
        private const val ACTION_ALARM = "dev.glassclock.ring.ALARM"
        private const val ACTION_TIMER = "dev.glassclock.ring.TIMER"
        const val ACTION_SNOOZE = "dev.glassclock.ring.SNOOZE"
        const val ACTION_DISMISS = "dev.glassclock.ring.DISMISS"
        const val ACTION_DUCK = "dev.glassclock.ring.DUCK"
        const val ACTION_TIMER_ADD = "dev.glassclock.ring.TIMER_ADD"
        private const val EXTRA_ID = "id"
        private const val EXTRA_MINUTES = "minutes"

        private val _current = MutableStateFlow<RingState?>(null)
        val current: StateFlow<RingState?> = _current.asStateFlow()

        private fun intent(context: Context, action: String, id: Int = -1, minutes: Int = 0) =
            Intent(context, RingService::class.java)
                .setAction(action)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_MINUTES, minutes)

        fun fireAlarm(context: Context, id: Int) {
            context.startForegroundService(intent(context, ACTION_ALARM, id))
        }

        fun fireTimer(context: Context, id: Int) {
            context.startForegroundService(intent(context, ACTION_TIMER, id))
        }

        /** Acciones sobre lo que ya está sonando (posponer, descartar, etc.). */
        fun send(context: Context, action: String, minutes: Int = 0) {
            if (_current.value == null) return
            context.startService(intent(context, action, minutes = minutes))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var crescendoJob: Job? = null
    private var autoSilenceJob: Job? = null
    private var duckJob: Job? = null
    private var level = 1f
    private var ducked = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0
        when (intent?.action) {
            ACTION_ALARM -> startAlarm(id)
            ACTION_TIMER -> startTimer(id)
            ACTION_SNOOZE -> _current.value?.let { snooze(it, minutes.takeIf { m -> m > 0 } ?: it.snoozeDefault) }
            ACTION_DISMISS -> _current.value?.let { dismiss(it) }
            ACTION_DUCK -> duck()
            ACTION_TIMER_ADD -> _current.value?.let { addMinute(it) }
        }
        if (_current.value == null) finish()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopFeedback()
        scope.cancel()
        _current.value = null
        super.onDestroy()
    }

    private fun startAlarm(id: Int) {
        // Hay que pasar a primer plano sí o sí tras startForegroundService.
        if (_current.value == null) enterForeground(basicNotification())
        val alarm = Alarms.get(id)?.takeIf { it.isActive } ?: return
        _current.value?.let { replacePrevious(it) }
        Alarms.onFired(this, id)
        val left = if (alarm.snoozeMaxCount == 0) null else alarm.snoozeMaxCount - alarm.snoozeCount
        val state = RingState(
            kind = RingState.Kind.ALARM,
            id = id,
            label = alarm.label.ifBlank { "Alarma" },
            snoozeAllowed = alarm.snoozeEnabled && (left == null || left > 0),
            snoozeDefault = alarm.snoozeMinutes,
            snoozesLeft = left,
        )
        val sound = alarm.ringtoneUri ?: Stores.settings.value.alarmRingtoneUri
        begin(state, sound, alarm.crescendo, alarm.crescendoSeconds, alarm.vibrate)
    }

    private fun startTimer(id: Int) {
        if (_current.value == null) enterForeground(basicNotification())
        val timer = Timers.get(id) ?: return
        _current.value?.let { replacePrevious(it) }
        val state = RingState(
            kind = RingState.Kind.TIMER,
            id = id,
            label = timer.label.ifBlank { "Temporizador" } + " · " + formatDurationShort(timer.durationMs),
            snoozeAllowed = false,
            snoozeDefault = 1,
            snoozesLeft = null,
        )
        begin(state, Stores.settings.value.timerRingtoneUri, crescendo = false, crescendoSeconds = 0, vibrate = true)
    }

    private fun begin(state: RingState, uri: String?, crescendo: Boolean, crescendoSeconds: Int, vibrate: Boolean) {
        _current.value = state
        enterForeground(ringNotification(state))
        acquireWakeLock()
        showRingScreen()
        startSound(uri, crescendo, crescendoSeconds)
        if (vibrate) startVibration()
        val minutes = Stores.settings.value.autoSilenceMinutes
        autoSilenceJob?.cancel()
        autoSilenceJob = scope.launch {
            delay(minutes * 60_000L)
            val s = _current.value ?: return@launch
            if (s.snoozeAllowed) snooze(s, s.snoozeDefault) else dismiss(s)
        }
    }

    /** Si suena algo nuevo mientras sonaba otra cosa, la anterior se pospone o se descarta. */
    private fun replacePrevious(prev: RingState) {
        stopFeedback()
        when (prev.kind) {
            RingState.Kind.ALARM ->
                if (prev.snoozeAllowed) Alarms.snooze(this, prev.id, prev.snoozeDefault) else Alarms.dismiss(this, prev.id)
            RingState.Kind.TIMER -> Timers.remove(this, prev.id)
        }
        _current.value = null
    }

    private fun snooze(state: RingState, minutes: Int) {
        if (state.kind == RingState.Kind.ALARM && state.snoozeAllowed) {
            Alarms.snooze(this, state.id, minutes)
            end()
        }
    }

    private fun dismiss(state: RingState) {
        when (state.kind) {
            RingState.Kind.ALARM -> Alarms.dismiss(this, state.id)
            RingState.Kind.TIMER -> Timers.remove(this, state.id)
        }
        end()
    }

    private fun addMinute(state: RingState) {
        if (state.kind != RingState.Kind.TIMER) return
        Timers.addTime(this, state.id, 60_000)
        end()
    }

    private fun end() {
        _current.value = null
        finish()
    }

    private fun finish() {
        stopFeedback()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Sonido y vibración ---

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun startSound(uriString: String?, crescendo: Boolean, crescendoSeconds: Int) {
        if (uriString == "") return // Silencio.
        val candidates = listOfNotNull(
            uriString?.let(Uri::parse),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        )
        player = candidates.firstNotNullOfOrNull { createPlayer(it) } ?: return
        level = if (crescendo) 0.05f else 1f
        ducked = false
        applyVolume()
        player?.start()
        if (crescendo && crescendoSeconds > 0) {
            crescendoJob = scope.launch {
                val steps = crescendoSeconds * 10
                for (i in 1..steps) {
                    delay(100)
                    level = 0.05f + 0.95f * i / steps
                    applyVolume()
                }
            }
        }
    }

    private fun createPlayer(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setDataSource(this@RingService, uri)
            isLooping = true
            prepare()
        }
    } catch (e: Exception) {
        null
    }

    private fun applyVolume() {
        val v = if (ducked) minOf(level, 0.15f) else level
        player?.setVolume(v, v)
    }

    /** Baja el volumen un rato mientras el usuario resuelve la tarea. */
    private fun duck() {
        if (player == null) return
        ducked = true
        applyVolume()
        duckJob?.cancel()
        duckJob = scope.launch {
            delay(20_000)
            ducked = false
            applyVolume()
        }
    }

    private fun startVibration() {
        val vib = getSystemService(VibratorManager::class.java).defaultVibrator
        vibrator = vib
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            vib.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(effect, audioAttributes)
        }
    }

    private fun stopFeedback() {
        crescendoJob?.cancel()
        autoSilenceJob?.cancel()
        duckJob?.cancel()
        player?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GlassClock:ring")
            .apply { acquire(30 * 60_000L) }
    }

    // --- Pantalla y notificaciones ---

    private fun ringActivityIntent() = Intent(this, RingActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

    /**
     * La notificación a pantalla completa abre la alarma con la pantalla apagada o bloqueada.
     * Si el sistema no concede ese permiso, intentamos abrirla directamente (funciona si la app
     * tiene "Mostrar sobre otras apps").
     */
    private fun showRingScreen() {
        val nm = getSystemService(NotificationManager::class.java)
        val fsiAllowed = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
        if (fsiAllowed && !Settings.canDrawOverlays(this)) return
        try {
            startActivity(ringActivityIntent())
        } catch (e: Exception) {
            // Bloqueado por las restricciones de inicio en segundo plano.
        }
    }

    private fun enterForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this, Notif.ID_RING, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun basicNotification(): Notification =
        NotificationCompat.Builder(this, Notif.CH_RING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

    private fun ringNotification(state: RingState): Notification {
        val full = PendingIntent.getActivity(
            this, 0, ringActivityIntent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(action: String, code: Int, minutes: Int = 0) = PendingIntent.getService(
            this, code, intent(this, action, minutes = minutes),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, Notif.CH_RING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state.label)
            .setContentText(if (state.kind == RingState.Kind.ALARM) "Sonando" else "Tiempo cumplido")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
        when (state.kind) {
            RingState.Kind.ALARM -> {
                if (state.snoozeAllowed) {
                    builder.addAction(0, "Posponer ${state.snoozeDefault} min", action(ACTION_SNOOZE, 1, state.snoozeDefault))
                }
                builder.addAction(0, "Descartar", action(ACTION_DISMISS, 2))
            }
            RingState.Kind.TIMER -> {
                builder.addAction(0, "+1 min", action(ACTION_TIMER_ADD, 3))
                builder.addAction(0, "Detener", action(ACTION_DISMISS, 2))
            }
        }
        return builder.build()
    }
}
