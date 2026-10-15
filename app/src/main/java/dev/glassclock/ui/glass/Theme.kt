package dev.glassclock.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Immutable
data class GlassPalette(
    val dark: Boolean,
    val base: Color,
    val content: Color,
    val contentSecondary: Color,
    val tint: Color,
    val tintStrong: Color,
    val highlight: Float,
    val rim: Float,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val success: Color,
    val blobs: List<Color>,
)

val DarkGlass = GlassPalette(
    dark = true,
    base = Color(0xFF070B18),
    content = Color.White,
    contentSecondary = Color.White.copy(alpha = 0.62f),
    tint = Color.White.copy(alpha = 0.07f),
    tintStrong = Color.White.copy(alpha = 0.13f),
    highlight = 0.14f,
    rim = 0.45f,
    accent = Color(0xFF7FB2FF),
    onAccent = Color(0xFF06122B),
    danger = Color(0xFFFF7A85),
    success = Color(0xFF6EE7B7),
    blobs = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4), Color(0xFFEC4899), Color(0xFF7C3AED)),
)

val LightGlass = GlassPalette(
    dark = false,
    base = Color(0xFFEFF2FA),
    content = Color(0xFF0E1222),
    contentSecondary = Color(0xFF0E1222).copy(alpha = 0.58f),
    tint = Color.White.copy(alpha = 0.40f),
    tintStrong = Color.White.copy(alpha = 0.58f),
    highlight = 0.55f,
    rim = 0.9f,
    accent = Color(0xFF2F6BFF),
    onAccent = Color.White,
    danger = Color(0xFFE5484D),
    success = Color(0xFF12A594),
    blobs = listOf(Color(0xFF93A8FF), Color(0xFF7FDBFF), Color(0xFFFFADCF), Color(0xFFC4B1FF)),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }
val LocalHaze = staticCompositionLocalOf<HazeState> { error("Falta GlassRoot") }

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkGlass else LightGlass
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.base, onBackground = p.content,
            surface = Color(0xFF151A2C), onSurface = p.content,
            error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.base, onBackground = p.content,
            surface = Color(0xFFF8F9FD), onSurface = p.content,
            error = p.danger,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalGlass provides p,
            LocalContentColor provides p.content,
            content = content,
        )
    }
}

/**
 * Raíz de cada pantalla: un fondo de manchas de color que hace de "fuente" del desenfoque,
 * y encima el contenido, cuyas superficies de cristal desenfocan ese fondo.
 */
@Composable
fun GlassRoot(animated: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val haze = remember { HazeState() }
    Box(Modifier.fillMaxSize()) {
        LiquidBackground(Modifier.fillMaxSize().hazeSource(haze), animated)
        CompositionLocalProvider(LocalHaze provides haze) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
fun LiquidBackground(modifier: Modifier = Modifier, animated: Boolean = true) {
    val p = LocalGlass.current
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "phase",
    )
    val alpha = if (p.dark) 0.55f else 0.75f
    Canvas(modifier) {
        drawRect(p.base)
        val w = size.width
        val h = size.height
        val r = max(w, h) * 0.55f
        val t = if (animated) phase else 0.8f
        p.blobs.forEachIndexed { i, color ->
            val a = t * (1 + i * 0.25f) + i * 1.7f
            val center = Offset(
                x = w * (0.5f + 0.38f * cos(a)),
                y = h * (0.5f + 0.40f * sin(a * 0.8f + i)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}
