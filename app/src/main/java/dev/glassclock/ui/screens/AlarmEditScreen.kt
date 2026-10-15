package dev.glassclock.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.alarm.Alarms
import dev.glassclock.alarm.isRepeating
import dev.glassclock.alarm.nextOccurrence
import dev.glassclock.data.Alarm
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.DaySelector
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassChip
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSwitch
import dev.glassclock.ui.glass.GlassTextField
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.LocalPrompt
import dev.glassclock.ui.glass.PromptSpec
import dev.glassclock.ui.glass.SectionLabel
import dev.glassclock.ui.glass.SettingRow
import dev.glassclock.ui.glass.WheelPicker
import dev.glassclock.util.formatDays
import dev.glassclock.util.formatUntil
import java.time.LocalTime

@Composable
fun AlarmEditScreen(alarmId: Int?, groupId: Int?, contentPadding: PaddingValues, onClose: () -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val existing = remember(alarmId) { alarmId?.let { Alarms.get(it) } }
    var draft by remember(alarmId) {
        val now = LocalTime.now()
        mutableStateOf(existing ?: Alarm(id = Alarms.newId(), hour = now.hour, minute = now.minute, groupId = groupId))
    }
    val settings by Stores.settings.state.collectAsStateWithLifecycle()
    val groups by Stores.groups.state.collectAsStateWithLifecycle()
    val showPrompt = LocalPrompt.current
    val draftGroup = groups.firstOrNull { it.id == draft.groupId }
    val pickRingtone = rememberRingtonePicker { draft = draft.copy(ringtoneUri = it) }
    val now by rememberNow(1000)

    fun save() {
        val saved = draft.copy(enabled = true, skippedOccurrence = null, snoozedUntil = null, snoozeCount = 0)
        Alarms.save(context, saved)
        Toast.makeText(context, saved.untilText(), Toast.LENGTH_SHORT).show()
        onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.Close, "Cancelar", onClose)
            Text(
                if (existing == null) "Nueva alarma" else "Editar alarma",
                Modifier.weight(1f).padding(horizontal = 12.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            GlassIconButton(Icons.Rounded.Check, "Guardar", ::save, prominent = true)
        }

        // Hora
        GlassCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(24, draft.hour, { draft = draft.copy(hour = it) })
                Text(":", fontSize = 40.sp, modifier = Modifier.padding(horizontal = 6.dp))
                WheelPicker(60, draft.minute, { draft = draft.copy(minute = it) })
            }
            val preview = draft.copy(enabled = true, skippedOccurrence = null, snoozedUntil = null)
            val at = preview.nextOccurrence(now).toInstant().toEpochMilli()
            Text(
                if (draftGroup?.enabled == false) {
                    "El grupo «${draftGroup.name}» está desactivado: no sonará hasta que lo actives"
                } else {
                    "Sonará en " + formatUntil(at - now)
                },
                Modifier.fillMaxWidth().padding(top = 8.dp),
                color = p.contentSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        SectionLabel("Repetir")
        GlassCard {
            DaySelector(draft.days, { draft = draft.copy(days = it) })
            Text(
                formatDays(draft.days) ?: "Una sola vez",
                Modifier.padding(top = 10.dp, start = 4.dp),
                fontSize = 13.sp,
                color = p.contentSecondary,
            )
        }

        SectionLabel("Grupo")
        GlassCard {
            ChipRow(
                options = listOf<Int?>(null) + groups.map { it.id } + NEW_GROUP,
                selected = draft.groupId,
                label = { id ->
                    when (id) {
                        null -> "Sin grupo"
                        NEW_GROUP -> "+ Nuevo"
                        else -> groups.firstOrNull { it.id == id }?.name ?: "?"
                    }
                },
                onSelect = { id ->
                    if (id == NEW_GROUP) {
                        showPrompt(
                            PromptSpec(title = "Nuevo grupo", confirmLabel = "Crear", placeholder = "Nombre del grupo") {
                                val created = Alarms.createGroup(it.ifBlank { "Grupo ${groups.size + 1}" })
                                draft = draft.copy(groupId = created)
                            },
                        )
                    } else {
                        draft = draft.copy(groupId = id)
                    }
                },
            )
        }

        SectionLabel("Nombre")
        GlassTextField(
            draft.label,
            { draft = draft.copy(label = it) },
            "Nombre de la alarma",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        SectionLabel("Sonido y vibración")
        GlassCard {
            SettingRow(
                "Sonido",
                subtitle = remember(draft.ringtoneUri, settings.alarmRingtoneUri) {
                    if (draft.ringtoneUri == null) {
                        "Predeterminado (${ringtoneTitle(context, settings.alarmRingtoneUri)})"
                    } else {
                        ringtoneTitle(context, draft.ringtoneUri)
                    }
                },
                onClick = { pickRingtone(draft.ringtoneUri) },
            )
            SettingRow("Vibración") {
                GlassSwitch(draft.vibrate, { draft = draft.copy(vibrate = it) })
            }
            SettingRow("Volumen creciente", subtitle = "Empieza bajo y sube poco a poco") {
                GlassSwitch(draft.crescendo, { draft = draft.copy(crescendo = it) })
            }
            if (draft.crescendo) {
                ChipRow(
                    options = listOf(15, 30, 60, 120),
                    selected = draft.crescendoSeconds,
                    label = { if (it < 60) "$it s" else "${it / 60} min" },
                    onSelect = { draft = draft.copy(crescendoSeconds = it) },
                )
            }
        }

        SectionLabel("Posponer")
        GlassCard {
            SettingRow("Posponer", subtitle = "Podrás elegir otro tiempo mientras suena") {
                GlassSwitch(draft.snoozeEnabled, { draft = draft.copy(snoozeEnabled = it) })
            }
            if (draft.snoozeEnabled) {
                Text("Intervalo", fontSize = 13.sp, color = p.contentSecondary, modifier = Modifier.padding(top = 6.dp))
                ChipRow(
                    options = listOf(5, 10, 15, 30),
                    selected = draft.snoozeMinutes,
                    label = { "$it min" },
                    onSelect = { draft = draft.copy(snoozeMinutes = it) },
                )
                Text("Repeticiones", fontSize = 13.sp, color = p.contentSecondary, modifier = Modifier.padding(top = 10.dp))
                ChipRow(
                    options = listOf(1, 3, 5, 0),
                    selected = draft.snoozeMaxCount,
                    label = { if (it == 0) "Sin límite" else "$it ${if (it == 1) "vez" else "veces"}" },
                    onSelect = { draft = draft.copy(snoozeMaxCount = it) },
                )
            }
        }

        if (existing != null) {
            Spacer(Modifier.height(20.dp))
            if (existing.enabled && existing.isRepeating) {
                val skipped = existing.skippedOccurrence?.let { it > now } == true
                GlassButton(
                    if (skipped) "Restaurar la próxima alarma" else "Omitir la próxima alarma",
                    onClick = {
                        if (skipped) Alarms.unskip(context, existing.id) else Alarms.skipNext(context, existing.id)
                        onClose()
                    },
                    icon = Icons.Rounded.SkipNext,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }
            GlassButton(
                "Eliminar alarma",
                onClick = {
                    Alarms.delete(context, existing.id)
                    onClose()
                },
                icon = Icons.Rounded.Delete,
                danger = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Opción "+ Nuevo" del selector de grupo. */
private const val NEW_GROUP = -1

@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    Box(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                GlassChip(label(option), option == selected, { onSelect(option) })
            }
        }
    }
}
