package app.nougat.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import app.nougat.library.ArtworkStore
import app.nougat.library.MediaLibrary
import app.nougat.library.Track
import app.nougat.playback.PlayQueue.Step
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.media3.common.Player as MediaPlayer

/**
 * What is playing and what plays next. Nougat's `PlayQueue` decides the order (shuffle included),
 * as on iPhone; ExoPlayer is given one song at a time. Media3 brings the notification, lock screen,
 * headset and car buttons (through `sessionPlayer`), audio focus and pausing when headphones go.
 * Screens read the Compose state below; everything runs on the main thread.
 */
class Player(context: Context, private val library: MediaLibrary, private val artwork: ArtworkStore) {
    private val scope = MainScope()
    private val stateFile = File(context.filesDir, "player.json")

    val exo: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    private var queue = PlayQueue()
    /** A position restored from the last run, applied when the song is first opened. */
    private var pendingSeek: Long? = null

    var current by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    /** The queue as the screens see it. */
    var items by mutableStateOf<List<String>>(emptyList())
        private set
    var index by mutableStateOf(0)
        private set
    var isShuffled by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(RepeatMode.Off)
        private set

    /** Not observable: read it on a timer while it needs to tick. */
    val positionMs get() = pendingSeek ?: exo.currentPosition
    val durationMs get() = current?.durationMs ?: 0L

    /**
     * The player the media session (notification, lock screen, buttons) talks to. Next and previous
     * go to Nougat's queue, so they are always offered, as is play after a restart.
     */
    val sessionPlayer: MediaPlayer = object : ForwardingPlayer(exo) {
        private val extra = listOf(
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )
        override fun getAvailableCommands() = super.getAvailableCommands().buildUpon().addAll(*extra.toIntArray()).build()
        override fun isCommandAvailable(command: Int) = command in extra || super.isCommandAvailable(command)
        override fun seekToNext() = next()
        override fun seekToNextMediaItem() = next()
        override fun seekToPrevious() = previous()
        override fun seekToPreviousMediaItem() = previous()
        override fun play() = resume()
    }

