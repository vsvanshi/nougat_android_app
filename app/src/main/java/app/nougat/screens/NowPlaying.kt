package app.nougat.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.BarIcon
import app.nougat.design.Depth
import app.nougat.design.LetterTile
import app.nougat.design.LocalColors
import app.nougat.design.Metrics
import app.nougat.design.PlayButton
import app.nougat.design.Slider
import app.nougat.design.ToggleIcon
import app.nougat.design.Type
import app.nougat.design.depth
import app.nougat.design.pressable
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Sample song until playback arrives in phase 3.
private const val TITLE = "Kite Season"
private const val ARTIST = "Anouk Verma"

/**
 * Our own bar above the bottom navigation: tap opens Now playing. Play/pause and next are as large
 * as the bar allows (D45): 34 and 30 dp glyphs, each in a 56 dp wide tap area.
 */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val colors = LocalColors.current
    var playing by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.depth(Depth.One, RoundedCornerShape(0.dp)).background(colors.surface).fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxSize().pressable(RoundedCornerShape(0.dp), onOpen).padding(start = Metrics.margin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterTile(TITLE)
            Column(Modifier.padding(start = 12.dp)) {
                Text(TITLE, style = Type.rowTitle, color = colors.ink, maxLines = 1)
                Text(ARTIST, style = Type.body, color = colors.ink2, maxLines = 1)
            }
        }
        MiniButton(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, if (playing) "Pause" else "Play", 34.dp) { playing = !playing }
        MiniButton(R.drawable.ic_skip_next, "Next", 30.dp) {}
    }
}

@Composable
private fun MiniButton(@DrawableRes icon: Int, label: String, glyph: Dp, onClick: () -> Unit) {
    Box(
        Modifier.width(56.dp).fillMaxSize().pressable(RoundedCornerShape(0.dp), onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = LocalColors.current.ink, modifier = Modifier.size(glyph))
    }
}

/**
 * Full screen over the tabs. System back and predictive back close it (the screen shrinks as the
 * back gesture is dragged), and so do the close button and a swipe down.
 */
@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
    val colors = LocalColors.current
    val scope = rememberCoroutineScope()
    var backProgress by remember { mutableFloatStateOf(0f) }
    val drag = remember { Animatable(0f) }
    val closeAt = with(LocalDensity.current) { 160.dp.toPx() }

    PredictiveBackHandler { events ->
        try {
            events.collect { backProgress = it.progress }
            onClose()
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }

    Column(
        Modifier.fillMaxSize()
            .graphicsLayer {
                val scale = 1f - 0.1f * backProgress
                scaleX = scale
                scaleY = scale
                translationY = drag.value
                shape = RoundedCornerShape((32 * backProgress).dp)
                clip = backProgress > 0f
            }
            .background(colors.paper)
            .draggable(
                rememberDraggableState { dy -> scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) } },
                Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (drag.value > closeAt || velocity > 2000f) onClose() else drag.animateTo(0f)
                },
            ),
    ) {
        // Cover area: the header colour with the title's letter until artwork and the visualizer arrive.
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(colors.header)) {
            Text(
                TITLE.take(1), style = Type.largeTitle.copy(fontSize = Type.largeTitle.fontSize * 3), color = colors.onHeader2,
                modifier = Modifier.align(Alignment.Center),
            )
            CompositionLocalProvider(LocalContentColor provides colors.onHeader) {
                Row(Modifier.statusBarsPadding().padding(4.dp)) {
                    BarIcon(R.drawable.ic_expand_more, "Close", onClose)
                }
            }
        }
        Column(Modifier.padding(horizontal = Metrics.margin)) {
            Text(TITLE, style = Type.title, color = colors.ink, modifier = Modifier.padding(top = Metrics.margin))
            Text("$ARTIST, Road Trip 2016", style = Type.body, color = colors.ink2)
            var position by remember { mutableFloatStateOf(0.37f) }
            Slider(position, { position = it }, "Position", "${(position * 178).roundToInt() / 60}:%02d".format((position * 178).roundToInt() % 60), Modifier.padding(top = Metrics.grid))
            Controls()
        }
    }
}

@Composable
private fun Controls() {
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var playing by rememberSaveable { mutableStateOf(false) }
    val ink = LocalColors.current.ink
    Row(
        Modifier.fillMaxWidth().padding(top = Metrics.grid),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleIcon(R.drawable.ic_shuffle, "Shuffle", shuffle, { shuffle = it })
        CompositionLocalProvider(LocalContentColor provides ink) { BarIcon(R.drawable.ic_skip_previous, "Previous") {} }
        PlayButton(
            if (playing) "Pause" else "Play", { playing = !playing },
            icon = if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow, size = Metrics.nowPlayingPlayButton,
        )
        CompositionLocalProvider(LocalContentColor provides ink) { BarIcon(R.drawable.ic_skip_next, "Next") {} }
        ToggleIcon(R.drawable.ic_repeat, "Repeat", repeat, { repeat = it })
    }
}
