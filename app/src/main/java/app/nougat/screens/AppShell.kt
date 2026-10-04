package app.nougat.screens

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import app.nougat.App
import app.nougat.R
import app.nougat.design.Accent
import app.nougat.design.Gallery
import app.nougat.design.LocalAccent
import app.nougat.design.LocalColors
import app.nougat.design.LocalOverHeader
import app.nougat.design.Motion
import app.nougat.design.NoticeHost
import app.nougat.design.Type
import app.nougat.design.rememberReducedMotion
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** A place in a tab's back stack. */
sealed interface Screen {
    data class Folder(val path: String) : Screen
    data object Songs : Screen
    data object Playlists : Screen
    data object Search : Screen
    data object Gallery : Screen
    data object Hidden : Screen
    data class Playlist(val id: String) : Screen
    data class SongPicker(val playlist: String) : Screen
    data object Settings : Screen
    data class Licence(val name: String) : Screen
}

enum class Tab(val label: String, @DrawableRes val icon: Int, val root: Screen) {
    Folders("Folders", R.drawable.ic_folder, Screen.Folder("")),
    Songs("Songs", R.drawable.ic_music_note, Screen.Songs),
    Playlists("Playlists", R.drawable.ic_queue_music, Screen.Playlists),
}

/** Snackbars for the whole app; they float above the mini player. */
val LocalNotices = staticCompositionLocalOf { SnackbarHostState() }

/**
 * The app's chrome: bottom navigation with a back stack per tab, the mini player above it, and
 * Now playing over everything. System back pops the current tab; from a tab's root it leaves the app.
 */
