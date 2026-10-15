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
    /** Grupo al que pertenece, o null. */
    val groupId: Int? = null,
    /** "Activar para mañana": no suena antes de este instante (inicio de mañana). */
    val pausedUntil: Long? = null,
)

/** Grupo de alarmas: su interruptor apaga todas sin tocar el interruptor de cada una. */
@Serializable
data class AlarmGroup(
    val id: Int,
    val name: String,
    val enabled: Boolean = true,
    val collapsed: Boolean = false,
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
    /** 0 = según el sistema, 1 = claro, 2 = oscuro. */
    val themeMode: Int = 0,
    /** 0 = colores en movimiento (COLOR_SETS), 1 = color de énfasis. */
    val backgroundMode: Int = 0,
    /** Color de énfasis en ARGB, para backgroundMode = 1. */
    val accentColor: Long = 0xFF0EA5E9,
    /** Índice del icono de la app (ver AppIcons). */
    val appIcon: Int = 0,
    /** Notificación 2 h antes de cada alarma para poder descartarla. */
    val upcomingNotice: Boolean = true,
    /** Formato de fecha: 0 = texto ("15 de octubre"), 1 = dd/mm/aaaa, 2 = mm/dd/aaaa. */
    val dateFormat: Int = 0,
    /** Sonido de las alarmas que usan el predeterminado. null = el del sistema. */
    val alarmRingtoneUri: String? = null,
    val timerRingtoneUri: String? = null,
    val bedtimeEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val bedtimeDays: Set<Int> = (1..7).toSet(),
)
