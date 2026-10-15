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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    base = Color(0xFF030407),
    content = Color.White,
    contentSecondary = Color.White.copy(alpha = 0.6f),
    tint = Color(0xFF0A0C12).copy(alpha = 0.28f),
    tintStrong = Color(0xFF0A0C12).copy(alpha = 0.42f),
    highlight = 0.10f,
    rim = 0.45f,
    accent = Color(0xFF7CC8FF),
    onAccent = Color(0xFF041322),
    danger = Color(0xFFFF7A8A),
    success = Color(0xFF6EE7B7),
    blobs = listOf(Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFF0E7490), Color(0xFF9D174D)),
)

val LightGlass = GlassPalette(
    dark = false,
    base = Color(0xFFF1F3F8),
    content = Color(0xFF0E1222),
    contentSecondary = Color(0xFF0E1222).copy(alpha = 0.58f),
    tint = Color.White.copy(alpha = 0.32f),
    tintStrong = Color.White.copy(alpha = 0.5f),
    highlight = 0.4f,
    rim = 0.9f,
    accent = Color(0xFF1C7ED6),
    onAccent = Color.White,
    danger = Color(0xFFE5484D),
    success = Color(0xFF12A594),
    blobs = listOf(Color(0xFFA5DCFB), Color(0xFFFBC4DE), Color(0xFF7CC8EE), Color(0xFFF5A3C7)),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }
/** Lo que refractan las superficies de cristal: el fondo, o el fondo más el contenido. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { error("Falta GlassRoot") }

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
 * Raíz de cada pantalla. Hay dos capas que el cristal puede refractar:
 * - el fondo de color, para las superficies que están dentro del contenido (tarjetas, botones);
 * - el fondo más el contenido, para lo que flota encima ([floating]: barra inferior, menús).
 */
@Composable
fun GlassRoot(
    animated: Boolean = true,
    floating: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val background = rememberLayerBackdrop()
    val page = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(page)) {
            LiquidBackground(Modifier.fillMaxSize().layerBackdrop(background), animated)
            CompositionLocalProvider(LocalBackdrop provides background) {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
        CompositionLocalProvider(LocalBackdrop provides page) {
            Box(Modifier.fillMaxSize(), content = floating)
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
    val alpha = if (p.dark) 0.34f else 0.6f
    Canvas(modifier) {
        drawRect(p.base)
        val w = size.width
        val h = size.height
        val r = max(w, h) * 0.48f
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
