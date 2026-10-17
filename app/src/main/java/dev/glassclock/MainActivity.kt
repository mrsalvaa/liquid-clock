package dev.glassclock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
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
import dev.glassclock.ui.glass.LocalTabActive
import dev.glassclock.ui.glass.SheetSpec
import dev.glassclock.ui.glass.LocalPrompt
import dev.glassclock.ui.glass.PromptHost
import dev.glassclock.ui.glass.PromptSpec
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
import dev.glassclock.ui.glass.isDarkTheme
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
    /** [id] null = alarma nueva, opcionalmente dentro de [groupId]. */
    data class EditAlarm(val id: Int?, val groupId: Int? = null) : Overlay
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
            val settings by Stores.settings.state.collectAsStateWithLifecycle()
            SystemBarsFor(isDarkTheme(settings.themeMode))
            GlassTheme(settings) {
                App(requestedTab) { requestedTab = null }
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
    // Posición de la barra mientras se desliza entre pestañas (p. ej. 1,4), o null.
    var tabDrag by remember { mutableStateOf<Float?>(null) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var sheet by remember { mutableStateOf<SheetSpec?>(null) }
    var prompt by remember { mutableStateOf<PromptSpec?>(null) }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            overlay = null
            onTabConsumed()
        }
    }
    BackHandler(enabled = overlay != null && sheet == null && prompt == null) { overlay = null }

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

    CompositionLocalProvider(
        LocalActionSheet provides { sheet = it },
        LocalPrompt provides { prompt = it },
    ) {
        GlassRoot(
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
                        dragPosition = { tabDrag },
                        onSelect = { tab = it },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                PermissionPrompt()
                ActionSheetHost(sheet) { sheet = null }
                PromptHost(prompt) { prompt = null }
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
                    null -> SwipeTabs(tab, onTab = { tab = it }, onDrag = { tabDrag = it }) { t ->
                        when (t) {
                            TAB_ALARM -> AlarmListScreen(
                                tabPadding,
                                onEdit = { overlay = Overlay.EditAlarm(it) },
                                onNew = { overlay = Overlay.EditAlarm(null, it) },
                                onSettings = { overlay = Overlay.Settings },
                            )
                            TAB_STOPWATCH -> StopwatchScreen(tabPadding)
                            TAB_TIMER -> TimerScreen(tabPadding)
                            else -> WorldClockScreen(tabPadding, onAddCity = { overlay = Overlay.AddCity })
                        }
                    }
                    is Overlay.EditAlarm -> AlarmEditScreen(current.id, current.groupId, overlayPadding) { overlay = null }
                    Overlay.Settings -> SettingsScreen(overlayPadding) { overlay = null }
                    Overlay.AddCity -> AddCityScreen(overlayPadding) { overlay = null }
                }
            }
        }
    }
}

/**
 * Pestañas que se cambian deslizando. Todas siguen creadas (no se reconstruyen pantalla ni cristal
 * en cada cambio, que daba tirones a 120 Hz), pero solo se dibuja la actual y, mientras se arrastra,
 * la vecina. Las ocultas quedan debajo y se tragan los toques.
 */
