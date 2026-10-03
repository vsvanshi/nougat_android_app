package app.nougat.screens

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.EmptyState
import app.nougat.design.FolderAvatar
import app.nougat.design.HeaderAction
import app.nougat.design.LetterTile
import app.nougat.design.ListRow
import app.nougat.design.LocalColors
import app.nougat.design.LocalOverHeader
import app.nougat.design.Metrics
import app.nougat.design.Page
import app.nougat.design.RowAction
import app.nougat.design.Type
import app.nougat.library.playbackTime
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch

// Stand-ins so the shell can be tried before the library (phase 2) and playlists (phase 4) exist.

@Composable
fun FolderScreen(path: String, push: (Screen) -> Unit, back: (() -> Unit)?) {
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    val name = path.substringAfterLast('/').ifEmpty { "Music" }
    val hasAccess = LocalLibrary.current.hasAccess
    Page(
        title = name,
        subtitle = "Sample folders until the library arrives",
        action = HeaderAction("Play folder") { scope.launch { notices.showSnackbar("Play $name") } },
        firstSubheader = "Folders",
        titleModifier = Modifier.sharedTitle("folder:$path"),
        navigationIcon = { back?.let { BarIcon(R.drawable.ic_arrow_back, "Back", it) } },
        barActions = {
            BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) }
            if (back == null) MoreMenu(push)
        },
    ) {
        if (path.isEmpty() && !hasAccess) {
            item { AccessRequest() }
            return@Page
        }
        for (child in listOf("Road Trip 2016", "Rainy Days", "Gym")) {
            item(key = child) {
                ListRow(
                    child, subtitle = "Sample folder",
                    actions = listOf(RowAction("Play") { scope.launch { notices.showSnackbar("Play $child") } }),
                    onTap = { push(Screen.Folder("$path/$child")) },
                    leading = { FolderAvatar() },
                    titleModifier = Modifier.sharedTitle("folder:$path/$child"),
                )
            }
        }
    }
}

/** The root Folders menu. Settings come in P7.1; the design gallery is in debug builds only. */
@Composable
private fun MoreMenu(push: (Screen) -> Unit) {
    val debug = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!debug) return
    var open by remember { mutableStateOf(false) }
    Box {
        BarIcon(R.drawable.ic_more_vert, "More") { open = true }
        DropdownMenu(open, { open = false }, containerColor = LocalColors.current.surface) {
            DropdownMenuItem(
                text = { Text("Design gallery", style = Type.rowTitle, color = LocalColors.current.ink) },
                onClick = { open = false; push(Screen.Gallery) },
            )
        }
    }
}

/** Every song, sorted by title. Pull to refresh, the sort menu and playing come in P2.5 and phase 3. */
@Composable
fun SongsScreen(push: (Screen) -> Unit) {
    val library = LocalLibrary.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    val songs = library.tracksByTitle
    Page(
        title = "Songs",
        subtitle = if (library.hasAccess) "${songs.size} songs" else null,
        barActions = { BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) } },
    ) {
        when {
            !library.hasAccess -> item { AccessRequest() }
            songs.isEmpty() -> item {
                EmptyState("No music yet", "Copy music into the Music folder over USB.", "Refresh", { scope.launch { library.refresh() } }, Modifier.fillMaxWidth())
            }
            else -> items(songs, key = { it.path }) { track ->
                ListRow(
                    track.title, subtitle = track.artist ?: "Unknown artist", detail = playbackTime(track.durationMs),
                    enabled = track.isPlayable, spokenState = if (track.isPlayable) null else "Cannot be played",
                    onTap = { scope.launch { notices.showSnackbar("Playback arrives in phase 3") } },
                    leading = { LetterTile(track.title) },
                )
            }
        }
    }
}

@Composable
fun PlaylistsScreen(push: (Screen) -> Unit) {
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    Page(
        title = "Playlists",
        barActions = { BarIcon(R.drawable.ic_search, "Search") { push(Screen.Search) } },
    ) {
        item {
            EmptyState(
                "No playlists yet", "Playlists arrive in phase 4.", "New playlist",
                { scope.launch { notices.showSnackbar("Playlists arrive in phase 4") } }, Modifier.fillMaxWidth(),
                icon = R.drawable.ic_queue_music,
            )
        }
    }
}

/** Search opens from the icon in any tab's bar, with the keyboard up. */
@Composable
fun SearchScreen(back: () -> Unit) {
    val colors = LocalColors.current
    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = false }
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarIcon(R.drawable.ic_arrow_back, "Back", back)
                Box(Modifier.weight(1f).padding(horizontal = Metrics.grid)) {
                    if (query.isEmpty()) Text("Search songs and folders", style = Type.rowTitle, color = colors.ink3)
                    BasicTextField(
                        query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = Type.rowTitle.copy(color = colors.ink), singleLine = true,
                        cursorBrush = SolidColor(colors.ink),
                    )
                }
                if (query.isNotEmpty()) BarIcon(R.drawable.ic_close, "Clear") { query = "" }
            }
        }
        Text(
            "Results arrive with the library in phase 2.", style = Type.body, color = colors.ink2,
            modifier = Modifier.padding(Metrics.margin),
        )
    }
}
