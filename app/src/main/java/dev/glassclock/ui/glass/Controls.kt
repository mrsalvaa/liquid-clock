package dev.glassclock.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rueda vertical tipo iOS. Con [cyclic] los valores se repiten indefinidamente.
 */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cyclic: Boolean = true,
    itemHeight: Dp = 52.dp,
    visibleItems: Int = 5,
    width: Dp = 84.dp,
    fontSize: Int = 38,
    label: (Int) -> String = { "%02d".format(it) },
) {
    val p = LocalGlass.current
    val total = if (cyclic) count * 1000 else count
    val base = if (cyclic) count * 500 else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = base + value)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val onChange by rememberUpdatedState(onValueChange)

    val centerIndex by remember {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }

    // Notifica al soltar la rueda.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect { onChange(centerIndex % count) }
    }
    // Sincroniza si el valor cambia desde fuera.
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && centerIndex % count != value) {
            val current = centerIndex
            val delta = ((value - current % count) + count) % count
            val target = if (cyclic && delta > count / 2) current + delta - count else current + delta
            state.scrollToItem(if (cyclic) target else value)
        }
    }

    Box(modifier.width(width).height(itemHeight * visibleItems)) {
        // Lente de cristal central.
        GlassSurface(
            Modifier.align(Alignment.Center).fillMaxWidth().height(itemHeight),
            shape = RoundedCornerShape(16.dp),
            strong = true,
            blur = 16.dp,
        ) {}
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(total) { index ->
                val distance = abs(index - centerIndex)
                Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(index % count),
                        fontSize = fontSize.sp,
                        fontWeight = if (distance == 0) FontWeight.Normal else FontWeight.Light,
                        color = if (distance == 0) p.content else p.contentSecondary,
                        modifier = Modifier.graphicsLayer {
                            val s = when (distance) {
                                0 -> 1f
                                1 -> 0.82f
                                else -> 0.68f
                            }
                            scaleX = s
                            scaleY = s
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> 0.6f
                                else -> 0.25f
                            }
                        },
                    )
                }
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

/** Barra inferior flotante con burbuja de selección que se desliza con rebote. */
@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalGlass.current
    GlassSurface(
        modifier.fillMaxWidth().height(70.dp),
        shape = CircleShape,
        strong = true,
        blur = 36.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / items.size
            val x by animateDpAsState(
                itemWidth * selected,
                spring(dampingRatio = 0.62f, stiffness = 420f),
                label = "bubble",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(p.content.copy(alpha = if (p.dark) 0.12f else 0.07f))
                    .border(1.dp, Color.White.copy(alpha = p.rim * 0.5f), CircleShape),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    val active = i == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            item.icon, null, Modifier.size(24.dp),
                            tint = if (active) p.accent else p.contentSecondary,
                        )
                        Text(
                            item.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (active) p.content else p.contentSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Deslizar el pulgar hasta el final para confirmar (descartar alarma). */
@Composable
fun SlideToConfirm(text: String, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalGlass.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val confirm by rememberUpdatedState(onConfirm)
    GlassSurface(modifier.fillMaxWidth().height(76.dp), shape = CircleShape, strong = true) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
            val thumb = 60.dp
            val maxPx = with(LocalDensity.current) { (maxWidth - thumb).toPx() }
            val progress = if (maxPx > 0) offset.value / maxPx else 0f
            Text(
                text,
                Modifier.align(Alignment.Center).graphicsLayer { alpha = 1f - progress },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = p.contentSecondary,
            )
            Box(
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .size(thumb)
                    .clip(CircleShape)
                    .background(p.accent)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxPx)) }
                        },
                        onDragStopped = {
                            if (offset.value > maxPx * 0.8f) {
                                offset.animateTo(maxPx)
                                confirm()
                                offset.snapTo(0f)
                            } else {
                                offset.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White)
            }
        }
    }
}
