package app.nougat.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.nougat.R
import app.nougat.design.BarIcon
import app.nougat.design.EmptyState
import app.nougat.design.Page
import kotlinx.coroutines.launch

// Stand-in until playlists arrive in phase 4.

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
