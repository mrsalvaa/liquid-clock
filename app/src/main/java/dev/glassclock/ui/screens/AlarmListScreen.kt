package dev.glassclock.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.alarm.Alarms
import dev.glassclock.alarm.nextOccurrence
import dev.glassclock.alarm.nextTrigger
import dev.glassclock.data.Alarm
import dev.glassclock.data.DismissTask
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSwitch
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.ScreenTitle
import dev.glassclock.util.formatClock
import dev.glassclock.util.formatDays
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.formatUntil
import dev.glassclock.util.is24h
import dev.glassclock.util.relativeDay
import java.time.Instant
import java.time.ZoneId

@Composable
fun AlarmListScreen(
    contentPadding: PaddingValues,
    onEdit: (Int?) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val alarms by Stores.alarms.state.collectAsStateWithLifecycle()
    val now by rememberNow(30_000)
    val is24 = is24h(context)
    val sorted = remember(alarms) { alarms.sortedBy { it.hour * 60 + it.minute } }
    val next = alarms.mapNotNull { it.nextTrigger(now) }.minOrNull()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            item {
                ScreenTitle(
                    "Alarmas",
                    subtitle = next?.let {
                        "Próxima en ${formatUntil(it - now)} · " +
                            relativeDay(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())).lowercase() +
                            " a las ${formatInstantClock(it, is24)}"
                    } ?: "Ninguna alarma activa",
                ) {
                    GlassIconButton(Icons.Rounded.Settings, "Ajustes", onSettings)
                    GlassIconButton(Icons.Rounded.Add, "Nueva alarma", { onEdit(null) }, prominent = true)
                }
            }
            item { PermissionBanner() }
            if (sorted.isEmpty()) {
                item {
                    GlassCard(onClick = { onEdit(null) }) {
                        Text("Sin alarmas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Toca aquí o el botón + para crear la primera.",
                            color = LocalGlass.current.contentSecondary,
                        )
                    }
                }
            }
            items(sorted, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    now = now,
                    is24 = is24,
                    onClick = { onEdit(alarm.id) },
                    onToggle = { Alarms.setEnabled(context, alarm.id, it) },
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    now: Long,
    is24: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val p = LocalGlass.current
    val dim = if (alarm.enabled) 1f else 0.45f
    val (time, suffix) = formatClock(alarm.hour, alarm.minute, is24)
    val status = alarmStatus(alarm, now, is24)
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (alarm.label.isNotBlank()) {
                    Text(alarm.label, fontSize = 14.sp, color = p.contentSecondary.copy(alpha = dim))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        time,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        color = p.content.copy(alpha = dim),
                    )
                    if (suffix != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            suffix,
                            fontSize = 16.sp,
                            color = p.content.copy(alpha = dim),
                            modifier = Modifier.padding(bottom = 9.dp),
                        )
                    }
                }
                Text(
                    status.first,
                    fontSize = 13.sp,
                    color = if (status.second) p.accent else p.contentSecondary.copy(alpha = dim),
                )
            }
            GlassSwitch(alarm.enabled, onToggle)
        }
    }
}

/** Texto bajo la hora y si debe resaltarse. */
private fun alarmStatus(alarm: Alarm, now: Long, is24: Boolean): Pair<String, Boolean> {
    val extras = buildList {
        if (alarm.task != DismissTask.NONE) {
            add(
                when (alarm.task) {
                    DismissTask.MATH -> "cálculo"
                    DismissTask.SHAKE -> "agitar"
                    DismissTask.TYPE -> "escribir"
                    DismissTask.NONE -> ""
                },
            )
        }
    }
    val suffix = if (extras.isEmpty()) "" else " · " + extras.joinToString(", ")
    if (alarm.enabled) {
        alarm.snoozedUntil?.takeIf { it > now }?.let {
            return "Pospuesta hasta las ${formatInstantClock(it, is24)}" to true
        }
        val skipped = alarm.skippedOccurrence?.takeIf { it > now }
        if (skipped != null) {
            val next = alarm.nextOccurrence(now)
            return "Omitida la próxima · siguiente: ${relativeDay(next).lowercase()}" to true
        }
    }
    val days = formatDays(alarm.days)
    if (days != null) return days + suffix to false
    return relativeDay(alarm.nextOccurrence(now)) + suffix to false
}

/** Avisos si falta algún permiso imprescindible para que las alarmas suenen. */
@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val missing = remember(tick) { missingPermissions(context) }
    if (missing.isEmpty()) return
    val p = LocalGlass.current
    GlassCard {
        Text("Faltan permisos", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = p.danger)
        Text(
            "Sin ellos las alarmas podrían no sonar o no aparecer en pantalla completa.",
            fontSize = 14.sp,
            color = p.contentSecondary,
        )
        Spacer(Modifier.height(10.dp))
        missing.forEach { (label, intent) ->
            GlassButton(
                label,
                onClick = { context.startActivity(intent) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
    }
}

fun missingPermissions(context: Context): List<Pair<String, Intent>> {
    val pkg = Uri.parse("package:${context.packageName}")
    return buildList {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            add(
                "Permitir notificaciones" to Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
        if (!context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            add("Permitir alarmas exactas" to Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
        }
        if (Build.VERSION.SDK_INT >= 34 &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            add("Permitir pantalla completa" to Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
        }
    }
}

/** Para el texto de "Sonará en..." tras guardar. */
fun Alarm.untilText(now: Long = System.currentTimeMillis()): String {
    val t = nextTrigger(now) ?: return "Alarma desactivada"
    return "Sonará en ${formatUntil(t - now)}"
}
