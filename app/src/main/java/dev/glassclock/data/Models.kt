package dev.glassclock.data

import kotlinx.serialization.Serializable

@Serializable
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    /** Días de la semana (DayOfWeek.value, 1 = lunes). Vacío = una sola vez. */
    val days: Set<Int> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    /** null = sonido de alarma de Ajustes, "" = silencio. */
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val crescendo: Boolean = true,
    val crescendoSeconds: Int = 30,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    /** 0 = ilimitado. */
    val snoozeMaxCount: Int = 3,
    /** Instante (epoch ms) de la ocurrencia que se omitirá. */
    val skippedOccurrence: Long? = null,
    val snoozedUntil: Long? = null,
    val snoozeCount: Int = 0,
)

@Serializable
enum class TimerState { RUNNING, PAUSED, RINGING }

@Serializable
data class TimerItem(
    val id: Int,
    val label: String,
    val durationMs: Long,
    val state: TimerState,
    /** Fin en tiempo de pared (epoch ms), válido si RUNNING. */
    val endAt: Long = 0,
    /** Tiempo restante, válido si PAUSED. */
    val remainingMs: Long = durationMs,
) {
    fun remaining(now: Long): Long = when (state) {
        TimerState.RUNNING -> (endAt - now).coerceAtLeast(0)
        TimerState.PAUSED -> remainingMs
        TimerState.RINGING -> 0
    }
}

@Serializable
data class TimerPreset(val id: Int, val label: String, val durationMs: Long)

@Serializable
data class StopwatchData(
    val running: Boolean = false,
    /** SystemClock.elapsedRealtime() del último inicio. */
    val startRealtime: Long = 0,
    val accumulatedMs: Long = 0,
    /** Tiempo total en cada vuelta. */
    val laps: List<Long> = emptyList(),
) {
    fun elapsed(nowRealtime: Long): Long =
        accumulatedMs + if (running) nowRealtime - startRealtime else 0
}

@Serializable
data class AppSettings(
    val volumeKeysSnooze: Boolean = true,
    val autoSilenceMinutes: Int = 10,
    val animatedBackground: Boolean = true,
    /** Índice en COLOR_SETS. */
    val colorSet: Int = 0,
    /** Sonido de las alarmas que usan el predeterminado. null = el del sistema. */
    val alarmRingtoneUri: String? = null,
    val timerRingtoneUri: String? = null,
    val bedtimeEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val bedtimeDays: Set<Int> = (1..7).toSet(),
)
