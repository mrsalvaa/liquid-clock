package dev.glassclock.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import dev.glassclock.data.AppSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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
    /** Azul de píldoras, interruptores y resaltados. */
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val success: Color,
    /** Acento para iconos, textos y líneas. */
    val accentFg: Color = accent,
)

/** Tema oscuro: fondo casi negro y liso, azul claro para que destaque sobre él. */
val DarkGlass = GlassPalette(
    dark = true,
    base = Color(0xFF0E0F12),
    content = Color.White,
    contentSecondary = Color.White.copy(alpha = 0.6f),
    tint = Color.White.copy(alpha = 0.05f),
    tintStrong = Color.White.copy(alpha = 0.09f),
    highlight = 0.10f,
    rim = 0.45f,
    accent = Color(0xFF8AB4F8),
    onAccent = Color.White,
    danger = Color(0xFFFF7A8A),
    success = Color(0xFF6EE7B7),
)

/** Tema claro: fondo gris muy claro y liso, azul más intenso para que se lea. */
val LightGlass = GlassPalette(
    dark = false,
    base = Color(0xFFF2F3F5),
    content = Color(0xFF111318),
    contentSecondary = Color(0xFF111318).copy(alpha = 0.58f),
    tint = Color.White.copy(alpha = 0.7f),
    tintStrong = Color.White.copy(alpha = 0.85f),
    highlight = 0.4f,
    rim = 0.9f,
    accent = Color(0xFF1A73E8),
    onAccent = Color.White,
    danger = Color(0xFFE5484D),
    success = Color(0xFF12A594),
)

/** Texto legible sobre un fondo de ese color. */
fun readableOn(color: Color): Color = if (color.luminance() > 0.45f) Color(0xFF0B0D12) else Color.White

/** Texto legible sobre el cristal teñido de acento (acento al ~55 % sobre la base). */
private fun onAccentGlass(accent: Color, base: Color): Color = readableOn(accent.copy(alpha = 0.55f).compositeOver(base))

val LocalGlass = staticCompositionLocalOf { DarkGlass }

/** false en las pestañas ocultas: sirve para pausar lo que se refresca en cada fotograma. */
val LocalTabActive = compositionLocalOf { true }
/** Lo que refractan las superficies de cristal: el fondo, o el fondo más el contenido. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { error("Falta GlassRoot") }

/** Si el tema es oscuro según los ajustes (0 sistema, 1 claro, 2 oscuro). */
@Composable
fun isDarkTheme(themeMode: Int): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun GlassTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings.themeMode)
    val base = if (dark) DarkGlass else LightGlass
    val p = base.copy(onAccent = onAccentGlass(base.accent, base.base))
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.base, onBackground = p.content,
            surface = Color(0xFF1A1C21), onSurface = p.content,
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
    floating: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val background = rememberLayerBackdrop()
    val page = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(page)) {
            // Fondo liso del tema: es lo que refractan las superficies de cristal.
            Box(Modifier.fillMaxSize().background(LocalGlass.current.base).layerBackdrop(background))
            CompositionLocalProvider(LocalBackdrop provides background) {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
        // Degradado bajo la barra de estado para que el contenido no se mezcle con la hora.
        val p = LocalGlass.current
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Brush.verticalGradient(listOf(p.base, p.base.copy(alpha = 0.6f)))),
        )
        CompositionLocalProvider(LocalBackdrop provides page) {
            Box(Modifier.fillMaxSize(), content = floating)
        }
    }
}
