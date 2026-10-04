package app.nougat.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import app.nougat.design.rememberReducedMotion
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.nougat.R
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.Depth
import app.nougat.design.LetterTile
import app.nougat.design.ListRow
import app.nougat.design.LocalAccent
import app.nougat.design.LocalColors
import app.nougat.design.Metrics
import app.nougat.design.Motion
import app.nougat.design.PlayButton
import app.nougat.design.Slider
import app.nougat.design.ToggleIcon
import app.nougat.design.Type
import app.nougat.design.depth
import app.nougat.design.pressable
import app.nougat.design.rememberReorderState
import app.nougat.design.reorderable
import app.nougat.library.Track
import app.nougat.library.playbackTime
import app.nougat.playback.Player
import app.nougat.playback.RepeatMode
import app.nougat.visualizer.Visualizer
import app.nougat.visualizer.VisualizerStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalPlayer = staticCompositionLocalOf<Player> { error("No player") }

/**
 * Our own bar above the bottom navigation, shown once there is a song: tap opens Now playing.
 * Play/pause and next are as large as the bar allows (D45): 34 and 30 dp glyphs in 56 dp wide tap areas.
 */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val colors = LocalColors.current
    val player = LocalPlayer.current
    val track = player.current ?: return
    Row(
        Modifier.depth(Depth.One, RoundedCornerShape(0.dp)).background(colors.surface).fillMaxWidth().heightIn(min = 64.dp).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxSize().pressable(RoundedCornerShape(0.dp), onOpen).padding(start = Metrics.margin)
                .semantics(mergeDescendants = true) { onClick(label = "Open Now playing") { onOpen(); true } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackThumbnail(track)
            Column(Modifier.padding(start = 12.dp)) {
                Text(track.title, style = Type.rowTitle, color = colors.ink, maxLines = 1)
                Text(track.artist ?: "Unknown artist", style = Type.body, color = colors.ink2, maxLines = 1)
            }
        }
        MiniButton(if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow, if (player.isPlaying) "Pause" else "Play", 34.dp) { player.toggle() }
        MiniButton(R.drawable.ic_skip_next, "Next", 30.dp) { player.next() }
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
 * back gesture is dragged), and so do the close button and a swipe down on the cover.
 */
@Composable
fun NowPlayingScreen(overHeader: MutableState<Boolean>, onClose: () -> Unit) {
    val colors = LocalColors.current
    val player = LocalPlayer.current
    val scope = rememberCoroutineScope()
    var backProgress by remember { mutableFloatStateOf(0f) }
    val drag = remember { Animatable(0f) }
    val closeAt = with(LocalDensity.current) { 160.dp.toPx() }
    val still = rememberReducedMotion()
    var queueShown by remember { mutableStateOf(false) }
    var equalizerShown by remember { mutableStateOf(false) }
    SideEffect { overHeader.value = !queueShown && !equalizerShown }
    // The music is analysed only while the visualizer can be seen (D50).
    LifecycleResumeEffect(player) {
        player.isVisualizing = true
        onPauseOrDispose { player.isVisualizing = false }
    }

    PredictiveBackHandler { events ->
        try {
            events.collect { backProgress = it.progress }
            onClose()
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }
    // Nothing left to show, for example when every queued song was deleted.
    if (player.current == null) LaunchedEffect(Unit) { onClose() }

    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val scale = 1f - 0.1f * backProgress
            scaleX = scale
            scaleY = scale
            translationY = drag.value
            shape = RoundedCornerShape((32 * backProgress).dp)
            clip = backProgress > 0f
        },
    ) {
        Column(Modifier.fillMaxSize().background(colors.paper)) {
            Cover(
                Modifier.draggable(
                    rememberDraggableState { dy -> scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) } },
                    Orientation.Vertical,
                    onDragStopped = { velocity -> if (drag.value > closeAt || velocity > 2000f) onClose() else drag.animateTo(0f) },
                ),
                onClose = onClose, onQueue = { queueShown = true },
            )
            player.current?.let { Details(it, onEqualizer = { equalizerShown = true }) }
        }
        AnimatedVisibility(queueShown, enter = overlayIn(still), exit = overlayOut(still)) {
            BackHandler { queueShown = false }
            QueueScreen(onBack = { queueShown = false })
        }
        AnimatedVisibility(equalizerShown, enter = overlayIn(still), exit = overlayOut(still)) {
            BackHandler { equalizerShown = false }
            EqualizerScreen(onBack = { equalizerShown = false })
        }
    }
}

/** Queue and Equalizer slide in over Now playing; with animations removed, they fade. */
private fun overlayIn(still: Boolean) = if (still) fadeIn(tween(Motion.fade)) else slideInHorizontally(tween(Motion.standard)) { it }
private fun overlayOut(still: Boolean) = if (still) fadeOut(tween(Motion.fade)) else slideOutHorizontally(tween(Motion.standard)) { it }