@Composable
private fun SwipeTabs(
    tab: Int,
    onTab: (Int) -> Unit,
    onDrag: (Float?) -> Unit,
    content: @Composable (Int) -> Unit,
) {
    var width by remember { mutableIntStateOf(1) }
    // Pestaña mostrada: se adelanta al estado del padre para que un deslizamiento nuevo, lanzado
    // mientras termina la animación del anterior, ya parta de la pestaña correcta.
    var shown by remember { mutableIntStateOf(tab) }
    // Desplazamiento del arrastre en px: negativo = hacia la pestaña siguiente.
    var dragX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var settle by remember { mutableStateOf<Job?>(null) }
    // El detector de gestos se crea una vez: que use siempre las funciones más recientes.
    val onTabNow by rememberUpdatedState(onTab)
    val onDragNow by rememberUpdatedState(onDrag)
    fun report() = onDragNow(if (dragX == 0f) null else shown - dragX / width)
    fun commit(newTab: Int) {
        shown = newTab
        onTabNow(newTab)
    }

    fun dragBy(delta: Float) {
        val min = if (shown == TAB_WORLD) 0f else -width.toFloat()
        val max = if (shown == TAB_ALARM) 0f else width.toFloat()
        dragX = (dragX + delta).coerceIn(min, max)
        report()
    }

    // Un deslizamiento nuevo interrumpe la animación del anterior. Si ya había pasado de la mitad,
    // la pestaña vecina pasa a ser la actual sin mover nada en pantalla.
    fun dragStart() {
        settle?.cancel()
        settle = null
        if (dragX < -width / 2f && shown < TAB_WORLD) {
            dragX += width
            commit(shown + 1)
        } else if (dragX > width / 2f && shown > TAB_ALARM) {
            dragX -= width
            commit(shown - 1)
        }
    }

    fun dragStop(velocity: Float) {
        val fast = 1200f
        val target = when {
            shown < TAB_WORLD && (dragX < -width * 0.3f || velocity < -fast && dragX < 0f) -> -width.toFloat()
            shown > TAB_ALARM && (dragX > width * 0.3f || velocity > fast && dragX > 0f) -> width.toFloat()
            else -> 0f
        }
        // En el borde (primera o última pestaña) no hay a dónde ir: se vuelve sin rebote.
        val initialVelocity = if (target == 0f && dragX == 0f) 0f else velocity
        settle = scope.launch {
            animate(dragX, target, initialVelocity, spring(dampingRatio = 1f, stiffness = 500f)) { v, _ ->
                dragX = v
                report()
            }
            if (target != 0f) commit(shown + if (target < 0f) 1 else -1)
            dragX = 0f
            onDragNow(null)
        }
    }

    // Cambio desde fuera (tocar la barra): se corta cualquier animación y se muestra sin más.
    LaunchedEffect(tab) {
        if (tab != shown) {
            settle?.cancel()
            dragX = 0f
            shown = tab
            onDrag(null)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            // Detector propio en vez de Modifier.draggable: cada gesto se procesa por separado y no
            // depende de un bucle interno que, al chocar con el borde de la última pestaña tras una
            // animación, podía quedarse muerto y dejar de reconocer deslizamientos.
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var overSlop = 0f
                    // Solo empieza si el gesto es horizontal y ningún hijo (listas horizontales,
                    // ruedas...) se lo quedó antes.
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        overSlop = over
                    } ?: return@awaitEachGesture
                    dragStart()
                    tracker.resetTracking()
                    tracker.addPosition(start.uptimeMillis, start.position)
                    dragBy(overSlop)
                    val finished = horizontalDrag(start.id) { change ->
                        dragBy(change.positionChange().x)
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                    }
                    dragStop(if (finished) tracker.calculateVelocity().x else 0f)
                }
            },
    ) {
        for (t in TAB_ALARM..TAB_WORLD) {
            val active = t == shown
            // Se ve si es la actual o la vecina que entra mientras se desliza.
            val visible = { active || (t == shown + 1 && dragX < 0f) || (t == shown - 1 && dragX > 0f) }
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(if (active) 1f else 0f)
                    // offset y no graphicsLayer: una capa propia por pestaña encarece el dibujo del cristal.
                    .offset {
                        val x = when {
                            active -> dragX
                            t == shown + 1 && dragX < 0f -> dragX + width
                            t == shown - 1 && dragX > 0f -> dragX - width
                            // Las ocultas, fuera de la pantalla: así ningún toque cae sobre ellas
                            // (p. ej. mientras la actual sale de lado al encadenar deslizamientos).
                            else -> 4f * width
                        }
                        IntOffset(x.roundToInt(), 0)
                    }
                    // Sin llamar a drawContent: ni se graban sus capas ni se calcula su cristal.
                    .drawWithContent { if (visible()) drawContent() }
                    .then(if (active) Modifier else Modifier.clearAndSetSemantics { })
                    // Las pestañas ocultas se tragan los toques para que no lleguen a sus botones
                    // invisibles. Se decide al tocar: la vecina que está entrando sí los recibe, así
                    // se pueden encadenar deslizamientos rápidos sin esperar a que termine el anterior.
                    // La clave [active] rehace el detector cuando cambia la pestaña activa: si no, se
                    // quedaba con el valor del primer momento y bloqueaba la pestaña ya visible.
                    .pointerInput(active) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            if (visible()) return@awaitEachGesture
                            down.consume()
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                CompositionLocalProvider(LocalTabActive provides active) { content(t) }
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
                        "esté bloqueado, la app necesita estos permisos.",
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

/** Iconos de las barras del sistema claros u oscuros según el tema elegido, no el del sistema. */
@Composable
fun SystemBarsFor(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    LaunchedEffect(dark) {
        val style = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
