package dev.glassclock.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassIconButton
import dev.glassclock.ui.glass.GlassSurface
import dev.glassclock.ui.glass.GlassTextField
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.ScreenTitle
import dev.glassclock.util.formatInstantClock
import dev.glassclock.util.formatLongDate
import dev.glassclock.util.formatOffsetDiff
import dev.glassclock.util.is24h
import dev.glassclock.util.relativeDay
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun cityName(zoneId: String) = zoneId.substringAfterLast('/').replace('_', ' ')
fun regionName(zoneId: String) = zoneId.substringBeforeLast('/').replace('_', ' ').replace("/", " · ")

@Composable
fun WorldClockScreen(contentPadding: PaddingValues, onAddCity: () -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    val cities by Stores.cities.state.collectAsStateWithLifecycle()
    val now by rememberNow(1000)
    val is24 = is24h(context)
    val localZone = ZoneId.systemDefault()

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item {
            ScreenTitle("Reloj mundial") {
                GlassIconButton(Icons.Rounded.Add, "Añadir ciudad", onAddCity, prominent = true)
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogClock(now, Modifier.padding(vertical = 12.dp).size(230.dp))
                Text(formatInstantClock(now, is24), fontSize = 44.sp, fontWeight = FontWeight.Light)
                Text(
                    formatLongDate(now) + " · " + cityName(localZone.id),
                    color = p.contentSecondary,
                    fontSize = 14.sp,
                )
            }
        }
        if (cities.isEmpty()) {
            item {
                GlassCard(onClick = onAddCity) {
                    Text("Añade ciudades", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Compara la hora de otras zonas horarias de un vistazo.", color = p.contentSecondary)
                }
            }
        }
        items(cities, key = { it }) { zoneId ->
            CityRow(zoneId, now, is24, localZone)
        }
    }
}

@Composable
private fun CityRow(zoneId: String, now: Long, is24: Boolean, localZone: ZoneId) {
    val p = LocalGlass.current
    val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrNull() } ?: return
    val instant = Instant.ofEpochMilli(now)
    val diff = zone.rules.getOffset(instant).totalSeconds - localZone.rules.getOffset(instant).totalSeconds
    val day = relativeDay(instant.atZone(zone), instant.atZone(localZone).toLocalDate())
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) {
                Stores.cities.update { list -> list - zoneId }
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Rounded.Delete, "Eliminar", tint = p.danger)
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cityName(zoneId), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("$day, ${formatOffsetDiff(diff)}", fontSize = 13.sp, color = p.contentSecondary)
                }
                Text(formatInstantClock(now, is24, zone), fontSize = 34.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
fun AnalogClock(now: Long, modifier: Modifier = Modifier) {
    val p = LocalGlass.current
    val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val seconds = t.second.toFloat()
    val minutes = t.minute + seconds / 60f
    val hours = (t.hour % 12) + minutes / 60f
    GlassSurface(modifier, shape = CircleShape, strong = true, blur = 32.dp) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val r = size.minDimension / 2
            val c = center
            fun point(fraction: Float, length: Float): Offset {
                val a = fraction * 2 * PI - PI / 2
                return Offset(c.x + cos(a).toFloat() * length, c.y + sin(a).toFloat() * length)
            }
            for (i in 0 until 60) {
                val major = i % 5 == 0
                drawLine(
                    color = p.content.copy(alpha = if (major) 0.7f else 0.22f),
                    start = point(i / 60f, r * if (major) 0.84f else 0.9f),
                    end = point(i / 60f, r * 0.96f),
                    strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawLine(p.content, c, point(hours / 12f, r * 0.5f), 6.dp.toPx(), StrokeCap.Round)
            drawLine(p.content, c, point(minutes / 60f, r * 0.74f), 4.dp.toPx(), StrokeCap.Round)
            drawLine(p.accent, point(seconds / 60f, -r * 0.12f), point(seconds / 60f, r * 0.82f), 2.dp.toPx(), StrokeCap.Round)
            drawCircle(p.accent, 5.dp.toPx(), c)
        }
    }
}

private fun normalize(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

@Composable
fun AddCityScreen(contentPadding: PaddingValues, onClose: () -> Unit) {
    val p = LocalGlass.current
    val context = LocalContext.current
    val is24 = is24h(context)
    val now by rememberNow(60_000)
    var query by remember { mutableStateOf("") }
    val all = remember {
        ZoneId.getAvailableZoneIds()
            .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .sortedBy { cityName(it) }
    }
    val filtered = remember(query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) all else all.filter { normalize(it.replace('_', ' ')).contains(q) }
    }
    Column(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp)) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onClose)
            Text("Añadir ciudad", Modifier.padding(start = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        GlassTextField(
            query, { query = it }, "Buscar ciudad o región",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(filtered, key = { it }) { zoneId ->
                val zone = ZoneId.of(zoneId)
                GlassCard(onClick = {
                    Stores.cities.update { if (zoneId in it) it else it + zoneId }
                    onClose()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cityName(zoneId), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text(
                                regionName(zoneId) + " · GMT" + zone.rules.getOffset(Instant.ofEpochMilli(now)).id.replace("Z", "+00:00"),
                                fontSize = 12.sp,
                                color = p.contentSecondary,
                            )
                        }
                        Text(formatInstantClock(now, is24, zone), fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
