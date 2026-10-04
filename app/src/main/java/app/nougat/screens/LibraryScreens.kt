package app.nougat.screens

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.EmptyState
import app.nougat.design.FolderAvatar
import app.nougat.design.HeaderAction
import app.nougat.design.ListRow
import app.nougat.design.LocalAccent
import app.nougat.design.LocalColors
import app.nougat.design.LocalOverHeader
import app.nougat.design.Metrics
import app.nougat.design.Page
import app.nougat.design.RowAction
import app.nougat.design.Subheader
import app.nougat.design.Type
import app.nougat.library.FolderListing
import app.nougat.library.FolderSummary
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import app.nougat.library.LibrarySort
import app.nougat.library.Track
import app.nougat.library.count
import app.nougat.library.commonFolder
import app.nougat.library.listing
import app.nougat.library.playbackTime
import app.nougat.library.search
import app.nougat.library.under
import kotlinx.coroutines.launch

/** Hides a folder or song, with Undo in the snackbar. */
@Composable
private fun rememberHide(): (path: String) -> Unit {
    val library = LocalLibrary.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    return { path ->
        library.hide(path)
        scope.launch {
            val name = path.substringAfterLast('/')
            if (notices.showSnackbar("Hid “$name”", "Undo") == SnackbarResult.ActionPerformed) library.unhide(path)
        }
    }
}

/**
 * A song: thumbnail, title, artist, duration; the one playing in the accent colour. Songs Android
 * cannot play are dimmed. In the library lists a swipe offers "Add to playlist" (`swipeToAdd`).
 */
@Composable
fun TrackRow(
    track: Track,
    onTap: () -> Unit,
    actions: List<RowAction> = emptyList(),
    menuOnLongPress: Boolean = true,
    swipeToAdd: Boolean = false,
) {
    val playing = LocalPlayer.current.current?.path == track.path
    val row = @Composable {
        ListRow(
            track.title, subtitle = if (track.isPlayable) track.artist ?: "Unknown artist" else "Unsupported format",
            detail = if (track.isPlayable) playbackTime(track.durationMs) else null,
            highlighted = playing, enabled = track.isPlayable, spokenState = if (playing) "Playing" else null,
            actions = actions, onTap = onTap, leading = { TrackThumbnail(track) }, menuOnLongPress = menuOnLongPress,
        )
    }
    if (!swipeToAdd || !track.isPlayable) return row()
    val addTo = LocalAddToPlaylist.current
    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            addTo(listOf(track.path))
            swipe.reset()
        }
    }
    SwipeToDismissBox(swipe, backgroundContent = { SwipeBackground("Add to playlist") }, enableDismissFromStartToEnd = false) {
        Box(Modifier.background(LocalColors.current.paper)) { row() }
    }
}

/** The menu of a song in the library lists. */
@Composable
private fun songActions(track: Track, hide: (String) -> Unit): List<RowAction> {
    val addTo = LocalAddToPlaylist.current
    return listOfNotNull(
        if (track.isPlayable) RowAction("Add to playlist") { addTo(listOf(track.path)) } else null,
        RowAction("Hide") { hide(track.path) },
    )
}

@Composable
private fun FolderRow(folder: FolderSummary, push: (Screen) -> Unit, actions: List<RowAction> = emptyList()) {
    ListRow(
        folder.name, subtitle = count(folder.songCount, "song"), actions = actions,
        onTap = { push(Screen.Folder(folder.path)) }, leading = { FolderAvatar() },
        titleModifier = Modifier.sharedTitle("folder:${folder.path}"),
    )
}

/**
 * One folder of the library: its subfolders, then its songs. An empty `path` is the top of the
 * Folders tab, which shows the deepest folder holding every song (decision A17).
 */
