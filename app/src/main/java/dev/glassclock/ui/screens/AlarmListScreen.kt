package dev.glassclock.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.rotate
import dev.glassclock.data.AlarmGroup
import dev.glassclock.ui.glass.LocalPrompt
import dev.glassclock.ui.glass.PromptSpec
import dev.glassclock.ui.glass.SectionLabel
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SkipNext
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
import dev.glassclock.alarm.isRepeating
import dev.glassclock.alarm.nextOccurrence
import dev.glassclock.alarm.nextTrigger
import dev.glassclock.data.Alarm
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSwitch
import dev.glassclock.ui.glass.LocalActionSheet
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.SheetAction
import dev.glassclock.ui.glass.SheetSpec
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
    onEdit: (Int) -> Unit,
    onNew: (groupId: Int?) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val alarms by Stores.alarms.state.collectAsStateWithLifecycle()
    val groups by Stores.groups.state.collectAsStateWithLifecycle()
    val now by rememberNow(30_000)
    val is24 = is24h(context)
    val sorted = remember(alarms) { alarms.sortedBy { it.hour * 60 + it.minute } }
    // nextTrigger consulta el estado de los grupos: hay que recalcular también cuando cambian.
    val next = remember(alarms, groups, now) { alarms.mapNotNull { it.nextTrigger(now) }.minOrNull() }
    val showSheet = LocalActionSheet.current
    val showPrompt = LocalPrompt.current
    val newGroup = {
        showPrompt(
            PromptSpec(title = "Nuevo grupo", confirmLabel = "Crear", placeholder = "Nombre del grupo") { name ->
                Alarms.createGroup(name.ifBlank { "Grupo ${groups.size + 1}" })
            },
        )
    }
    val alarmCard: LazyListScope.(Alarm, Boolean) -> Unit = { alarm, nested ->
        item(key = "a${alarm.id}") {
            AlarmCard(
                alarm = alarm,
                active = alarm.enabled && groups.firstOrNull { it.id == alarm.groupId }?.enabled != false,
                now = now,
                is24 = is24,
                modifier = if (nested) Modifier.padding(start = 16.dp) else Modifier,
                onClick = { onEdit(alarm.id) },
                onLongClick = {
                    showSheet(alarmMenu(context, alarm, now, is24, showSheet, showPrompt) { onEdit(alarm.id) })
                },
                onToggle = { Alarms.setEnabled(context, alarm.id, it) },
            )
        }
    }

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
                    GlassIconButton(Icons.Rounded.CreateNewFolder, "Nuevo grupo", newGroup)
                    GlassIconButton(Icons.Rounded.Add, "Nueva alarma", { onNew(null) }, prominent = true)
                }
            }
            item { PermissionBanner() }
            if (sorted.isEmpty() && groups.isEmpty()) {
                item {
                    GlassCard(onClick = { onNew(null) }) {
                        Text("Sin alarmas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Toca aquí o el botón + para crear la primera.",
                            color = LocalGlass.current.contentSecondary,
                        )
                    }
                }
            }
            groups.forEach { group ->
                val members = sorted.filter { it.groupId == group.id }
                item(key = "g${group.id}") {
                    GroupCard(
                        group = group,
                        members = members,
                        now = now,
                        is24 = is24,
                        onLongClick = {
                            showSheet(groupMenu(context, group, members.size, showPrompt) { onNew(group.id) })
                        },
                    )
                }
                if (!group.collapsed) members.forEach { alarmCard(it, true) }
            }
            val loose = sorted.filter { a -> a.groupId == null || groups.none { it.id == a.groupId } }
            if (groups.isNotEmpty() && loose.isNotEmpty()) {
                item(key = "loose") { SectionLabel("Sin grupo") }
            }
            loose.forEach { alarmCard(it, false) }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    active: Boolean,
    now: Long,
    is24: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalGlass.current
    val dim = if (active) 1f else 0.45f
    val (time, suffix) = formatClock(alarm.hour, alarm.minute, is24)
    val status = alarmStatus(alarm, active, now, is24)
    GlassCard(modifier, onClick = onClick, onLongClick = onLongClick) {
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

private fun alarmMenu(
    context: Context,
    alarm: Alarm,
    now: Long,
    is24: Boolean,
    showSheet: (SheetSpec) -> Unit,
    showPrompt: (PromptSpec) -> Unit,
    onEdit: () -> Unit,
): SheetSpec {
    val (time, suffix) = formatClock(alarm.hour, alarm.minute, is24)
    val skipped = alarm.skippedOccurrence?.let { it > now } == true
    return SheetSpec(
        title = listOfNotNull(time, suffix).joinToString(" ") +
            if (alarm.label.isNotBlank()) " · ${alarm.label}" else "",
        subtitle = formatDays(alarm.days) ?: "Una sola vez",
        actions = buildList {
            add(SheetAction("Editar", Icons.Rounded.Edit, onClick = onEdit))
            add(
                SheetAction(
                    if (alarm.enabled) "Desactivar" else "Activar",
                    if (alarm.enabled) Icons.Rounded.AlarmOff else Icons.Rounded.AlarmOn,
                ) { Alarms.setEnabled(context, alarm.id, !alarm.enabled) },
            )
            if (alarm.enabled && alarm.isRepeating) {
                add(
                    SheetAction(
                        if (skipped) "Restaurar la próxima" else "Omitir la próxima",
                        Icons.Rounded.SkipNext,
                    ) { if (skipped) Alarms.unskip(context, alarm.id) else Alarms.skipNext(context, alarm.id) },
                )
            }
            add(
                SheetAction("Mover a grupo", Icons.AutoMirrored.Rounded.DriveFileMove) {
                    showSheet(moveMenu(context, alarm, showPrompt))
                },
            )
            add(SheetAction("Duplicar", Icons.Rounded.ContentCopy) { Alarms.duplicate(context, alarm.id) })
            add(SheetAction("Eliminar", Icons.Rounded.Delete, danger = true) { Alarms.delete(context, alarm.id) })
        },
    )
}

private fun moveMenu(context: Context, alarm: Alarm, showPrompt: (PromptSpec) -> Unit): SheetSpec =
    SheetSpec(
        title = "Mover a grupo",
        actions = buildList {
            add(
                SheetAction(
                    "Sin grupo",
                    if (alarm.groupId == null) Icons.Rounded.Check else Icons.Rounded.FolderOff,
                ) { Alarms.moveToGroup(context, alarm.id, null) },
            )
            Stores.groups.value.forEach { g ->
                add(
                    SheetAction(g.name, if (alarm.groupId == g.id) Icons.Rounded.Check else Icons.Rounded.Folder) {
                        Alarms.moveToGroup(context, alarm.id, g.id)
                    },
                )
            }
            add(
                SheetAction("Nuevo grupo", Icons.Rounded.CreateNewFolder) {
                    showPrompt(
                        PromptSpec(title = "Nuevo grupo", confirmLabel = "Crear y mover", placeholder = "Nombre del grupo") {
                            val id = Alarms.createGroup(it.ifBlank { "Grupo ${Stores.groups.value.size + 1}" })
                            Alarms.moveToGroup(context, alarm.id, id)
                        },
                    )
                },
            )
        },
    )

private fun groupMenu(
    context: Context,
    group: AlarmGroup,
    count: Int,
    showPrompt: (PromptSpec) -> Unit,
    onNewInGroup: () -> Unit,
): SheetSpec = SheetSpec(
    title = group.name,
    subtitle = alarmCount(count),
    actions = buildList {
        add(SheetAction("Nueva alarma en el grupo", Icons.Rounded.Add, onClick = onNewInGroup))
        add(
            SheetAction(
                if (group.enabled) "Desactivar grupo" else "Activar grupo",
                if (group.enabled) Icons.Rounded.AlarmOff else Icons.Rounded.AlarmOn,
            ) { Alarms.setGroupEnabled(context, group.id, !group.enabled) },
        )
        add(
            SheetAction("Renombrar", Icons.Rounded.Edit) {
                showPrompt(
                    PromptSpec(
                        title = "Renombrar grupo",
                        subtitle = group.name,
                        confirmLabel = "Guardar",
                        placeholder = "Nombre del grupo",
                    ) { if (it.isNotBlank()) Alarms.renameGroup(group.id, it) },
                )
            },
        )
        if (count > 0) {
            add(SheetAction("Eliminar grupo y conservar alarmas", Icons.Rounded.FolderOff) {
                Alarms.deleteGroup(context, group.id, deleteAlarms = false)
            })
            add(SheetAction("Eliminar grupo y sus alarmas", Icons.Rounded.Delete, danger = true) {
                Alarms.deleteGroup(context, group.id, deleteAlarms = true)
            })
        } else {
            add(SheetAction("Eliminar grupo", Icons.Rounded.Delete, danger = true) {
                Alarms.deleteGroup(context, group.id, deleteAlarms = false)
            })
        }
    },
)

private fun alarmCount(n: Int) = when (n) {
    0 -> "Sin alarmas"
    1 -> "1 alarma"
    else -> "$n alarmas"
}

/** Cabecera de un grupo: tocar pliega o despliega, el interruptor apaga todas sus alarmas. */
@Composable
private fun GroupCard(
    group: AlarmGroup,
    members: List<Alarm>,
    now: Long,
    is24: Boolean,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val rotation by animateFloatAsState(if (group.collapsed) -90f else 0f, label = "chevron")
    val next = members.mapNotNull { it.nextTrigger(now) }.minOrNull()
    val summary = when {
        !group.enabled -> alarmCount(members.size) + " · desactivado"
        next != null -> alarmCount(members.size) + " · próxima " +
            relativeDay(Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault())).lowercase() +
            " a las " + formatInstantClock(next, is24)
        else -> alarmCount(members.size)
    }
    GlassCard(
        onClick = { Alarms.setGroupCollapsed(group.id, !group.collapsed) },
        onLongClick = onLongClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ExpandMore,
                if (group.collapsed) "Desplegar" else "Plegar",
                Modifier.size(28.dp).rotate(rotation),
                tint = p.contentSecondary,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.content.copy(alpha = if (group.enabled) 1f else 0.5f),
                )
                Text(summary, fontSize = 13.sp, color = p.contentSecondary)
            }
            GlassSwitch(group.enabled, { Alarms.setGroupEnabled(context, group.id, it) })
        }
    }
}

/** Texto bajo la hora y si debe resaltarse. */
private fun alarmStatus(alarm: Alarm, active: Boolean, now: Long, is24: Boolean): Pair<String, Boolean> {
    if (alarm.enabled && !active) return "Grupo desactivado" to false
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
    if (days != null) return days to false
    return relativeDay(alarm.nextOccurrence(now)) to false
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
