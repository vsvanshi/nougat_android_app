package app.nougat.screens

import android.view.HapticFeedbackConstants
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.HeaderAction
import app.nougat.design.ListRow
import app.nougat.design.LocalAccent
import app.nougat.design.LocalColors
import app.nougat.design.LocalOverHeader
import app.nougat.design.Metrics
import app.nougat.design.Page
import app.nougat.design.RowAction
import app.nougat.design.TextButton
import app.nougat.design.Type
import app.nougat.design.rememberReorderState
import app.nougat.design.reorderable
import app.nougat.library.count
import app.nougat.library.search
import app.nougat.playlists.Playlist
import app.nougat.playlists.Playlists
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalPlaylists = staticCompositionLocalOf<Playlists> { error("No playlists") }

/** Opens the "Add to playlist" sheet for these songs. */
val LocalAddToPlaylist = staticCompositionLocalOf<(List<String>) -> Unit> { {} }

/** The circle that leads a playlist row. */
@Composable
fun PlaylistAvatar() {
    val colors = LocalColors.current
    Box(Modifier.size(Metrics.thumbnail).background(colors.avatar, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_library_music), null, tint = colors.onAvatar)
    }
}

/** The accent-coloured "+" row that starts something new: a playlist, or songs for one. */
@Composable
fun NewItemRow(title: String, onTap: () -> Unit) {
    ListRow(title, highlighted = true, onTap = onTap, leading = {
        Box(Modifier.size(Metrics.thumbnail).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_add), null, tint = LocalAccent.current.text)
        }
    })
}

/** A name prompt: new playlist, rename, save preset. */
@Composable
fun NameDialog(title: String, confirm: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Type.title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(confirm, { onDismiss(); onConfirm(name) }) },
        dismissButton = { TextButton("Cancel", onDismiss) },
        containerColor = LocalColors.current.surface,
    )
}

/** Rename and delete for one playlist, shared by the list and the playlist's own screen. */
@Composable
private fun PlaylistEditing(renaming: Playlist?, deleting: Playlist?, onDone: () -> Unit, onDeleted: () -> Unit = {}) {
    val playlists = LocalPlaylists.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    if (renaming != null) NameDialog("Rename playlist", "Rename", renaming.name, onDone) { name ->
        if (!playlists.rename(renaming.id, name)) scope.launch { notices.showSnackbar("That name is already in use") }
    }
    if (deleting != null) AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Delete “${deleting.name}”?", style = Type.title) },
        text = { Text("The songs stay in your library.", style = Type.body) },
        confirmButton = { TextButton("Delete playlist", { onDone(); playlists.delete(deleting.id); onDeleted() }) },
        dismissButton = { TextButton("Cancel", onDone) },
        containerColor = LocalColors.current.surface,
    )
}

/** The Playlists tab: every playlist, and a row to make a new one. */
@Composable
fun PlaylistsScreen(push: (Screen) -> Unit) {
    val playlists = LocalPlaylists.current
    val library = LocalLibrary.current
    val player = LocalPlayer.current
    var naming by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    Page(
        title = "Playlists",
        subtitle = if (playlists.all.isEmpty()) "No playlists" else count(playlists.all.size, "playlist"),
        barActions = { BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) } },
    ) {
        item { NewItemRow("New playlist") { naming = true } }
        items(playlists.sorted, key = { it.id }) { p ->
            val tracks = p.songs.mapNotNull(library::track)
            ListRow(
                p.name, subtitle = count(tracks.size, "song"), leading = { PlaylistAvatar() },
                onTap = { push(Screen.Playlist(p.id)) },
                actions = listOf(
                    RowAction("Play") { player.play(tracks) },
                    RowAction("Shuffle") { player.play(tracks, shuffled = true) },
                    RowAction("Rename") { renaming = p },
                    RowAction("Delete") { deleting = p },
                ),
                titleModifier = Modifier.sharedTitle("playlist:${p.id}"),
            )
        }
    }
    if (naming) NameDialog("New playlist", "Create", onDismiss = { naming = false }) { playlists.create(it) }
    PlaylistEditing(renaming, deleting, onDone = { renaming = null; deleting = null })
}

