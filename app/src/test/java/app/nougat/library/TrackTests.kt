package app.nougat.library

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackTests {
    @Test
    fun pathsAreRelativeToTheStorageRoot() {
        assertEquals("Music/Road Trip/Kite Season.mp3", libraryPath("external_primary", "Music/Road Trip/", "Kite Season.mp3"))
        assertEquals("song.mp3", libraryPath("external_primary", "", "song.mp3"))
        assertEquals("song.mp3", libraryPath(null, null, "song.mp3"))
        // An SD card keeps its volume name, so the same relative path on two volumes stays two songs.
        assertEquals("1a2b-3c4d/Music/song.mp3", libraryPath("1a2b-3c4d", "Music/", "song.mp3"))
    }

    @Test
    fun oldAndroidPathsComeFromTheFilePath() {
        val root = "/storage/emulated/0"
        assertEquals("Music/Road Trip/Kite Season.mp3", libraryPath("/storage/emulated/0/Music/Road Trip/Kite Season.mp3", root))
        assertEquals("1A2B-3C4D/Music/song.mp3", libraryPath("/storage/1A2B-3C4D/Music/song.mp3", root))
    }

    @Test
    fun playbackTimeReadsLikeAClock() {
        assertEquals("0:00", playbackTime(0))
        assertEquals("3:42", playbackTime(222_000))
        assertEquals("0:12", playbackTime(12_042))
        assertEquals("1:02:03", playbackTime(3_723_000))
    }
}
