package app.nougat.playlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the iPhone PlaylistTests.swift.

class PlaylistTests {
    @Test
    fun playlistNeverHoldsASongTwice() {
        val store = Playlists()
        val p = store.create("Sunday Slow")!!
        store.add(listOf("a", "b"), p.id)
        assertEquals(listOf("c", "d"), store.add(listOf("b", "c", "c", "d"), p.id))   // only the new ones, each once
        assertEquals(listOf("a", "b", "c", "d"), store[p.id]!!.songs)
        assertEquals(emptyList<String>(), store.add(listOf("a"), p.id))

        store.remove(listOf("b", "zzz"), p.id)
        assertEquals(listOf("a", "c", "d"), store[p.id]!!.songs)
    }

    @Test
    fun reorderingKeepsSongsWhoseFilesAreMissing() {
        // "gone" is in the playlist but its file was deleted, so it is not on screen.
        val playlist = Playlist("1", "Mix", listOf("a", "gone", "b", "c"))
        assertEquals(listOf("c", "a", "b", "gone"), playlist.moving(listOf("a", "b", "c"), 2, 0).songs)   // drag "c" to the top
    }

    @Test
    fun playlistsAreSavedAndNamesStayUnique() {
        var file = emptyList<Playlist>()
        val store = Playlists { file = it }

        assertNull(store.create("   "))                                   // blank names are refused
        val slow = store.create(" Sunday Slow ")!!
        assertEquals("Sunday Slow", slow.name)
        assertEquals(slow.id, store.create("sunday slow")?.id)              // same name: the same playlist, not a second
        assertEquals(slow.id, store.create("Sunday Slöw")?.id)              // accents ignored too
        val gym = store.create("Gym")!!
        assertEquals(2, store.all.size)
        assertEquals(listOf("Gym", "Sunday Slow"), store.sorted.map { it.name })

        assertEquals(listOf("x.mp3", "y.mp3"), store.add(listOf("x.mp3", "y.mp3"), slow.id))
        assertEquals(emptyList<String>(), store.add(listOf("y.mp3"), slow.id))
        assertEquals(1, store.additions)                                     // the success haptic: only for a real add
        assertFalse(store.rename(gym.id, "Sunday Slow"))                     // taken
        assertTrue(store.rename(gym.id, "Workout"))
        assertTrue(store.rename(slow.id, "sunday slow"))                     // changing only the case of its own name is fine

        // A fresh store starts from what was saved.
        val reloaded = Playlists(file) { file = it }
        assertEquals(store.all, reloaded.all)
        assertEquals(listOf("x.mp3", "y.mp3"), reloaded[slow.id]!!.songs)

        reloaded.delete(gym.id)
        assertEquals(listOf("sunday slow"), Playlists(file).all.map { it.name })
    }
}