@Composable
fun FolderScreen(path: String, push: (Screen) -> Unit, back: (() -> Unit)?) {
    val library = LocalLibrary.current
    val hide = rememberHide()
    val folder = remember(library.tracks, path) { if (path.isEmpty()) library.tracks.commonFolder() else path }
    val listing = remember(library.tracks, folder, library.folderSort) { library.tracks.listing(folder, library.folderSort) }
    val player = LocalPlayer.current
    // The play button takes everything in the folder, subfolders included.
    val everything = remember(library.tracks, folder) { library.tracks.under(folder).filter { it.isPlayable } }
    val addTo = LocalAddToPlaylist.current

    Page(
        title = if (path.isEmpty()) "Music" else path.substringAfterLast('/'),
        subtitle = when {
            !library.hasAccess -> null
            listing.songCount == 0 -> if (library.isReading) "Looking for music" else "No songs"
            listing.folders.isEmpty() -> count(listing.songCount, "song")
            else -> "${count(listing.folders.size, "folder")}, ${count(listing.songCount, "song")}"
        },
        action = if (everything.isNotEmpty()) HeaderAction("Play folder") { player.play(everything) } else null,
        firstSubheader = when {
            listing.folders.isNotEmpty() -> "Folders"
            listing.tracks.isNotEmpty() -> "Songs"
            else -> null
        },
        titleModifier = Modifier.sharedTitle("folder:$path"),
        navigationIcon = { back?.let { BarIcon(R.drawable.ic_arrow_back, "Back", it) } },
        barActions = {
            BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) }
            MoreMenu(library.folderSort, library::sortFolders) { close ->
                if (everything.isNotEmpty()) {
                    MenuItem("Shuffle") { close(); player.play(everything, shuffled = true) }
                    // Every song in this folder and its subfolders.
                    MenuItem("Add all to playlist") { close(); addTo(everything.map { it.path }) }
                }
                if (path.isEmpty()) MenuItem("Settings") { close(); push(Screen.Settings) }
                val debug = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                if (path.isEmpty() && debug) MenuItem("Design gallery") { close(); push(Screen.Gallery) }
            }
        },
        onRefresh = { library.refresh() },
    ) {
        if (!library.hasAccess) {
            item { AccessRequest() }
            return@Page
        }
        items(listing.folders, key = { "f:" + it.path }) { f ->
            FolderRow(f, push, listOf(
                RowAction("Play") { player.play(library.tracks.under(f.path)) },
                RowAction("Shuffle") { player.play(library.tracks.under(f.path), shuffled = true) },
                RowAction("Add to playlist") { addTo(library.tracks.under(f.path).filter { it.isPlayable }.map { it.path }) },
                RowAction("Hide") { hide(f.path) },
            ))
        }
        if (listing.folders.isNotEmpty() && listing.tracks.isNotEmpty()) item { Subheader("Songs") }
        items(listing.tracks, key = { "t:" + it.path }) { t ->
            TrackRow(t, { player.play(listing.tracks, t) }, songActions(t, hide), swipeToAdd = true)
        }
        if (listing.songCount == 0 && !library.isReading) item {
            EmptyState(
                "No music here yet", "Copy music into the Music folder over USB, then pull down to refresh.",
                "Refresh", { library.requestRefresh() }, Modifier.fillMaxWidth().padding(top = 48.dp),
            )
        }
    }
}

/** Every song in one list. */
@Composable
fun SongsScreen(push: (Screen) -> Unit) {
    val library = LocalLibrary.current
    val player = LocalPlayer.current
    val hide = rememberHide()
    val byName = library.songSort == LibrarySort.Name
    val songs = remember(library.tracksByTitle, library.songSort) {
        if (byName) library.tracksByTitle else library.tracks.sortedByDescending { it.modified }
    }
    Page(
        title = "Songs",
        subtitle = if (!library.hasAccess) null else if (songs.isEmpty()) "No songs" else count(songs.size, "song"),
        action = if (songs.any { it.isPlayable }) HeaderAction("Shuffle all", R.drawable.ic_shuffle) { player.play(songs, shuffled = true) } else null,
        firstSubheader = if (songs.isEmpty()) null else if (byName) "By name" else "Newest first",
        barActions = {
            BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) }
            MoreMenu(library.songSort, library::sortSongs)
        },
        onRefresh = { library.refresh() },
    ) {
        when {
            !library.hasAccess -> item { AccessRequest() }
            songs.isEmpty() && !library.isReading -> item {
                EmptyState("No music yet", "Copy music into the Music folder over USB, then pull down to refresh.", "Refresh", { library.requestRefresh() }, Modifier.fillMaxWidth().padding(top = 48.dp))
            }
            else -> items(songs, key = { it.path }) { t -> TrackRow(t, { player.play(songs, t) }, songActions(t, hide), swipeToAdd = true) }
        }
    }
}

/** Sort by name or date added, plus any extra entries. */
@Composable
private fun MoreMenu(sort: LibrarySort, onSort: (LibrarySort) -> Unit, extra: @Composable (close: () -> Unit) -> Unit = {}) {
    var open by remember { mutableStateOf(false) }
    Box {
        BarIcon(R.drawable.ic_more_vert, "More") { open = true }
        DropdownMenu(open, { open = false }, containerColor = LocalColors.current.surface) {
            for ((option, label) in listOf(LibrarySort.Name to "Sort by name", LibrarySort.DateAdded to "Sort by date added")) {
                MenuItem(label, checked = sort == option) { open = false; onSort(option) }
            }
            extra { open = false }
        }
    }
}

