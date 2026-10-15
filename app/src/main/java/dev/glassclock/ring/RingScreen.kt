package dev.glassclock.ring

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassChip
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSurface
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.SlideToConfirm
import dev.glassclock.ui.screens.rememberNow
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.formatLongDate
import dev.glassclock.util.is24h

@Composable
fun RingScreen(state: RingState) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        RingMain(state, onDismiss = { RingService.send(context, RingService.ACTION_DISMISS) })
    }
}

@Composable
private fun RingMain(state: RingState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val now by rememberNow(1000)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    var snoozeMin by remember(state.id) { mutableIntStateOf(state.snoozeDefault) }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            GlassSurface(
                Modifier.size(92.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                shape = CircleShape,
                strong = true,
                tint = p.accent.copy(alpha = 0.45f),
            ) {
                Icon(
                    if (state.kind == RingState.Kind.ALARM) Icons.Rounded.Alarm else Icons.Rounded.HourglassBottom,
                    null,
                    Modifier.align(Alignment.Center).size(44.dp),
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(state.label, fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(formatInstantClock(now, is24h(context)), fontSize = 84.sp, fontWeight = FontWeight.Thin)
            Text(formatLongDate(now), fontSize = 15.sp, color = p.contentSecondary)
        }

        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            when {
                state.kind == RingState.Kind.TIMER -> {
                    GlassButton(
                        "+1 min",
                        { RingService.send(context, RingService.ACTION_TIMER_ADD) },
                        Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Add,
                    )
                    Spacer(Modifier.height(14.dp))
                }
                state.snoozeAllowed -> {
                    SnoozePanel(state, snoozeMin, { snoozeMin = it })
                    Spacer(Modifier.height(14.dp))
                }
            }
            SlideToConfirm(
                text = if (state.kind == RingState.Kind.TIMER) "Desliza para detener" else "Desliza para descartar",
                onConfirm = onDismiss,
            )
        }
    }
}

/** Posponer con chips rápidos y ajuste fino minuto a minuto. */
@Composable
private fun SnoozePanel(state: RingState, minutes: Int, onMinutes: (Int) -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Posponer", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            state.snoozesLeft?.let {
                Text("Quedan $it", fontSize = 13.sp, color = p.contentSecondary)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            GlassIconButton(Icons.Rounded.Remove, "Menos", { onMinutes((minutes - 1).coerceAtLeast(1)) })
            Text(
                "$minutes min",
                Modifier.weight(1f),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )
            GlassIconButton(Icons.Rounded.Add, "Más", { onMinutes((minutes + 1).coerceAtMost(120)) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { m ->
                GlassChip("$m", minutes == m, { onMinutes(m) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(
            "Posponer $minutes min",
            { RingService.send(context, RingService.ACTION_SNOOZE, minutes) },
            Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Snooze,
            prominent = true,
        )
    }
}