@Composable
fun AppShell(accent: Accent, onAccent: (Accent) -> Unit) {
    val colors = LocalColors.current
    val context = LocalContext.current
    val still = rememberReducedMotion()
    val app = context.applicationContext as App
    val library = app.library
    val artwork = app.artwork
    val player = app.player
    val playlists = app.playlists
    var adding by remember { mutableStateOf<List<String>?>(null) }
    var bottomBars by remember { mutableIntStateOf(0) }
    // Follow MediaStore while the app is in front, and read again on every return, which also
    // picks up a permission granted in Settings.
    LifecycleResumeEffect(library) {
        val scope = MainScope()
        library.follow(scope)
        onPauseOrDispose {
            scope.cancel()
            player.save() // the position, when the app goes to the background (D35)
        }
    }
    val notices = remember { SnackbarHostState() }
    val overHeader = remember { mutableStateOf(true) }
    // Now playing's own answer: its cover is dark, its queue is paper.
    val nowPlayingOverHeader = remember { mutableStateOf(true) }
    var tab by rememberSaveable { mutableStateOf(Tab.Folders) }
    val stacks = rememberSaveable(saver = StacksSaver) { Tab.entries.map { mutableStateListOf(it.root) } }
    var nowPlaying by rememberSaveable { mutableStateOf(false) }
    val tabs = rememberSaveableStateHolder()
    val stack = stacks[tab.ordinal]

    // Status-bar icons: light over the header colour (and over Now playing's cover), else the theme's.
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    LaunchedEffect(dark, nowPlaying) {
        val window = (view.context as Activity).window
        snapshotFlow { if (nowPlaying) nowPlayingOverHeader.value else overHeader.value }.collect { over ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !over && !dark
        }
    }

    CompositionLocalProvider(LocalNotices provides notices, LocalOverHeader provides overHeader, LocalLibrary provides library, LocalArtwork provides artwork, LocalPlayer provides player, LocalPlaylists provides playlists, LocalEqualizer provides app.equalizer, LocalAddToPlaylist provides { paths: List<String> -> if (paths.isNotEmpty()) adding = paths }) {
        AdditionHaptic()
        Box(Modifier.fillMaxSize().background(colors.paper)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    tabs.SaveableStateProvider(tab.name) {
                        SharedTransitionLayout(Modifier.swipeBack(stack.size > 1)) {
                            CompositionLocalProvider(LocalSharedScope provides this) {
                                NavDisplay(
                                    backStack = stack,
                                    onBack = { stack.removeLastOrNull() },
                                    sharedTransitionScope = this,
                                    transitionSpec = { if (still) fade else push },
                                    popTransitionSpec = { if (still) fade else pop },
                                    predictivePopTransitionSpec = { if (still) fade else pop },
                                    entryProvider = entryProvider {
                                        val push = { s: Screen -> stack.add(s); Unit }
                                        val pop = { stack.removeLastOrNull(); Unit }
                                        // Decided by the entry, not the stack size, so the page underneath keeps its bar while another slides in.
                                        entry<Screen.Folder> { FolderScreen(it.path, push, if (it.path.isEmpty()) null else pop) }
                                        entry<Screen.Songs> { SongsScreen(push) }
                                        entry<Screen.Playlists> { PlaylistsScreen(push) }
                                        entry<Screen.Search> { SearchScreen(push, pop) }
                                        entry<Screen.Hidden> { HiddenScreen(pop) }
                                        entry<Screen.Playlist> { PlaylistScreen(it.id, push, pop) }
                                        entry<Screen.SongPicker> { SongPickerScreen(it.playlist, pop) }
                                        entry<Screen.Settings> { SettingsScreen(accent, onAccent, push, pop) }
                                        entry<Screen.Licence> { LicenceScreen(it.name, pop) }
                                        entry<Screen.Gallery> { Gallery(accent, onAccent, pop) }
                                    },
                                )
                            }
                        }
                    }
                }
                Column(Modifier.onSizeChanged { bottomBars = it.height }) {
                    MiniPlayer(onOpen = { nowPlaying = true })
                    NavigationBar(containerColor = colors.surface) {
                        for (t in Tab.entries) {
                            NavigationBarItem(
                                selected = t == tab,
                                // Tapping the tab that is showing goes back to its root.
                                onClick = { if (t == tab) stack.removeRange(1, stack.size) else tab = t },
                                icon = { Icon(painterResource(t.icon), contentDescription = null) },
                                label = { Text(t.label, style = Type.caption) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LocalAccent.current.text, selectedTextColor = LocalAccent.current.text,
                                    unselectedIconColor = colors.ink2, unselectedTextColor = colors.ink2,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                nowPlaying,
                enter = if (still) fadeIn(tween(Motion.fade)) else slideInVertically(tween(Motion.entering, easing = Motion.easing)) { it },
                exit = if (still) fadeOut(tween(Motion.fade)) else slideOutVertically(tween(Motion.leaving, easing = Motion.easing)) { it },
            ) {
                NowPlayingScreen(nowPlayingOverHeader, onClose = { nowPlaying = false })
            }
            // Snackbars float above the mini player and bottom navigation, and over Now playing too.
            val above = with(LocalDensity.current) { if (nowPlaying) 0.dp else bottomBars.toDp() }
            NoticeHost(notices, Modifier.align(Alignment.BottomCenter).padding(bottom = above).then(if (nowPlaying) Modifier.navigationBarsPadding() else Modifier))
            adding?.let { AddToPlaylistSheet(it) { adding = null } }
        }
    }
}

/**
 * Opening a page slides it in from the right over the old one, which drifts a quarter of the way
 * left. Going back is the reverse, with the old page underneath; a back gesture or a swipe drives it.
 */
private val push = ContentTransform(
    slideInHorizontally(tween(Motion.standard, easing = Motion.easing)) { it },
    slideOutHorizontally(tween(Motion.standard, easing = Motion.easing)) { -it / 4 },
)
private val pop = ContentTransform(
    slideInHorizontally(tween(Motion.standard, easing = Motion.easing)) { -it / 4 },
    slideOutHorizontally(tween(Motion.standard, easing = Motion.easing)) { it },
    targetContentZIndex = -1f,
)

/** With animations removed, pages change with a short fade. */
private val fade = ContentTransform(fadeIn(tween(Motion.fade)), fadeOut(tween(Motion.fade)))

val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * Marks a title that flies between screens: a folder's name in its row grows into the large title
 * of the folder's page, and shrinks back on the way out. Rows and headers with the same key pair up.
 */
@Composable
fun Modifier.sharedTitle(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val scope = LocalNavAnimatedContentScope.current
    return with(shared) {
        sharedBounds(
            rememberSharedContentState(key), scope,
            enter = fadeIn(tween(0)), exit = fadeOut(tween(0)),
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.CenterStart),
        )
    }
}

/**
 * Drag a pushed page to the right, from anywhere on it, to go back. The drag is fed to the system's
 * back events, so it runs the same animation as the predictive back gesture and can be let go halfway.
 */
@Composable
private fun Modifier.swipeBack(enabled: Boolean): Modifier {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return this
    val input = remember { DirectNavigationEventInput() }
    DisposableEffect(dispatcher) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
    if (!enabled) return this
    return pointerInput(Unit) {
        var dx = 0f
        var started = false
        val velocity = VelocityTracker()
        fun event(x: Float, y: Float) = NavigationEvent(NavigationEvent.EDGE_LEFT, (dx / size.width).coerceIn(0f, 1f), x, y)
        detectHorizontalDragGestures(
            onDragStart = { dx = 0f; started = false; velocity.resetTracking() },
            onDragEnd = {
                if (started) {
                    if (dx > size.width / 3f || velocity.calculateVelocity().x > 1500f) input.backCompleted() else input.backCancelled()
                }
                started = false
            },
            onDragCancel = { if (started) input.backCancelled(); started = false },
        ) { change, amount ->
            velocity.addPosition(change.uptimeMillis, change.position)
            dx = (dx + amount).coerceAtLeast(0f)
            if (!started && dx > 0f) {
                input.backStarted(event(change.position.x, change.position.y))
                started = true
            }
            if (started) {
                input.backProgressed(event(change.position.x, change.position.y))
                change.consume()
            }
        }
    }
}

private fun Screen.encode() = when (this) {
    is Screen.Folder -> "folder:$path"
    Screen.Songs -> "songs"
    Screen.Playlists -> "playlists"
    Screen.Search -> "search"
    Screen.Gallery -> "gallery"
    Screen.Hidden -> "hidden"
    is Screen.Playlist -> "playlist:$id"
    is Screen.SongPicker -> "picker:$playlist"
    Screen.Settings -> "settings"
    is Screen.Licence -> "licence:$name"
}

private fun decode(s: String) = when (s) {
    "songs" -> Screen.Songs
    "playlists" -> Screen.Playlists
    "search" -> Screen.Search
    "gallery" -> Screen.Gallery
    "hidden" -> Screen.Hidden
    "settings" -> Screen.Settings
    else -> when {
        s.startsWith("playlist:") -> Screen.Playlist(s.removePrefix("playlist:"))
        s.startsWith("picker:") -> Screen.SongPicker(s.removePrefix("picker:"))
        s.startsWith("licence:") -> Screen.Licence(s.removePrefix("licence:"))
        else -> Screen.Folder(s.removePrefix("folder:"))
    }
}

/** Keeps every tab's back stack across a configuration change or the process being stopped. */
private val StacksSaver = Saver<List<SnapshotStateList<Screen>>, ArrayList<ArrayList<String>>>(
    save = { stacks -> ArrayList(stacks.map { stack -> ArrayList(stack.map { it.encode() }) }) },
    restore = { saved -> saved.map { stack -> mutableStateListOf<Screen>().apply { addAll(stack.map(::decode)) } } },
)