    init {
        exo.addListener(object : MediaPlayer.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady
                if (!playWhenReady) save()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == MediaPlayer.STATE_ENDED) apply(queue.next(auto = true))
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w("Nougat", "Cannot play ${current?.path}", error)
                apply(queue.next())
            }
        })
        restore()
        scope.launch {
            // The library knows the songs; follow it to show the remembered song and to drop deleted ones (D40).
            if (!library.hasRead) library.refresh()
            snapshotFlow { library.readCount }.collect { libraryChanged() }
        }
    }

    // Starting playback

    /** Plays `list`, beginning with `start` (or the first song). Songs Android cannot play are left out (D32). */
    fun play(list: List<Track>, start: Track? = null, shuffled: Boolean = false) {
        val playable = list.filter { it.isPlayable }
        if (playable.isEmpty()) return
        val keepShuffle = queue.isShuffled || shuffled
        var at = start?.let { s -> playable.indexOfFirst { it.path == s.path } }?.takeIf { it >= 0 } ?: 0
        if (shuffled && start == null) at = playable.indices.random()
        queue = PlayQueue(playable.map { it.path }, at).apply { repeatMode = queue.repeatMode; setShuffled(keepShuffle) }
        apply(queue.current?.let { Step.Play(it) } ?: Step.Stop)
    }

    fun toggle() = if (exo.playWhenReady) pause() else resume()

    fun resume() {
        val track = current ?: return
        if (exo.mediaItemCount == 0) {
            // First play after a restart: open the remembered song at the remembered position.
            val at = pendingSeek
            load(track)
            at?.let { exo.seekTo(it) }
        }
        pendingSeek = null
        if (exo.playbackState == MediaPlayer.STATE_ENDED) exo.seekTo(0)
        exo.play()
    }

    fun pause() = exo.pause()

    fun next() = apply(queue.next())

    fun previous() = apply(queue.previous(positionMs))

    fun seek(ms: Long) {
        if (exo.mediaItemCount > 0) exo.seekTo(ms) else pendingSeek = ms
    }

    // Changing the queue

    fun toggleShuffle() {
        queue.setShuffled(!queue.isShuffled)
        publish()
        save()
    }

    fun cycleRepeat() {
        queue.repeatMode = RepeatMode.entries[(queue.repeatMode.ordinal + 1) % RepeatMode.entries.size]
        publish()
        save()
    }

    fun jump(position: Int) = apply(queue.jump(position))

    fun remove(position: Int) {
        queue.remove(position)?.let { apply(it, autoplay = exo.playWhenReady) }
        publish()
        save()
    }

    fun move(from: Int, to: Int) {
        queue.move(from, to)
        publish()
        save()
    }

    /** Songs deleted from the phone leave the queue (D40). Hidden ones stay: hiding only stops Nougat listing them. */
    private fun libraryChanged() {
        if (!library.hasRead) return
        if (current == null) current = queue.current?.let(library::track)
        val step = queue.removeAll { library.track(it) == null } ?: return run { publish() }
        when (step) {
            Step.Stop -> {
                exo.stop()
                exo.clearMediaItems()
                current = null
                pendingSeek = null
            }
            else -> apply(step, autoplay = exo.playWhenReady)
        }
        publish()
        save()
    }

    // Private

    private fun apply(step: Step, autoplay: Boolean = true) {
        when (step) {
            is Step.Play -> {
                // A file can vanish. Skip forward, but never loop for ever.
                var path: String? = step.path
                repeat(maxOf(1, queue.items.size)) {
                    val track = path?.let(library::track)
                    if (track != null && track.isPlayable) {
                        load(track)
                        if (autoplay) exo.play() else exo.pause()
                        publish()
                        save()
                        return
                    }
                    path = (queue.next() as? Step.Play)?.path
                }
                stop()
            }
            Step.Restart -> {
                seek(0)
                if (autoplay) resume()
            }
            Step.Stop -> stop()
        }
    }

    /** End of the queue: stay on the last song, paused at its start. */
    private fun stop() {
        exo.pause()
        if (exo.mediaItemCount > 0) exo.seekTo(0)
        publish()
        save()
    }

    private fun load(track: Track) {
        current = track
        pendingSeek = null
        exo.setMediaItem(mediaItem(track, null))
        exo.prepare()
        // The lock screen and notification get the same art as Now playing, embedded or beside the file.
        scope.launch {
            val image = artwork.fullImage(track) ?: return@launch
            if (current?.path == track.path && exo.mediaItemCount > 0) exo.replaceMediaItem(0, mediaItem(track, image))
        }
    }

    private fun mediaItem(track: Track, art: Bitmap?) = MediaItem.Builder()
        .setMediaId(track.path)
        .setUri(Uri.parse(track.uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .apply {
                    if (art != null) {
                        val bytes = ByteArrayOutputStream().also { art.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
                        setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                }
                .build(),
        )
        .build()

    private fun publish() {
        items = queue.items
        index = queue.index
        isShuffled = queue.isShuffled
        repeatMode = queue.repeatMode
        if (queue.current == null) current = null
    }

    // Remembering between runs: the queue and the position, saved on every song change, on pause and
    // when the app goes to the background (D35).

    fun save() {
        val s = queue.save()
        val json = JSONObject()
            .put("items", JSONArray(s.items)).put("index", s.index).put("shuffled", s.shuffled)
            .put("repeat", s.repeat.name).put("unshuffled", JSONArray(s.unshuffled)).put("elapsed", positionMs)
        runCatching { stateFile.writeText(json.toString()) }
    }

    private fun restore() {
        val json = runCatching { JSONObject(stateFile.readText()) }.getOrNull() ?: return
        fun list(key: String) = json.getJSONArray(key).let { a -> List(a.length()) { a.getString(it) } }
        queue = PlayQueue.restore(
            PlayQueue.Saved(
                list("items"), json.getInt("index"), json.getBoolean("shuffled"),
                RepeatMode.valueOf(json.getString("repeat")), list("unshuffled"),
            ),
        )
        pendingSeek = json.getLong("elapsed")
        publish()
    }
}
