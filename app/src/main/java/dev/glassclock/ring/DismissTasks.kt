package dev.glassclock.ring

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.glassclock.data.DismissTask
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSurface
import dev.glassclock.ui.glass.GlassTextField
import dev.glassclock.ui.glass.LocalGlass
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun DismissTaskScreen(state: RingState, onSolved: () -> Unit, onInteract: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onBack)
            Text(
                when (state.task) {
                    DismissTask.MATH -> "Resuelve para apagar"
                    DismissTask.SHAKE -> "Agita para apagar"
                    DismissTask.TYPE -> "Escribe para apagar"
                    DismissTask.NONE -> ""
                },
                Modifier.padding(start = 12.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when (state.task) {
            DismissTask.MATH -> MathTask(state.taskDifficulty, state.taskAmount.coerceAtLeast(1), onSolved, onInteract)
            DismissTask.SHAKE -> ShakeTask(state.taskAmount.coerceAtLeast(1), onSolved, onInteract)
            DismissTask.TYPE -> TypeTask(state.taskAmount.coerceAtLeast(1), onSolved, onInteract)
            DismissTask.NONE -> LaunchedEffect(Unit) { onSolved() }
        }
    }
}

// --- Cálculo ---

private data class Problem(val text: String, val answer: Int)

private fun newProblem(difficulty: Int): Problem {
    val r = Random.Default
    return when (difficulty) {
        0 -> {
            val a = r.nextInt(10, 50)
            val b = r.nextInt(10, 50)
            Problem("$a + $b", a + b)
        }
        1 -> {
            val a = r.nextInt(3, 13)
            val b = r.nextInt(3, 13)
            val c = r.nextInt(10, 100)
            Problem("$a × $b + $c", a * b + c)
        }
        else -> {
            val a = r.nextInt(11, 30)
            val b = r.nextInt(3, 10)
            val c = r.nextInt(6, 16)
            val d = r.nextInt(6, 16)
            Problem("$a × $b + $c × $d", a * b + c * d)
        }
    }
}

@Composable
private fun MathTask(difficulty: Int, count: Int, onSolved: () -> Unit, onInteract: () -> Unit) {
    val p = LocalGlass.current
    val scope = rememberCoroutineScope()
    var solved by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(newProblem(difficulty)) }
    var input by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }

    fun press(key: String) {
        onInteract()
        when (key) {
            "⌫" -> input = input.dropLast(1)
            "✓" -> if (input.toIntOrNull() == problem.answer) {
                solved++
                if (solved >= count) onSolved() else {
                    problem = newProblem(difficulty)
                    input = ""
                }
            } else {
                input = ""
                scope.launch {
                    for (x in listOf(24f, -20f, 14f, -8f, 0f)) shake.animateTo(x)
                }
            }
            else -> if (input.length < 6) input += key
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Problema ${solved + 1} de $count", color = p.contentSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(problem.text, fontSize = 44.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(16.dp))
        GlassSurface(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .offset { IntOffset(shake.value.roundToInt(), 0) },
            shape = RoundedCornerShape(22.dp),
            strong = true,
        ) {
            Text(
                input.ifEmpty { "?" },
                Modifier.align(Alignment.Center),
                fontSize = 36.sp,
                color = if (input.isEmpty()) p.contentSecondary else p.content,
            )
        }
        Spacer(Modifier.weight(1f))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
        keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { key ->
                    GlassSurface(
                        Modifier.weight(1f).aspectRatio(1.9f),
                        shape = RoundedCornerShape(22.dp),
                        strong = key == "✓",
                        tint = if (key == "✓") p.accent.copy(alpha = 0.55f) else null,
                        blur = 20.dp,
                        onClick = { press(key) },
                    ) {
                        when (key) {
                            "⌫" -> Icon(Icons.AutoMirrored.Rounded.Backspace, "Borrar", Modifier.align(Alignment.Center))
                            "✓" -> Icon(
                                Icons.Rounded.Check, "Comprobar", Modifier.align(Alignment.Center),
                                tint = androidx.compose.ui.graphics.Color.White,
                            )
                            else -> Text(key, Modifier.align(Alignment.Center), fontSize = 26.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// --- Agitar ---

@Composable
private fun ShakeTask(count: Int, onSolved: () -> Unit, onInteract: () -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    var shakes by remember { mutableIntStateOf(0) }
    val solved by rememberUpdatedState(onSolved)
    val interact by rememberUpdatedState(onInteract)
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    if (accelerometer != null) {
        DisposableEffect(Unit) {
            var last = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (x, y, z) = event.values
                    val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                    val t = System.currentTimeMillis()
                    if (g > 2.2f && t - last > 250) {
                        last = t
                        shakes++
                        interact()
                        if (shakes >= count) solved()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val progress by animateFloatAsState(shakes.coerceAtMost(count) / count.toFloat(), label = "shake")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                val tl = Offset(stroke / 2, stroke / 2)
                drawArc(p.content.copy(alpha = 0.12f), 0f, 360f, false, tl, arc, style = Stroke(stroke))
                drawArc(p.accent, -90f, 360f * progress, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(count - shakes).coerceAtLeast(0)}", fontSize = 72.sp, fontWeight = FontWeight.Light)
                Text("sacudidas", color = p.contentSecondary)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (accelerometer == null) {
            Text("Este dispositivo no tiene acelerómetro.", color = p.danger)
            Spacer(Modifier.height(12.dp))
            GlassButton("Descartar", onSolved, Modifier.fillMaxWidth(), prominent = true)
        } else {
            Text("¡Agita el teléfono con fuerza!", fontSize = 18.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
    }
}

// --- Escribir ---

private val PHRASES = listOf(
    "Hoy es un gran día para empezar",
    "Me levanto y no vuelvo a la cama",
    "El café ya me está esperando",
    "Despierto con energía y buen humor",
    "Cada mañana es una nueva oportunidad",
    "Las sábanas pueden esperar hasta la noche",
    "Abro la ventana y respiro aire fresco",
    "Ya estoy despierto y listo para el día",
)

private fun normalizePhrase(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9ñ ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

@Composable
private fun TypeTask(count: Int, onSolved: () -> Unit, onInteract: () -> Unit) {
    val p = LocalGlass.current
    val phrases = remember { PHRASES.shuffled().take(count.coerceAtMost(PHRASES.size)) }
    var index by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val target = phrases[index]

    Column(Modifier.fillMaxSize()) {
        Text("Frase ${index + 1} de ${phrases.size}", color = p.contentSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        GlassCard {
            Text(target, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
        GlassTextField(
            input,
            {
                onInteract()
                input = it
                if (normalizePhrase(it) == normalizePhrase(target)) {
                    if (index + 1 >= phrases.size) onSolved() else {
                        index++
                        input = ""
                    }
                }
            },
            "Escribe la frase exactamente",
            singleLine = false,
            textStyle = TextStyle(fontSize = 20.sp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
            ),
        )
        Text(
            "No importan las mayúsculas, tildes ni signos de puntuación.",
            Modifier.padding(top = 8.dp, start = 8.dp),
            fontSize = 12.sp,
            color = p.contentSecondary,
        )
    }
}
