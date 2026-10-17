package dev.glassclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import dev.glassclock.data.AppSettings
import dev.glassclock.ui.glass.GlassCard
import dev.glassclock.ui.glass.GlassSwitch
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.SettingRow
import dev.glassclock.util.AppIcons

@Composable
fun AppearanceCard(s: AppSettings, edit: ((AppSettings) -> AppSettings) -> Unit) {
    val context = LocalContext.current
    val p = LocalGlass.current
    GlassCard {
        Label("Tema")
        ChipRow(listOf(0, 1, 2), s.themeMode, { listOf("Sistema", "Claro", "Oscuro")[it] }) { v ->
            edit { it.copy(themeMode = v) }
        }

        Label("Icono de la app")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppIcons.all.forEachIndexed { i, icon ->
                IconChoice(icon, selected = s.appIcon == i) {
                    if (s.appIcon != i) {
                        edit { it.copy(appIcon = i) }
                        AppIcons.apply(context, i)
                    }
                }
            }
        }
        Text(
            "El launcher puede tardar unos segundos en mostrar el icono nuevo, y quizá tengas que " +
                "volver a ponerlo en la pantalla de inicio.",
            fontSize = 12.sp,
            color = p.contentSecondary,
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 10.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

/** Vista previa de un icono adaptativo: fondo y primer plano recortados en forma de "squircle". */
@Composable
private fun IconChoice(icon: AppIcons.Icon, selected: Boolean, onClick: () -> Unit) {
    val p = LocalGlass.current
    val shape: Shape = RoundedCornerShape(30)
    Column(
        Modifier.width(66.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .border(2.dp, if (selected) p.content else Color.Transparent, RoundedCornerShape(32))
                .padding(4.dp)
                .clip(shape),
        ) {
            // El icono adaptativo mide 108 y se ven los 72 centrales: se amplía 1,5 veces.
            val zoom = Modifier.matchParentSize().graphicsLayer {
                scaleX = 1.5f
                scaleY = 1.5f
            }
            Image(painterResource(icon.background), null, zoom)
            Image(painterResource(icon.foreground), null, zoom)
        }
        Text(
            icon.name,
            Modifier.padding(top = 6.dp),
            fontSize = 12.sp,
            color = if (selected) p.content else p.contentSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
