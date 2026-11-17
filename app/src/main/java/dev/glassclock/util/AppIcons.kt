package dev.glassclock.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dev.glassclock.R

/** Iconos alternativos de la app, cada uno es un activity-alias del manifest. */
object AppIcons {
    data class Icon(val name: String, val alias: String, val background: Int, val foreground: Int)

    val all = listOf(
        Icon("Liquid", "IconLiquid", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground),
        Icon("Material", "IconMaterial", R.drawable.ic_launcher_material_background, R.drawable.ic_launcher_material_foreground),
        Icon("Samsung", "IconSamsung", R.drawable.ic_launcher_samsung_background, R.drawable.ic_launcher_samsung_foreground),
        Icon("iPhone", "IconIphone", R.drawable.ic_launcher_iphone_background, R.drawable.ic_launcher_iphone_foreground),
        Icon("Motorola", "IconMotorola", R.drawable.ic_launcher_motorola_background, R.drawable.ic_launcher_motorola_foreground),
    )

    /** Activa el alias elegido y desactiva el resto (primero activa, para no quedarse sin icono). */
    fun apply(context: Context, index: Int) {
        val pm = context.packageManager
        fun set(icon: Icon, enabled: Boolean) = pm.setComponentEnabledSetting(
            ComponentName(context.packageName, "dev.glassclock.${icon.alias}"),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        val chosen = all.getOrElse(index) { all[0] }
        set(chosen, true)
        all.filter { it != chosen }.forEach { set(it, false) }
    }

    private var largeFor: String? = null
    private var largeBitmap: android.graphics.Bitmap? = null

    /** El ícono que está puesto en el launcher: el alias encendido (o, sin tocar nada, el del manifest). */
    fun current(context: Context): Icon? {
        val pm = context.packageManager
        return all.firstOrNull { icon ->
            val component = ComponentName(context.packageName, "dev.glassclock.${icon.alias}")
            when (pm.getComponentEnabledSetting(component)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ->
                    runCatching { pm.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS).enabled }.getOrDefault(false)
                else -> false
            }
        } ?: all.firstOrNull()
    }

    /**
     * El ícono elegido, dibujado para ir como imagen grande de una notificación. El ícono de app que
     * el sistema pone en cada notificación es el fijo del manifest y no se puede cambiar en uso; así
     * el elegido también se ve ahí.
     */
    @Synchronized
    fun large(context: Context): android.graphics.Bitmap? {
        val icon = current(context) ?: return null
        largeBitmap?.takeIf { largeFor == icon.alias }?.let { return it }
        return runCatching {
            val drawable = android.graphics.drawable.AdaptiveIconDrawable(context.getDrawable(icon.background), context.getDrawable(icon.foreground))
            val size = (48 * context.resources.displayMetrics.density).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(android.graphics.Canvas(bitmap))
            largeFor = icon.alias
            largeBitmap = bitmap
            bitmap
        }.getOrNull()
    }
}
