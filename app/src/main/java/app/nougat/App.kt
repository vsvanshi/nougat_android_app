package app.nougat

import android.app.Application
import app.nougat.library.ArtworkStore
import app.nougat.library.MediaLibrary
import app.nougat.playback.Player

/** One library, artwork store and player for the whole process, shared by the screens and the media service. */
class App : Application() {
    val library by lazy { MediaLibrary(this) }
    val artwork by lazy { ArtworkStore(this) }
    val player by lazy { Player(this, library, artwork) }
}
