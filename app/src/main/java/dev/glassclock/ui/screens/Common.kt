package dev.glassclock.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import kotlinx.coroutines.delay

/** Hora actual (epoch ms), actualizada en cada múltiplo de [periodMs]. */
@Composable
fun rememberNow(periodMs: Long = 1000): State<Long> =
    produceState(System.currentTimeMillis(), periodMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(periodMs - value % periodMs)
        }
    }

/** null = predeterminado, "" = silencio. */
fun ringtoneTitle(context: Context, uri: String?): String = when (uri) {
    null -> "Predeterminado"
    "" -> "Silencio"
    else -> try {
        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) ?: "Desconocido"
    } catch (e: Exception) {
        "Desconocido"
    }
}

/** Devuelve una función que abre el selector de tonos del sistema. */
@Composable
fun rememberRingtonePicker(onPicked: (String?) -> Unit): (current: String?) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = res.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        onPicked(
            when (picked) {
                null -> ""
                Settings.System.DEFAULT_ALARM_ALERT_URI -> null
                else -> picked.toString()
            },
        )
    }
    return remember(launcher) {
        { current ->
            val existing: Uri? = when (current) {
                null -> Settings.System.DEFAULT_ALARM_ALERT_URI
                "" -> null
                else -> Uri.parse(current)
            }
            launcher.launch(
                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonido"),
            )
        }
    }
}
