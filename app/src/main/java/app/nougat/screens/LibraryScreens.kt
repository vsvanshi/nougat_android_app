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
import app.nougat.library.FolderSummary
import app.nougat.library.LibrarySort
import app.nougat.library.Track
import app.nougat.library.count
import app.nougat.library.commonFolder
import app.nougat.library.listing
import app.nougat.library.playbackTime
import app.nougat.library.search
import app.nougat.library.under
import kotlinx.coroutines.launch

private const val NOT_YET = "Playback arrives in phase 3"

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

@Composable
private fun rememberNotify(): (String) -> Unit {
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    return { message -> scope.launch { notices.showSnackbar(message) } }
}

/** A song: thumbnail, title, artist, duration. Songs Android cannot play are greyed out. */
@Composable
fun TrackRow(track: Track, onTap: () -> Unit, actions: List<RowAction> = emptyList()) {
    ListRow(
        track.title, subtitle = track.artist ?: "Unknown artist", detail = playbackTime(track.durationMs),
        enabled = track.isPlayable, spokenState = if (track.isPlayable) null else "Cannot be played",
        actions = actions, onTap = onTap, leading = { TrackThumbnail(track) },
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
    val notify = rememberNotify()
    val hide = rememberHide()
    val folder = if (path.isEmpty()) library.tracks.commonFolder() else path
    val listing = remember(library.tracks, folder, library.folderSort) { library.tracks.listing(folder, library.folderSort) }
    val playable = remember(library.tracks, folder) { library.tracks.under(folder).any { it.isPlayable } }

    Page(
        title = if (path.isEmpty()) "Music" else path.substringAfterLast('/'),
        subtitle = when {
            !library.hasAccess -> null
            listing.songCount == 0 -> if (library.isReading) "Looking for music" else "No songs"
            listing.folders.isEmpty() -> count(listing.songCount, "song")
            else -> "${count(listing.folders.size, "folder")}, ${count(listing.songCount, "song")}"
        },
        action = if (playable) HeaderAction("Play folder") { notify(NOT_YET) } else null,
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
                if (path.isEmpty() && library.hidden.isNotEmpty()) MenuItem("Hidden folders and songs") { close(); push(Screen.Hidden) }
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
            FolderRow(f, push, listOf(RowAction("Play") { notify(NOT_YET) }, RowAction("Hide") { hide(f.path) }))
        }
        if (listing.folders.isNotEmpty() && listing.tracks.isNotEmpty()) item { Subheader("Songs") }
        items(listing.tracks, key = { "t:" + it.path }) { t ->
            TrackRow(t, { notify(NOT_YET) }, listOf(RowAction("Hide") { hide(t.path) }))
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
    val notify = rememberNotify()
    val hide = rememberHide()
    val byName = library.songSort == LibrarySort.Name
    val songs = remember(library.tracksByTitle, library.songSort) {
        if (byName) library.tracksByTitle else library.tracks.sortedByDescending { it.modified }
    }
    Page(
        title = "Songs",
        subtitle = if (!library.hasAccess) null else if (songs.isEmpty()) "No songs" else count(songs.size, "song"),
        action = if (songs.any { it.isPlayable }) HeaderAction("Shuffle all", R.drawable.ic_shuffle) { notify(NOT_YET) } else null,
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
            else -> items(songs, key = { it.path }) { t -> TrackRow(t, { notify(NOT_YET) }, listOf(RowAction("Hide") { hide(t.path) })) }
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
    val notify = rememberNotify()
    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = false }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(library.tracks, query) { library.tracks.search(query) }
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
                    if (query.isEmpty()) Text("Songs, artists, albums, folders", style = Type.rowTitle, color = colors.ink3)
                    BasicTextField(
                        query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = Type.rowTitle.copy(color = colors.ink), singleLine = true, cursorBrush = SolidColor(colors.ink),
                    )
                }
                if (query.isNotEmpty()) BarIcon(R.drawable.ic_close, "Clear") { query = "" }
            }
        }
        if (results.folders.isEmpty() && results.tracks.isEmpty()) {
            Text(
                if (query.isBlank()) "Search songs, artists, albums and folders." else "Nothing matches “${query.trim()}”.",
                style = Type.body, color = colors.ink2, modifier = Modifier.padding(Metrics.margin),
            )
        }
        LazyColumn(Modifier.fillMaxSize(), state = list) {
            if (results.folders.isNotEmpty()) item { Subheader("Folders") }
            items(results.folders, key = { "f:" + it.path }) { FolderRow(it, push) }
            if (results.tracks.isNotEmpty()) item { Subheader("Songs") }
            items(results.tracks, key = { "t:" + it.path }) { TrackRow(it, { notify(NOT_YET) }) }
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
