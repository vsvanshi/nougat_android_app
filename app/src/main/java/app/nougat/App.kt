package app.nougat

import android.app.Application
import app.nougat.library.ArtworkStore
import app.nougat.library.MediaLibrary
import app.nougat.equalizer.EqualizerSettings
import app.nougat.playback.Player
import app.nougat.playlists.PlaylistFile
import app.nougat.playlists.Playlists
import java.io.File

/** One library, artwork store, playlists, equalizer and player for the whole process, shared by the screens and the media service. */
class App : Application() {
    val library by lazy { MediaLibrary(this) }
    val artwork by lazy { ArtworkStore(this) }
    val equalizer by lazy { EqualizerSettings(File(filesDir, "equalizer.json")) }
    val playlists by lazy { PlaylistFile(File(filesDir, "playlists.json")).let { file -> Playlists(file.read(), file::write) } }
    val player by lazy { Player(this, library, artwork, equalizer) }
}
