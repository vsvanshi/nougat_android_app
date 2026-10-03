package app.nougat.library

import org.junit.Assert.assertEquals
import org.junit.Test

// Ported from the iPhone LibraryTests.swift (listing) and PlaybackTests.swift (search).

private fun track(path: String, title: String, daysAgo: Int = 0, artist: String? = null, album: String? = null) =
    Track(path, title, artist, album, 60_000, true, 1, 1_000_000L - daysAgo * 86_400L, "content://$path")

class ListingTests {
    private val tracks = listOf(
        track("Gym/Push.mp3", "Push", daysAgo = 9),
        track("Road Trip/Kite.mp3", "Kite", daysAgo = 5),
        track("Road Trip/Live/Encore.mp3", "Encore", daysAgo = 1),
        track("Zebra.mp3", "Zebra", daysAgo = 3),
        track("apple.mp3", "apple", daysAgo = 7),
    )

    @Test
    fun listingSplitsFoldersFromSongs() {
        val top = tracks.listing("")
        assertEquals(listOf("Gym", "Road Trip"), top.folders.map { it.name })
        assertEquals(listOf(1, 2), top.folders.map { it.songCount })       // counts include subfolders
        assertEquals(listOf("apple", "Zebra"), top.tracks.map { it.title })  // case-insensitive name order
        assertEquals(5, top.songCount)

        val trip = tracks.listing("Road Trip")
        assertEquals(listOf("Road Trip/Live"), trip.folders.map { it.path })
        assertEquals(listOf("Kite"), trip.tracks.map { it.title })

        // A folder whose name merely starts the same is a different folder.
        assertEquals(0, tracks.listing("Road").songCount)

        val byDate = tracks.listing("", LibrarySort.DateAdded)
        assertEquals(listOf("Road Trip", "Gym"), byDate.folders.map { it.name })  // Encore is the newest song anywhere
        assertEquals(listOf("Zebra", "apple"), byDate.tracks.map { it.title })
    }

    @Test
    fun namesSortWithNumbersInOrder() {
        val names = listOf("Track 10", "track 2", "Track 1", "Álbum", "album b")
        assertEquals(listOf("Álbum", "album b", "Track 1", "track 2", "Track 10"), names.sortedWith(NameOrder))
    }

    @Test
    fun searchMatchesSongsArtistsAlbumsAndFolders() {
        val tracks = listOf(
            track("Hindi/Arijit/Tum Hi Ho.mp3", "Tum Hi Ho", artist = "Arijit Singh", album = "Aashiqui 2"),
            track("Hindi/Saaiyaan.mp3", "Saaiyaan", artist = "Rahat Fateh Ali Khan"),
            track("Gym/Push.mp3", "Push", artist = "Café Tacvba"),
        )
        val arijit = tracks.search("arijit")
        assertEquals(listOf("Tum Hi Ho"), arijit.tracks.map { it.title })      // by artist
        assertEquals(listOf("Hindi/Arijit"), arijit.folders.map { it.path })   // and the folder of that name

        assertEquals(listOf("Tum Hi Ho"), tracks.search("aashiqui").tracks.map { it.title })  // by album
        assertEquals(listOf("Push"), tracks.search("cafe").tracks.map { it.title })           // accents ignored
        assertEquals(2, tracks.search("hindi").folders.first().songCount)                     // includes subfolders
        assertEquals(FolderListing(), tracks.search("   "))
        assertEquals(FolderListing(), tracks.search("zzz"))
        assertEquals(2, tracks.under("Hindi").size)
        assertEquals(3, tracks.under("").size)
    }

    @Test
    fun theTopIsTheDeepestFolderHoldingEverySong() {
        assertEquals("Music", listOf(track("Music/A/1.mp3", "1"), track("Music/B/2.mp3", "2")).commonFolder())
        assertEquals("Music/A", listOf(track("Music/A/1.mp3", "1"), track("Music/A/2.mp3", "2")).commonFolder())
        assertEquals("", listOf(track("Music/1.mp3", "1"), track("Download/2.mp3", "2")).commonFolder())
        // "Music" and "Musicals" share letters, not a folder.
        assertEquals("", listOf(track("Music/1.mp3", "1"), track("Musicals/2.mp3", "2")).commonFolder())
        assertEquals("", emptyList<Track>().commonFolder())
    }
}
