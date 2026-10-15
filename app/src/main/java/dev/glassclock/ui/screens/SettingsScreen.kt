package dev.glassclock.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import dev.glassclock.ui.glass.COLOR_SETS
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.R
import dev.glassclock.bedtime.Bedtime
import dev.glassclock.data.AppSettings
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.DaySelector
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSwitch
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.SectionLabel
import dev.glassclock.ui.glass.SettingRow
import dev.glassclock.ui.glass.WheelPicker
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.formatUntil
import dev.glassclock.util.is24h
import dev.glassclock.util.nextOccurrenceOf

@Composable
fun SettingsScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val s by Stores.settings.state.collectAsStateWithLifecycle()
    val pickAlarmSound = rememberRingtonePicker { uri -> Stores.settings.update { it.copy(alarmRingtoneUri = uri) } }
    val pickTimerSound = rememberRingtonePicker { uri -> Stores.settings.update { it.copy(timerRingtoneUri = uri) } }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    fun edit(block: (AppSettings) -> AppSettings) {
        Stores.settings.update(block)
        Bedtime.schedule(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onClose)
            Text("Ajustes", Modifier.padding(start = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        SectionLabel("Alarmas")
        GlassCard {
            SettingRow(
                "Sonido de alarma",
                subtitle = remember(s.alarmRingtoneUri) { ringtoneTitle(context, s.alarmRingtoneUri) },
                onClick = { pickAlarmSound(s.alarmRingtoneUri) },
            )
            SettingRow("Botones de volumen posponen", subtitle = "Mientras suena una alarma") {
                GlassSwitch(s.volumeKeysSnooze, { v -> edit { it.copy(volumeKeysSnooze = v) } })
            }
            Text("Silenciar automáticamente tras", fontSize = 13.sp, color = p.contentSecondary, modifier = Modifier.padding(top = 8.dp))
            ChipRow(listOf(1, 5, 10, 15, 30), s.autoSilenceMinutes, { "$it min" }) { v ->
                edit { it.copy(autoSilenceMinutes = v) }
            }
        }

        SectionLabel("Temporizador")
        GlassCard {
            SettingRow(
                "Sonido del temporizador",
                subtitle = remember(s.timerRingtoneUri) { ringtoneTitle(context, s.timerRingtoneUri) },
                onClick = { pickTimerSound(s.timerRingtoneUri) },
            )
        }

        SectionLabel("Modo sueño")
        GlassCard {
            SettingRow("Recordatorio para dormir", subtitle = "Te avisa a la hora de acostarte") {
                GlassSwitch(s.bedtimeEnabled, { v -> edit { it.copy(bedtimeEnabled = v) } })
            }
            if (s.bedtimeEnabled) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(24, s.bedtimeHour, { v -> edit { it.copy(bedtimeHour = v) } }, visibleItems = 3)
                    Text(":", fontSize = 36.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    WheelPicker(60, s.bedtimeMinute, { v -> edit { it.copy(bedtimeMinute = v) } }, visibleItems = 3)
                }
                DaySelector(s.bedtimeDays, { v -> edit { it.copy(bedtimeDays = v) } })
                val bed = nextOccurrenceOf(s.bedtimeHour, s.bedtimeMinute, s.bedtimeDays.ifEmpty { (1..7).toSet() })
                    .toInstant().toEpochMilli()
                val wake = Bedtime.nextAlarm(bed)
                Text(
                    if (wake != null) {
                        "Si te acuestas a esa hora dormirás ${formatUntil(wake - bed)} " +
                            "(alarma a las ${formatInstantClock(wake, is24h(context))})."
                    } else {
                        "No hay alarmas activas después de esa hora."
                    },
                    Modifier.padding(top = 12.dp),
                    fontSize = 13.sp,
                    color = p.contentSecondary,
                )
            }
        }

        SectionLabel("Apariencia")
        GlassCard {
            Text("Colores del fondo", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                COLOR_SETS.forEachIndexed { i, set ->
                    val selected = s.colorSet == i
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(remember { MutableInteractionSource() }, null) {
                                edit { it.copy(colorSet = i) }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val blobs = if (p.dark) set.darkBlobs else set.lightBlobs
                        Box(
                            Modifier
                                .size(52.dp)
                                .border(
                                    2.dp,
                                    if (selected) p.content else Color.Transparent,
                                    CircleShape,
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(blobs[0], blobs[1]))),
                        )
                        Text(
                            set.name,
                            Modifier.padding(top = 6.dp),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            color = if (selected) p.content else p.contentSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            SettingRow("Fondo animado", subtitle = "Desactívalo para ahorrar batería") {
                GlassSwitch(s.animatedBackground, { v -> edit { it.copy(animatedBackground = v) } })
            }
        }

        SectionLabel("Permisos")
        val missing = remember(tick) { missingPermissions(context) }
        val pm = context.getSystemService(PowerManager::class.java)
        val ignoringBattery = remember(tick) { pm.isIgnoringBatteryOptimizations(context.packageName) }
        val canOverlay = remember(tick) { Settings.canDrawOverlays(context) }
        GlassCard {
            if (missing.isEmpty()) {
                Text("Notificaciones, alarmas exactas y pantalla completa concedidos.", color = p.success, fontSize = 14.sp)
            }
            missing.forEach { (label, intent) ->
                GlassButton(label, { context.startActivity(intent) }, Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
            SettingRow(
                "Mostrar sobre otras apps",
                subtitle = if (canOverlay) "Activado: la alarma se abre aunque estés usando el móvil"
                else "Opcional: abre la alarma a pantalla completa aunque estés usando el móvil",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                    )
                },
            )
            SettingRow(
                "Optimización de batería",
                subtitle = if (ignoringBattery) "Sin restricciones" else "Restringida: el sistema podría retrasar la app",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )
        }

        SectionLabel("Acerca de")
        GlassCard {
            Text(stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Software libre bajo licencia GPL-3.0. Sin anuncios, sin rastreo y sin conexión a internet.",
                fontSize = 13.sp,
                color = p.contentSecondary,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