@Composable
private fun MenuItem(label: String, checked: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = Type.rowTitle, color = LocalColors.current.ink) },
        trailingIcon = if (checked) ({ Icon(painterResource(R.drawable.ic_check), null, tint = LocalAccent.current.text) }) else null,
        onClick = onClick,
    )
}

/** Search opens from the icon in any tab's bar, with the keyboard up; results update as you type. */
@Composable
fun SearchScreen(push: (Screen) -> Unit, back: () -> Unit) {
    val colors = LocalColors.current
    val library = LocalLibrary.current
    val player = LocalPlayer.current
    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = false }
    var query by rememberSaveable { mutableStateOf("") }
    // Searched off the main thread, once typing pauses, so thousands of songs do not slow the keyboard.
    val results by produceState(FolderListing(), library.tracks, query) {
        if (value.tracks.isNotEmpty() || value.folders.isNotEmpty()) delay(150)
        value = withContext(Dispatchers.Default) { library.tracks.search(query) }
    }
    val playlists = LocalPlaylists.current
    val lists = remember(playlists.all, query) {
        val term = app.nougat.library.folded(query.trim())
        if (term.isEmpty()) emptyList() else playlists.sorted.filter { term in app.nougat.library.folded(it.name) }
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(list.isScrollInProgress) { if (list.isScrollInProgress) keyboard?.hide() }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarIcon(R.drawable.ic_arrow_back, "Back", back)
                Box(Modifier.weight(1f).padding(horizontal = Metrics.grid)) {
                    if (query.isEmpty()) Text("Songs, artists, albums, folders, playlists", style = Type.rowTitle, color = colors.ink3)
                    BasicTextField(
                        query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = Type.rowTitle.copy(color = colors.ink), singleLine = true, cursorBrush = SolidColor(colors.ink),
                    )
                }
                if (query.isNotEmpty()) BarIcon(R.drawable.ic_close, "Clear") { query = "" }
            }
        }
        if (results.folders.isEmpty() && results.tracks.isEmpty() && lists.isEmpty()) {
            Text(
                if (query.isBlank()) "Search songs, artists, albums, folders and playlists." else "Nothing matches “${query.trim()}”.",
                style = Type.body, color = colors.ink2, modifier = Modifier.padding(Metrics.margin),
            )
        }
        LazyColumn(Modifier.fillMaxSize(), state = list) {
            if (lists.isNotEmpty()) item { Subheader("Playlists") }
            items(lists, key = { "p:" + it.id }) { p ->
                ListRow(p.name, subtitle = count(p.songs.mapNotNull(library::track).size, "song"), onTap = { push(Screen.Playlist(p.id)) }, leading = { PlaylistAvatar() })
            }
            if (results.folders.isNotEmpty()) item { Subheader("Folders") }
            items(results.folders, key = { "f:" + it.path }) { FolderRow(it, push) }
            if (results.tracks.isNotEmpty()) item { Subheader("Songs") }
            items(results.tracks, key = { "t:" + it.path }) { t -> TrackRow(t, { player.play(results.tracks, t) }) }
        }
    }
}

/** Folders and songs the user hid, each shown again with a tap (decision A10). Settings will link here (P7.1). */
@Composable
fun HiddenScreen(back: () -> Unit) {
    val library = LocalLibrary.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    val hidden = library.hidden.sortedWith(compareBy(app.nougat.library.NameOrder) { it })
    Page(
        title = "Hidden",
        subtitle = if (hidden.isEmpty()) "Nothing hidden" else "Tap one to show it again",
        navigationIcon = { BarIcon(R.drawable.ic_arrow_back, "Back", back) },
    ) {
        items(hidden, key = { it }) { path ->
            val show = {
                library.unhide(path)
                scope.launch {
                    if (notices.showSnackbar("Showing “${path.substringAfterLast('/')}” again", "Undo") == SnackbarResult.ActionPerformed) library.hide(path)
                }
                Unit
            }
            ListRow(
                path.substringAfterLast('/'), subtitle = path.substringBeforeLast('/', ""),
                actions = listOf(RowAction("Show again", show)), onTap = show,
                leading = { if (library.isSong(path)) app.nougat.design.LetterTile(path.substringAfterLast('/')) else FolderAvatar() },
            )
        }
    }
}