/** One playlist: its songs in the user's order. Long-press and drag to reorder, swipe to remove. */
@Composable
fun PlaylistScreen(id: String, push: (Screen) -> Unit, back: () -> Unit) {
    val playlists = LocalPlaylists.current
    val library = LocalLibrary.current
    val player = LocalPlayer.current
    val notices = LocalNotices.current
    val addTo = LocalAddToPlaylist.current
    val scope = rememberCoroutineScope()
    val playlist = playlists[id]
    if (playlist == null) {
        // Deleted while this screen was open.
        LaunchedEffect(Unit) { back() }
        return
    }
    // Songs whose files are missing stay in the playlist but are not shown (D41).
    val tracks = playlist.songs.mapNotNull(library::track)
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var menu by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val reorder = rememberReorderState(list, first = 1) { tracks.size }
    fun remove(path: String) {
        playlists.remove(listOf(path), id)
        scope.launch {
            if (notices.showSnackbar("Removed from “${playlist.name}”", "Undo") == SnackbarResult.ActionPerformed) {
                playlists.add(listOf(path), id)
            }
        }
    }

    Page(
        title = playlist.name,
        subtitle = if (tracks.isEmpty()) "No songs" else "${count(tracks.size, "song")}, ${maxOf(1, (tracks.sumOf { it.durationMs } / 60_000.0).roundToInt())} min",
        action = if (tracks.isEmpty()) null else HeaderAction("Shuffle", R.drawable.ic_shuffle) { player.play(tracks, shuffled = true) },
        firstSubheader = if (tracks.isEmpty()) null else "Your order",
        titleModifier = Modifier.sharedTitle("playlist:$id"),
        navigationIcon = { BarIcon(R.drawable.ic_arrow_back, "Back", back) },
        barActions = {
            Box {
                BarIcon(R.drawable.ic_more_vert, "More") { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = LocalColors.current.surface) {
                    for ((label, action) in listOfNotNull(
                        if (tracks.isNotEmpty()) "Play" to { player.play(tracks) } else null,
                        "Rename" to { renaming = playlist },
                        "Delete playlist" to { deleting = playlist },
                    )) DropdownMenuItem(text = { Text(label, style = Type.rowTitle, color = LocalColors.current.ink) }, onClick = { menu = false; action() })
                }
            }
        },
        list = list,
    ) {
        items(tracks, key = { it.path }) { track ->
            val swipe = rememberSwipeToDismissBoxState()
            LaunchedEffect(swipe.currentValue) { if (swipe.currentValue != SwipeToDismissBoxValue.Settled) remove(track.path) }
            SwipeToDismissBox(
                swipe, backgroundContent = { SwipeBackground("Remove") },
                modifier = Modifier.reorderable(reorder, track.path, { tracks.indexOfFirst { it.path == track.path } }) { from, to ->
                    playlists.move(id, playlists[id]?.songs.orEmpty().mapNotNull(library::track).map { it.path }, from, to)
                },
            ) {
                Box(Modifier.background(LocalColors.current.paper)) {
                    TrackRow(
                        track, { player.play(tracks, track) },
                        listOf(RowAction("Add to playlist") { addTo(listOf(track.path)) }, RowAction("Remove from playlist") { remove(track.path) }),
                        menuOnLongPress = false,
                    )
                }
            }
        }
        item { NewItemRow("Add songs") { push(Screen.SongPicker(id)) } }
    }
    PlaylistEditing(renaming, deleting, onDone = { renaming = null; deleting = null }, onDeleted = back)
}

/** What a swipe reveals behind a row. */
@Composable
fun SwipeBackground(label: String) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(accent.fill).padding(horizontal = Metrics.margin), contentAlignment = Alignment.CenterEnd) {
        Text(label.uppercase(), style = Type.button, color = accent.onAccent)
    }
}

