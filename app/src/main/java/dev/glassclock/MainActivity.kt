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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.glassclock.ui.glass.ActionSheetHost
import dev.glassclock.ui.glass.GlassButton
import dev.glassclock.ui.glass.GlassSurface
import dev.glassclock.ui.glass.LocalActionSheet
import dev.glassclock.ui.glass.LocalGlass
import dev.glassclock.ui.glass.SheetSpec
import dev.glassclock.ui.screens.missingPermissions
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
private const val TAB_STOPWATCH = 1
private const val TAB_TIMER = 2
private const val TAB_WORLD = 3

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
                App(settings.animatedBackground, requestedTab) { requestedTab = null }
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
private fun App(animated: Boolean, requestedTab: Int?, onTabConsumed: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_ALARM) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var sheet by remember { mutableStateOf<SheetSpec?>(null) }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            overlay = null
            onTabConsumed()
        }
    }
    BackHandler(enabled = overlay != null && sheet == null) { overlay = null }

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

    CompositionLocalProvider(LocalActionSheet provides { sheet = it }) {
        GlassRoot(
            animated = animated,
            floating = {
                AnimatedVisibility(
                    overlay == null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    GlassNavBar(
                        items = listOf(
                            NavItem("Alarma", Icons.Rounded.Alarm),
                            NavItem("Cronómetro", Icons.Rounded.Timer),
                            NavItem("Temporizador", Icons.Rounded.HourglassBottom),
                            NavItem("Mundial", Icons.Rounded.Public),
                        ),
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                PermissionPrompt()
                ActionSheetHost(sheet) { sheet = null }
            },
        ) {
            AnimatedContent(
                targetState = overlay,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150))
                },
                label = "overlay",
            ) { current ->
                when (current) {
                    null -> AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                        label = "tab",
                    ) { t ->
                        when (t) {
                            TAB_ALARM -> AlarmListScreen(
                                tabPadding,
                                onEdit = { overlay = Overlay.EditAlarm(it) },
                                onSettings = { overlay = Overlay.Settings },
                            )
                            TAB_STOPWATCH -> StopwatchScreen(tabPadding)
                            TAB_TIMER -> TimerScreen(tabPadding)
                            else -> WorldClockScreen(tabPadding, onAddCity = { overlay = Overlay.AddCity })
                        }
                    }
                    is Overlay.EditAlarm -> AlarmEditScreen(current.id, overlayPadding) { overlay = null }
                    Overlay.Settings -> SettingsScreen(overlayPadding) { overlay = null }
                    Overlay.AddCity -> AddCityScreen(overlayPadding) { overlay = null }
                }
            }
        }
    }
}

/**
 * Al abrir la app, si falta algún permiso imprescindible (sobre todo "pantalla completa",
 * sin el cual la alarma no enciende la pantalla), lo pide con una tarjeta en el centro.
 */
@Composable
private fun BoxScope.PermissionPrompt() {
    val context = LocalContext.current
    val p = LocalGlass.current
    var tick by remember { mutableIntStateOf(0) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val missing = remember(tick) { missingPermissions(context) }
    val visible = !dismissed && missing.isNotEmpty()
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
    }
    AnimatedVisibility(
        visible,
        modifier = Modifier.align(Alignment.Center).padding(20.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
    ) {
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(34.dp), strong = true, frost = 16.dp) {
            Column(Modifier.padding(24.dp)) {
                Text("Faltan permisos", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Para que la alarma suene a su hora y encienda la pantalla aunque el móvil " +
                        "esté bloqueado, Glass Clock necesita estos permisos.",
                    Modifier.padding(top = 6.dp, bottom = 14.dp),
                    fontSize = 14.sp,
                    color = p.contentSecondary,
                )
                missing.forEach { (label, intent) ->
                    GlassButton(
                        label,
                        { context.startActivity(intent) },
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        prominent = true,
                    )
                }
                GlassButton("Ahora no", { dismissed = true }, Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}
