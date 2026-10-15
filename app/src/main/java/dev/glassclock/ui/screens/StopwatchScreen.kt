package dev.glassclock.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.data.StopwatchData
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassSurface
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.ScreenTitle
import dev.glassclock.util.formatStopwatch

object Stopwatch {
    fun start() {
        Stores.stopwatch.update { it.copy(running = true, startRealtime = SystemClock.elapsedRealtime()) }
    }

    fun pause() {
        Stores.stopwatch.update {
            it.copy(running = false, accumulatedMs = it.elapsed(SystemClock.elapsedRealtime()))
        }
    }

    fun lap() {
        Stores.stopwatch.update { it.copy(laps = it.laps + it.elapsed(SystemClock.elapsedRealtime())) }
    }

    fun reset() {
        Stores.stopwatch.update { StopwatchData() }
    }
}

@Composable
fun StopwatchScreen(contentPadding: PaddingValues) {
    val p = LocalGlass.current
    val data by Stores.stopwatch.state.collectAsStateWithLifecycle()
    var nowRt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(data.running) {
        nowRt = SystemClock.elapsedRealtime()
        while (data.running) withFrameMillis { nowRt = SystemClock.elapsedRealtime() }
    }
    val elapsed = data.elapsed(nowRt)
    val lapTimes = remember(data.laps) {
        data.laps.mapIndexed { i, total -> total - (data.laps.getOrNull(i - 1) ?: 0L) }
    }
    val best = if (lapTimes.size >= 3) lapTimes.min() else null
    val worst = if (lapTimes.size >= 3) lapTimes.max() else null
    val currentLap = elapsed - (data.laps.lastOrNull() ?: 0L)

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item { ScreenTitle("Cronómetro") }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                GlassSurface(Modifier.size(280.dp), shape = CircleShape, strong = true, frost = 8.dp) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = p.content.copy(alpha = 0.1f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
                        )
                        drawArc(
                            color = p.accent,
                            startAngle = -90f,
                            sweepAngle = (elapsed % 60_000) / 60_000f * 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatStopwatch(elapsed),
                            fontSize = if (elapsed >= 3_600_000) 40.sp else 50.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp,
                        )
                        if (data.laps.isNotEmpty()) {
                            Text(
                                "Vuelta ${data.laps.size + 1} · ${formatStopwatch(currentLap)}",
                                fontSize = 14.sp,
                                color = p.contentSecondary,
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    data.running -> GlassButton("Vuelta", Stopwatch::lap, Modifier.weight(1f), icon = Icons.Rounded.Flag)
                    elapsed > 0 -> GlassButton("Reiniciar", Stopwatch::reset, Modifier.weight(1f), icon = Icons.Rounded.Refresh)
                    else -> GlassButton("Vuelta", {}, Modifier.weight(1f), icon = Icons.Rounded.Flag, enabled = false)
                }
                if (data.running) {
                    GlassButton("Detener", Stopwatch::pause, Modifier.weight(1f), icon = Icons.Rounded.Pause, danger = true)
                } else {
                    GlassButton(
                        if (elapsed > 0) "Reanudar" else "Iniciar",
                        Stopwatch::start,
                        Modifier.weight(1f),
                        icon = Icons.Rounded.PlayArrow,
                        prominent = true,
                    )
                }
            }
        }
        if (lapTimes.isNotEmpty()) {
            item {
                GlassCard {
                    lapTimes.indices.reversed().forEach { i ->
                        val lap = lapTimes[i]
                        val color = when (lap) {
                            best -> p.success
                            worst -> p.danger
                            else -> p.content
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("Vuelta ${i + 1}", Modifier.weight(1f), color = color, fontSize = 15.sp)
                            Text(
                                formatStopwatch(lap),
                                Modifier.weight(1f),
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                            )
                            Text(
                                formatStopwatch(data.laps[i]),
                                color = p.contentSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
