package dev.glassclock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

val SPANISH: Locale = Locale.forLanguageTag("es")

/** Letras de los días, lunes primero, con su DayOfWeek.value. */
val DAY_LETTERS = listOf(1 to "L", 2 to "M", 3 to "X", 4 to "J", 5 to "V", 6 to "S", 7 to "D")

/** Próximo instante con esa hora en alguno de los días indicados (vacío = cualquiera). */
fun nextOccurrenceOf(
    hour: Int,
    minute: Int,
    days: Set<Int>,
    from: ZonedDateTime = ZonedDateTime.now(),
): ZonedDateTime {
    var candidate = from.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!candidate.isAfter(from)) candidate = candidate.plusDays(1)
    if (days.isNotEmpty()) {
        while (candidate.dayOfWeek.value !in days) candidate = candidate.plusDays(1)
    }
    return candidate
}

fun is24h(context: Context) = DateFormat.is24HourFormat(context)

/** "07:30" o ("7:30", "a. m."). */
fun formatClock(hour: Int, minute: Int, is24: Boolean): Pair<String, String?> =
    if (is24) "%02d:%02d".format(hour, minute) to null
    else {
        val h = if (hour % 12 == 0) 12 else hour % 12
        "%d:%02d".format(h, minute) to if (hour < 12) "a. m." else "p. m."
    }

fun formatInstantClock(epochMs: Long, is24: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
    val t = Instant.ofEpochMilli(epochMs).atZone(zone)
    val (time, suffix) = formatClock(t.hour, t.minute, is24)
    return if (suffix != null) "$time $suffix" else time
}

/** "7 h 32 min", "45 min", "menos de 1 min". */
fun formatUntil(ms: Long): String {
    val totalMin = (ms + 59_999) / 60_000
    if (ms < 60_000) return "menos de 1 min"
    val d = totalMin / (24 * 60)
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return buildList {
        if (d > 0) add("$d d")
        if (h > 0) add("$h h")
        if (m > 0) add("$m min")
    }.joinToString(" ")
}

fun formatDays(days: Set<Int>): String? = when {
    days.isEmpty() -> null
    days.size == 7 -> "Todos los días"
    days == setOf(1, 2, 3, 4, 5) -> "Lunes a viernes"
    days == setOf(6, 7) -> "Fines de semana"
    else -> days.sorted().joinToString(", ") {
        DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, SPANISH).trimEnd('.')
    }
}

/** "Hoy", "Mañana" o "vie, 17 oct". */
fun relativeDay(target: ZonedDateTime, today: LocalDate = LocalDate.now(target.zone)): String {
    val date = target.toLocalDate()
    return when (date) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        today.minusDays(1) -> "Ayer"
        else -> DateTimeFormatter.ofPattern("EEE, d MMM", SPANISH).format(target).replace(".", "")
    }
}

fun formatLongDate(epochMs: Long): String =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SPANISH)
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
        .replaceFirstChar { it.uppercase() }

/** Cronómetro: "01:23.45" o "1:01:23.45". */
fun formatStopwatch(ms: Long): String {
    val cs = (ms / 10) % 100
    val s = (ms / 1000) % 60
    val m = (ms / 60_000) % 60
    val h = ms / 3_600_000
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}

/** Temporizador: redondea hacia arriba los segundos. "05:00" o "1:05:00". */
fun formatTimer(ms: Long): String {
    val total = (ms + 999) / 1000
    val s = total % 60
    val m = (total / 60) % 60
    val h = total / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatDurationShort(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total / 60) % 60
    val s = total % 60
    return buildList {
        if (h > 0) add("$h h")
        if (m > 0) add("$m min")
        if (s > 0) add("$s s")
    }.ifEmpty { listOf("0 s") }.joinToString(" ")
}

/** "+3 h", "−4:30 h", "Misma hora". */
fun formatOffsetDiff(seconds: Int): String {
    if (seconds == 0) return "Misma hora"
    val sign = if (seconds > 0) "+" else "−"
    val a = abs(seconds)
    val h = a / 3600
    val m = (a % 3600) / 60
    return if (m == 0) "$sign$h h" else "$sign$h:%02d h".format(m)
}

/** Alarma exacta que despierta el dispositivo; si no hay permiso, una inexacta. */
fun AlarmManager.setExactCompat(triggerAt: Long, pi: PendingIntent) {
    if (canScheduleExactAlarms()) {
        setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    } else {
        setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
}
