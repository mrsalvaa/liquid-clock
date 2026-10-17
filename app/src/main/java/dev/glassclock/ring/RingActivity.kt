package dev.glassclock.ring

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassRoot
import dev.glassclock.ui.glass.GlassTheme

/** Pantalla completa sobre la pantalla de bloqueo mientras suena una alarma o temporizador. */
class RingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Atrás no descarta la alarma.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        setContent {
            dev.glassclock.SystemBarsFor(dev.glassclock.ui.glass.isDarkTheme(Stores.settings.value.themeMode))
            GlassTheme(Stores.settings.value) {
                val state by RingService.current.collectAsStateWithLifecycle()
                LaunchedEffect(state) { if (state == null) finish() }
                GlassRoot {
                    state?.let { RingScreen(it) }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val state = RingService.current.value
            if (state != null && Stores.settings.value.volumeKeysSnooze &&
                state.kind == RingState.Kind.ALARM && state.snoozeAllowed
            ) {
                RingService.send(this, RingService.ACTION_SNOOZE, state.snoozeDefault)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