/**
 * The cover with the visualizer: a low strip over a cover, or the whole area on the header colour
 * when there is none (D50). Tapping steps through the looks, which are remembered (D52).
 */
@Composable
private fun Cover(modifier: Modifier, onClose: () -> Unit, onQueue: () -> Unit) {
    val colors = LocalColors.current
    val accent = LocalAccent.current
    val player = LocalPlayer.current
    val store = LocalArtwork.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nougat", Context.MODE_PRIVATE) }
    var style by remember { mutableStateOf(VisualizerStyle.entries.firstOrNull { it.name == prefs.getString("visualizerStyle", null) } ?: VisualizerStyle.Spectrum) }
    var nameShown by remember { mutableStateOf(0) }
    val track = player.current
    // The last cover stays until the next song's is known; `checked` keeps the bars from flashing up before a cover.
    var checked by remember { mutableStateOf(false) }
    val cover by produceState<ImageBitmap?>(null, track?.path) {
        value = track?.let { store.fullImage(it)?.asImageBitmap() }
        checked = true
    }
    LaunchedEffect(nameShown) { if (nameShown > 0) { delay(1500); nameShown = 0 } }

    Box(
        modifier.fillMaxWidth().aspectRatio(1f).background(colors.header).pointerInput(Unit) {
            detectTapGestures {
                style = if (cover == null) style.next else style.nextStrip
                prefs.edit().putString("visualizerStyle", style.name).apply()
                nameShown++
            }
        },
    ) {
        val image = cover
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize().clearAndSetSemantics {}, contentScale = ContentScale.Crop)
            // Over a cover: a low strip on a scrim.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(128.dp).background(Brush.verticalGradient(listOf(colors.header.copy(alpha = 0f), colors.header.copy(alpha = 0.85f)))))
            Visualizer(
                player.spectrum, player.isPlaying, style, colors.onHeader, colors.onHeader.copy(alpha = 0.6f),
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp).padding(horizontal = Metrics.margin).padding(bottom = 0.dp).offset(y = (-12).dp),
            )
        } else if (checked) {
            // No cover: the visualizer is the picture. It stays clear of the buttons at the top.
            Visualizer(
                player.spectrum, player.isPlaying, style, accent.accent, colors.onHeader,
                Modifier.fillMaxSize().padding(start = Metrics.margin, end = Metrics.margin, top = 120.dp, bottom = Metrics.margin),
                stage = true, title = track?.title.orEmpty(), progress = { player.roughProgress },
            )
        }
        // A scrim keeps the status bar and the buttons readable over any cover.
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(colors.header.copy(alpha = 0.6f), colors.header.copy(alpha = 0f)))))
        if (nameShown > 0) Text(
            (if (cover == null) style else style.strip).title, style = Type.bodyStrong, color = colors.onHeader,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp).background(colors.header.copy(alpha = 0.7f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        CompositionLocalProvider(LocalContentColor provides colors.onHeader) {
            Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp).fillMaxWidth().height(BarHeight), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(R.drawable.ic_expand_more, "Close", onClose)
                Spacer(Modifier.weight(1f))
                BarIcon(R.drawable.ic_queue_music, "Queue", onQueue)
            }
        }
    }
}

/** Title, seek bar and controls. */
@Composable
private fun ColumnScope.Details(track: Track, onEqualizer: () -> Unit) {
    val colors = LocalColors.current
    val player = LocalPlayer.current
    val addTo = LocalAddToPlaylist.current
    val equalizer = LocalEqualizer.current
    // Scrolls rather than clips at the largest text sizes; the equalizer shortcut stays at the bottom.
    Column(Modifier.weight(1f)) {
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Metrics.margin)) {
        Row(Modifier.padding(top = Metrics.margin), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(track.title, style = Type.title, color = colors.ink, maxLines = 2)
                val folder = track.path.substringBeforeLast('/', "").substringAfterLast('/')
                Text(listOfNotNull(track.artist ?: "Unknown artist", folder.ifEmpty { null }).joinToString(", "), style = Type.body, color = colors.ink2, maxLines = 2)
            }
            CompositionLocalProvider(LocalContentColor provides colors.ink2) {
                BarIcon(R.drawable.ic_playlist_add, "Add to playlist") { addTo(listOf(track.path)) }
            }
        }
        SeekBar()
        Row(
            Modifier.fillMaxWidth().padding(top = Metrics.margin),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToggleIcon(R.drawable.ic_shuffle, "Shuffle", player.isShuffled, { player.toggleShuffle() })
            ControlIcon(R.drawable.ic_skip_previous, "Previous") { player.previous() }
            PlayButton(
                if (player.isPlaying) "Pause" else "Play", { player.toggle() },
                icon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow, size = Metrics.nowPlayingPlayButton,
            )
            ControlIcon(R.drawable.ic_skip_next, "Next") { player.next() }
            ToggleIcon(
                if (player.repeatMode == RepeatMode.One) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                "Repeat ${player.repeatMode.name.lowercase()}", player.repeatMode != RepeatMode.Off, { player.cycleRepeat() },
            )
        }
    }
        Spacer(Modifier.weight(1f).height(Metrics.margin))
        // The shortcut names the preset in use, or just says "Equalizer" when it is off.
        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = Metrics.grid).navigationBarsPadding()
                .pressable(RoundedCornerShape(50), onEqualizer).padding(horizontal = Metrics.margin, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_equalizer), null, tint = LocalAccent.current.text, modifier = Modifier.size(20.dp))
            Text(
                (if (equalizer.state.isOn) equalizer.state.presetName else "Equalizer").uppercase(), style = Type.button,
                color = LocalAccent.current.text, modifier = Modifier.padding(start = Metrics.grid),
            )
        }
    }
}

