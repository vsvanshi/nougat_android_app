package app.nougat.playlists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.nougat.library.NameOrder
import app.nougat.library.folded
import java.util.UUID

data class Playlist(
    val id: String,
    val name: String,
    /** Library paths in play order. A path whose file has gone is kept, so the song returns if the file does (D41). */
    val songs: List<String> = emptyList(),
) {
    /** The songs that are not already here, each once; a playlist never holds a song twice. */
    fun newSongs(paths: List<String>): List<String> {
        val present = songs.toMutableSet()
        return paths.filter { present.add(it) }
    }

    fun removing(paths: List<String>) = copy(songs = songs - paths.toSet())

    /**
     * Moves a song among those on screen (`visible`: the ones whose files exist). Songs not on screen
     * keep their order, after the visible ones.
     */
    fun moving(visible: List<String>, from: Int, to: Int): Playlist {
        val shown = visible.toMutableList()
        if (from !in shown.indices) return this
        shown.add(to.coerceIn(0, shown.size - 1), shown.removeAt(from))
        val onScreen = shown.toSet()
        return copy(songs = shown + songs.filterNot { it in onScreen })
    }
}

/**
 * The user's playlists. Names are unique ignoring case and accents. `save` is called after every
 * change; the app writes `playlists.json` with it (see `PlaylistFile`).
 */
class Playlists(initial: List<Playlist> = emptyList(), private val save: (List<Playlist>) -> Unit = {}) {
    var all by mutableStateOf(initial)
        private set

    /**
     * Goes up whenever songs are really added to a playlist; the app shell plays the success haptic on
     * it (D57), because the sheet that adds songs closes at that very moment.
     */
    var additions by mutableIntStateOf(0)
        private set

    /** In name order, for lists. */
    val sorted get() = all.sortedWith(compareBy(NameOrder) { it.name })

    operator fun get(id: String) = all.firstOrNull { it.id == id }

    /** Makes a playlist. A name already in use returns that playlist instead of a second one; a blank name returns null. */
    fun create(name: String): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        named(trimmed)?.let { return it }
        val playlist = Playlist(UUID.randomUUID().toString(), trimmed)
        update(all + playlist)
        return playlist
    }

    /** Returns false, and changes nothing, if the name is blank or belongs to another playlist. */
    fun rename(id: String, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || named(trimmed).let { it != null && it.id != id }) return false
        change(id) { it.copy(name = trimmed) }
        return true
    }

    fun delete(id: String) = update(all.filterNot { it.id == id })

    /** Adds the songs not already in the playlist and returns those. */
    fun add(paths: List<String>, id: String): List<String> {
        val added = this[id]?.newSongs(paths).orEmpty()
        if (added.isEmpty()) return added
        change(id) { it.copy(songs = it.songs + added) }
        additions++
        return added
    }

    fun remove(paths: List<String>, id: String) = change(id) { it.removing(paths) }

    fun move(id: String, visible: List<String>, from: Int, to: Int) = change(id) { it.moving(visible, from, to) }

    private fun named(name: String) = all.firstOrNull { folded(it.name) == folded(name) }

    private fun change(id: String, edit: (Playlist) -> Playlist) = update(all.map { if (it.id == id) edit(it) else it })

    private fun update(playlists: List<Playlist>) {
        all = playlists
        save(playlists)
    }
}
