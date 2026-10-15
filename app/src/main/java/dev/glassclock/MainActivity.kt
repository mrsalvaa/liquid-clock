package dev.glassclock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.glassclock.data.Stores
import dev.glassclock.ui.glass.GlassNavBar
import dev.glassclock.ui.glass.GlassRoot
import dev.glassclock.ui.glass.GlassTheme
import dev.glassclock.ui.glass.NavItem
import dev.glassclock.ui.screens.AddCityScreen
import dev.glassclock.ui.screens.AlarmEditScreen
import dev.glassclock.ui.screens.AlarmListScreen
import dev.glassclock.ui.screens.SettingsScreen
import dev.glassclock.ui.screens.StopwatchScreen
import dev.glassclock.ui.screens.TimerScreen
import dev.glassclock.ui.screens.WorldClockScreen

/** Pantallas que tapan las pestañas. */
sealed interface Overlay {
    data class EditAlarm(val id: Int?) : Overlay
    data object Settings : Overlay
    data object AddCity : Overlay
}

private const val TAB_ALARM = 0
private const val TAB_TIMER = 3

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TAB = "tab"
    }

    private var requestedTab by mutableStateOf<Int?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            GlassTheme {
                val settings by Stores.settings.state.collectAsStateWithLifecycle()
                GlassRoot(animated = settings.animatedBackground) {
                    App(requestedTab) { requestedTab = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_TAB) == "timer") requestedTab = TAB_TIMER
    }
}

@Composable
private fun App(requestedTab: Int?, onTabConsumed: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_ALARM) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            overlay = null
            onTabConsumed()
        }
    }
    BackHandler(enabled = overlay != null) { overlay = null }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val dir = LocalLayoutDirection.current
    val navHeight = 70.dp + 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabPadding = PaddingValues(
        top = insets.calculateTopPadding(),
        bottom = navHeight + 24.dp,
        start = insets.calculateStartPadding(dir),
        end = insets.calculateEndPadding(dir),
    )
    val overlayPadding = PaddingValues(
        top = insets.calculateTopPadding(),
        bottom = insets.calculateBottomPadding(),
    )

    AnimatedContent(
        targetState = overlay,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150))
        },
        label = "overlay",
    ) { current ->
        when (current) {
            null -> Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "tab",
                ) { t ->
                    when (t) {
                        0 -> AlarmListScreen(
                            tabPadding,
                            onEdit = { overlay = Overlay.EditAlarm(it) },
                            onSettings = { overlay = Overlay.Settings },
                        )
                        1 -> WorldClockScreen(tabPadding, onAddCity = { overlay = Overlay.AddCity })
                        2 -> StopwatchScreen(tabPadding)
                        else -> TimerScreen(tabPadding)
                    }
                }
                GlassNavBar(
                    items = listOf(
                        NavItem("Alarma", Icons.Rounded.Alarm),
                        NavItem("Mundial", Icons.Rounded.Public),
                        NavItem("Cronómetro", Icons.Rounded.Timer),
                        NavItem("Temporizador", Icons.Rounded.HourglassBottom),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            is Overlay.EditAlarm -> AlarmEditScreen(current.id, overlayPadding) { overlay = null }
            Overlay.Settings -> SettingsScreen(overlayPadding) { overlay = null }
            Overlay.AddCity -> AddCityScreen(overlayPadding) { overlay = null }
        }
    }
}