@Composable
private fun ControlIcon(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(Metrics.touchTarget).pressable(androidx.compose.foundation.shape.CircleShape, onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = LocalColors.current.ink, modifier = Modifier.size(36.dp))
    }
}

/** The slider follows the song twice a second; a drag moves playback when the finger lifts. */
@Composable
private fun SeekBar() {
    val player = LocalPlayer.current
    var position by remember { mutableLongStateOf(player.positionMs) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(player.current?.path) {
        while (true) {
            position = player.positionMs
            delay(500)
        }
    }
    val duration = player.durationMs.coerceAtLeast(1)
    val shown = scrub?.toLong() ?: position
    Column(Modifier.padding(top = Metrics.grid)) {
        Slider(
            shown.toFloat(), { scrub = it }, "Position", "${playbackTime(shown)} of ${playbackTime(duration)}",
            range = 0f..duration.toFloat(),
            onEditingChanged = { editing -> if (!editing) scrub?.let { player.seek(it.toLong()); position = it.toLong(); scrub = null } },
        )
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            Text(playbackTime(shown), style = Type.caption, color = LocalColors.current.ink2)
            Spacer(Modifier.weight(1f))
            Text(playbackTime(duration), style = Type.caption, color = LocalColors.current.ink2)
        }
    }
}

/**
 * What plays next. Tap to jump, long-press and drag to reorder, swipe sideways to remove.
 * Compose has no list reordering of its own, so the drag is done here.
 */
@Composable
private fun QueueScreen(onBack: () -> Unit) {
    val colors = LocalColors.current
    val player = LocalPlayer.current
    val library = LocalLibrary.current
    val list = rememberLazyListState()
    val items = player.items
    val unique = items.toSet().size == items.size
    val reorder = rememberReorderState(list) { player.items.size }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(Modifier.statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(R.drawable.ic_arrow_back, "Back", onBack)
                Text("Queue", style = Type.barTitle, color = colors.ink, modifier = Modifier.padding(start = Metrics.margin))
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            state = list,
        ) {
            itemsIndexed(items, key = { i, path -> if (unique) path else "$i" }) { position, path ->
                val track = library.track(path) ?: return@itemsIndexed
                val swipe = rememberSwipeToDismissBoxState()
                LaunchedEffect(swipe.currentValue) {
                    if (swipe.currentValue != SwipeToDismissBoxValue.Settled) player.items.indexOf(path).takeIf { it >= 0 }?.let(player::remove)
                }
                SwipeToDismissBox(
                    swipe, backgroundContent = { Box(Modifier.fillMaxSize().background(colors.fill)) },
                    modifier = Modifier.reorderable(reorder, path, { player.items.indexOf(path) }, player::move)
                        .animateItem(placementSpec = if (reorder.dragged == position) null else tween(Motion.standard)),
                ) {
                    // No menu here: a long press starts a drag. TalkBack gets the same edits as actions.
                    Box(
                        Modifier.background(colors.paper).semantics {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction("Remove from queue") { player.remove(position); true },
                                if (position > 0) CustomAccessibilityAction("Move up") { player.move(position, position - 1); true } else null,
                                if (position < items.size - 1) CustomAccessibilityAction("Move down") { player.move(position, position + 1); true } else null,
                            )
                        },
                    ) {
                        ListRow(
                            track.title, subtitle = track.artist ?: "Unknown artist", detail = playbackTime(track.durationMs),
                            highlighted = position == player.index, spokenState = if (position == player.index) "Playing" else null,
                            onTap = { player.jump(position) }, leading = { TrackThumbnail(track) },
                        )
                    }
                }
            }
        }
    }
}

