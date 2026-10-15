package dev.glassclock.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.data.Stores
import dev.glassclock.data.TimerItem
import dev.glassclock.data.TimerState
import dev.glassclock.timer.Timers
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassChip
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassTextField
import dev.glassclock.ui.glass.LocalActionSheet
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.LocalPrompt
import dev.glassclock.ui.glass.PromptSpec
import dev.glassclock.ui.glass.SheetAction
import dev.glassclock.ui.glass.SheetSpec
import dev.glassclock.ui.glass.ScreenTitle
import dev.glassclock.ui.glass.WheelPicker
import dev.glassclock.util.formatDurationShort
import dev.glassclock.util.formatTimer

@Composable
fun TimerScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val timers by Stores.timers.state.collectAsStateWithLifecycle()
    val presets by Stores.presets.state.collectAsStateWithLifecycle()
    val now by rememberNow(250)
    var h by rememberSaveable { mutableStateOf(0) }
    var m by rememberSaveable { mutableStateOf(5) }
    var s by rememberSaveable { mutableStateOf(0) }
    var label by rememberSaveable { mutableStateOf("") }
    val duration = (h * 3600L + m * 60L + s) * 1000L
    val showSheet = LocalActionSheet.current
    val showPrompt = LocalPrompt.current
    val savePreset = {
        val d = duration
        showPrompt(
            PromptSpec(
                title = "Guardar preajuste",
                subtitle = formatDurationShort(d),
                placeholder = "Nombre",
                confirmLabel = "Guardar",
            ) { name -> Timers.savePreset(name.ifBlank { formatDurationShort(d) }, d) },
        )
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item {
            ScreenTitle("Temporizador") {
                GlassIconButton(
                    Icons.Rounded.BookmarkAdd,
                    "Guardar preajuste",
                    savePreset,
                    enabled = duration > 0,
                )
            }
        }
        item {
            GlassCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn("h") { WheelPicker(100, h, { h = it }, width = 76.dp, fontSize = 34) }
                    Text(":", fontSize = 34.sp)
                    WheelColumn("min") { WheelPicker(60, m, { m = it }, width = 76.dp, fontSize = 34) }
                    Text(":", fontSize = 34.sp)
                    WheelColumn("s") { WheelPicker(60, s, { s = it }, width = 76.dp, fontSize = 34) }
                }
            }
        }
        if (presets.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        GlassChip(
                            "${preset.label} · ${formatDurationShort(preset.durationMs)}",
                            selected = preset.durationMs == duration,
                            onClick = {
                                val total = preset.durationMs / 1000
                                h = (total / 3600).toInt()
                                m = ((total / 60) % 60).toInt()
                                s = (total % 60).toInt()
                                label = preset.label
                            },
                            onLongClick = {
                                showSheet(
                                    SheetSpec(
                                        preset.label,
                                        subtitle = formatDurationShort(preset.durationMs),
                                        actions = listOf(
                                            SheetAction("Iniciar", Icons.Rounded.PlayArrow) {
                                                Timers.start(context, preset.durationMs, preset.label)
                                            },
                                            SheetAction("Eliminar preajuste", Icons.Rounded.Delete, danger = true) {
                                                Timers.deletePreset(preset.id)
                                            },
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            GlassTextField(
                label, { label = it }, "Nombre (opcional)",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        item {
            GlassButton(
                "Iniciar",
                onClick = {
                    Timers.start(context, duration, label.trim())
                    label = ""
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.PlayArrow,
                prominent = true,
                enabled = duration > 0,
            )
        }
        items(timers.reversed(), key = { it.id }) { timer ->
            TimerCard(timer, now)
        }
    }
}

@Composable
private fun WheelColumn(unit: String, wheel: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        wheel()
        Text(unit, fontSize = 13.sp, color = LocalGlass.current.contentSecondary)
    }
}

@Composable
private fun TimerCard(timer: TimerItem, now: Long) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val remaining = timer.remaining(now)
    val progress = if (timer.durationMs > 0) remaining.toFloat() / timer.durationMs else 0f
    val showSheet = LocalActionSheet.current
    val title = timer.label.ifBlank { formatDurationShort(timer.durationMs) }
    GlassCard(onLongClick = {
        showSheet(
            SheetSpec(
                title,
                subtitle = "Quedan ${formatTimer(timer.remaining(System.currentTimeMillis()))}",
                actions = buildList {
                    when (timer.state) {
                        TimerState.RUNNING -> add(SheetAction("Pausar", Icons.Rounded.Pause) { Timers.pause(context, timer.id) })
                        TimerState.PAUSED -> add(SheetAction("Reanudar", Icons.Rounded.PlayArrow) { Timers.resume(context, timer.id) })
                        TimerState.RINGING -> Unit
                    }
                    add(SheetAction("Añadir 1 minuto", Icons.Rounded.Add) { Timers.addTime(context, timer.id, 60_000) })
                    add(
                        SheetAction("Guardar como preajuste", Icons.Rounded.BookmarkAdd) {
                            Timers.savePreset(title, timer.durationMs)
                        },
                    )
                    add(SheetAction("Eliminar", Icons.Rounded.Delete, danger = true) { Timers.remove(context, timer.id) })
                },
            ),
        )
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val arc = Size(size.width - stroke, size.height - stroke)
                    val tl = Offset(stroke / 2, stroke / 2)
                    drawArc(p.content.copy(alpha = 0.12f), 0f, 360f, false, tl, arc, style = Stroke(stroke))
                    drawArc(
                        if (timer.state == TimerState.PAUSED) p.contentSecondary else p.accent,
                        -90f, 360f * progress, false, tl, arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 13.sp,
                    color = p.contentSecondary,
                )
                Text(
                    formatTimer(remaining),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = if (timer.state == TimerState.PAUSED) p.contentSecondary else p.content,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            GlassIconButton(Icons.Rounded.Add, "+1 min", { Timers.addTime(context, timer.id, 60_000) }, size = 42.dp)
            when (timer.state) {
                TimerState.RUNNING ->
                    GlassIconButton(Icons.Rounded.Pause, "Pausar", { Timers.pause(context, timer.id) }, size = 42.dp)
                TimerState.PAUSED ->
                    GlassIconButton(
                        Icons.Rounded.PlayArrow, "Reanudar", { Timers.resume(context, timer.id) },
                        size = 42.dp, prominent = true,
                    )
                TimerState.RINGING -> Unit
            }
            GlassIconButton(Icons.Rounded.Close, "Cancelar", { Timers.remove(context, timer.id) }, size = 42.dp)
        }
    }
}