/** Picks several songs for a playlist, with search. Songs already in it are shown but cannot be picked. */
@Composable
fun SongPickerScreen(id: String, back: () -> Unit) {
    val colors = LocalColors.current
    val library = LocalLibrary.current
    val playlists = LocalPlaylists.current
    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = false }
    var query by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    val already = playlists[id]?.songs.orEmpty().toSet()
    // Both are already in title order.
    val tracks = remember(library.tracks, query) {
        (if (query.isBlank()) library.tracksByTitle else library.tracks.search(query).tracks).filter { it.isPlayable }
    }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(Modifier.statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(R.drawable.ic_close, "Cancel", back)
                Text("Add songs", style = Type.barTitle, color = colors.ink, modifier = Modifier.padding(start = Metrics.margin).weight(1f))
                if (chosen.isNotEmpty()) TextButton("Add ${chosen.size}", {
                    // In the order shown, not the order tapped.
                    playlists.add(tracks.map { it.path }.filter { it in chosen }, id)
                    back()
                })
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Metrics.margin, vertical = Metrics.grid).background(colors.fill, androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).padding(horizontal = Metrics.margin, vertical = 12.dp)) {
            if (query.isEmpty()) Text("Songs, artists, albums", style = Type.rowTitle, color = colors.ink3)
            BasicTextField(query, { query = it }, Modifier.fillMaxWidth(), textStyle = Type.rowTitle.copy(color = colors.ink), singleLine = true, cursorBrush = SolidColor(colors.ink))
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            items(tracks, key = { it.path }) { track ->
                val inPlaylist = track.path in already
                val picked = track.path in chosen
                Box(Modifier.semantics { selected = picked }) {
                    ListRow(
                        track.title, subtitle = if (inPlaylist) "Already in this playlist" else track.artist ?: "Unknown artist",
                        enabled = !inPlaylist, onTap = { chosen = if (picked) chosen - track.path else chosen + track.path },
                        leading = { TrackThumbnail(track) },
                    )
                    if (picked) Icon(
                        painterResource(R.drawable.ic_check), null, tint = LocalAccent.current.text,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = Metrics.margin),
                    )
                }
            }
        }
    }
}

/** Chooses a playlist for some songs, or makes a new one for them. Says what happened, with Undo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(paths: List<String>, onDismiss: () -> Unit) {
    val playlists = LocalPlaylists.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf(false) }
    fun add(to: Playlist) {
        val added = playlists.add(paths, to.id)
        onDismiss()
        scope.launch {
            if (added.isEmpty()) notices.showSnackbar("Already in “${to.name}”")
            else {
                val what = if (added.size == 1) "Added" else "Added ${count(added.size, "song")}"
                if (notices.showSnackbar("$what to “${to.name}”", "Undo") == SnackbarResult.ActionPerformed) playlists.remove(added, to.id)
            }
        }
    }
    ModalBottomSheet(onDismiss, containerColor = LocalColors.current.paper) {
        Text("Add to playlist", style = Type.barTitle, color = LocalColors.current.ink, modifier = Modifier.padding(horizontal = Metrics.margin, vertical = Metrics.grid))
        LazyColumn(Modifier.navigationBarsPadding()) {
            item { NewItemRow("New playlist") { naming = true } }
            items(playlists.sorted, key = { it.id }) { p ->
                ListRow(p.name, subtitle = count(p.songs.size, "song"), onTap = { add(p) }, leading = { PlaylistAvatar() })
            }
            item { Spacer(Modifier.height(Metrics.margin)) }
        }
    }
    if (naming) NameDialog("New playlist", "Create", onDismiss = { naming = false }) { name -> playlists.create(name)?.let(::add) }
}

/** The success haptic when songs are added to a playlist (D57). */
@Composable
fun AdditionHaptic() {
    val playlists = LocalPlaylists.current
    val view = LocalView.current
    val start = remember { playlists.additions }
    LaunchedEffect(playlists.additions) {
        if (playlists.additions != start) {
            view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
