package app.nougat.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The permission that lets Nougat see audio files: READ_MEDIA_AUDIO from Android 13, storage before. */
val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

/**
 * The music library: every song in the system's media library (decision A9), read with MediaStore.
 * Nothing is copied. Compose screens read `tracks` and `hasAccess` and recompose when they change.
 * Hidden folders and songs are left out of `tracks`.
 */
class MediaLibrary(private val context: Context) {
    private val prefs = context.getSharedPreferences("nougat", Context.MODE_PRIVATE)

    /** Every song MediaStore has, hidden ones included, sorted by path, and the same by title. */
    private var all by mutableStateOf<List<Track>>(emptyList())
    private var allByTitle = emptyList<Track>()

    /** Folders and songs the user hid (decision A10). Nothing is deleted; Settings can show them again. */
    var hidden by mutableStateOf(prefs.getStringSet(HIDDEN, emptySet())!!.toSet())
        private set

    /** The songs to show, sorted by path. */
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set

    /** The same songs sorted by title, made once per change (iPhone D60). */
    var tracksByTitle by mutableStateOf<List<Track>>(emptyList())
        private set

    var hasAccess by mutableStateOf(checkAccess())
        private set

    var isReading by mutableStateOf(false)
        private set

    /** True once the first read has finished, so an empty list means "no songs", not "not read yet". */
    var hasRead = false
        private set

    /** Goes up after every read that changed something; the player follows it. */
    var readCount by mutableStateOf(0)
        private set

    private var byPath = emptyMap<String, Track>()

    /** A song on the phone by path, hidden or not; null once its file has gone. */
    fun track(path: String) = byPath[path]

    /** Sort order of folder screens and of Songs, remembered. */
    var folderSort by mutableStateOf(sortPref(FOLDER_SORT))
        private set
    var songSort by mutableStateOf(sortPref(SONG_SORT))
        private set

    fun sortFolders(sort: LibrarySort) { folderSort = sort; prefs.edit().putString(FOLDER_SORT, sort.name).apply() }
    fun sortSongs(sort: LibrarySort) { songSort = sort; prefs.edit().putString(SONG_SORT, sort.name).apply() }

    private fun sortPref(key: String) = LibrarySort.entries.firstOrNull { it.name == prefs.getString(key, null) } ?: LibrarySort.Name

    /** Whether a hidden path is a song (rather than a folder), for the Hidden screen. */
    fun isSong(path: String) = all.any { it.path == path }

    private fun checkAccess() = context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED

    private val requests = Channel<Unit>(Channel.CONFLATED)

    /**
     * Keeps the library in step with MediaStore while `scope` lives: reads now, and again whenever
     * MediaStore reports a change (songs copied over USB, deleted by another app) or `requestRefresh`
     * is called. A request that arrives during a read gets one more read afterwards, never a lost one.
     */
    fun follow(scope: CoroutineScope) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { requestRefresh() }
        }
        context.contentResolver.registerContentObserver(collection, true, observer)
        scope.launch {
            try {
                requestRefresh()
                for (request in requests) {
                    delay(300) // MediaStore sends a burst of changes for one copy
                    refresh()
                }
            } finally {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    fun requestRefresh() { requests.trySend(Unit) }

    private val reading = Mutex()

    /**
     * Re-checks the permission and, with it, reads the media library again. A call that arrives while
     * another read is running waits for that one instead of reading twice; changes MediaStore reports
     * meanwhile still get their own read through `follow`.
     */
    suspend fun refresh() {
        if (!reading.tryLock()) {
            reading.withLock {}
            return
        }
        try { readNow() } finally { reading.unlock() }
    }

    private suspend fun readNow() {
        hasAccess = checkAccess()
        if (!hasAccess) {
            all = emptyList()
            allByTitle = emptyList()
            byPath = emptyMap()
            publish()
            return
        }
        isReading = true
        val started = System.currentTimeMillis()
        // Sorting thousands of titles takes a moment, so it happens off the main thread too.
        val (read, byTitle) = withContext(Dispatchers.IO) { read().let { it to it.sortedWith(compareBy(NameOrder) { t -> t.title }) } }
        Log.i("Nougat", "Read ${read.size} songs in ${System.currentTimeMillis() - started} ms")
        isReading = false
        hasRead = true
        if (read != all || readCount == 0) {
            all = read
            allByTitle = byTitle
            byPath = read.associateBy { it.path }
            publish()
            readCount++
        }
    }

    /** Hides a folder (everything under it) or a song. */
    fun hide(path: String) = saveHidden(hidden + path)

    fun unhide(path: String) = saveHidden(hidden - path)

    private fun saveHidden(paths: Set<String>) {
        hidden = paths
        prefs.edit().putStringSet(HIDDEN, paths).apply()
        publish()
    }

    private fun publish() {
        val visible = all.filterNot { isHidden(it.path, hidden) }
        if (visible != tracks) {
            tracks = visible
            tracksByTitle = allByTitle.filterNot { isHidden(it.path, hidden) }
        }
    }

    private val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private companion object {
        const val HIDDEN = "hidden"
        const val FOLDER_SORT = "librarySort"
        const val SONG_SORT = "songsSort"
    }

    private fun read(): List<Track> {
        val columns = buildList {
            add(MediaStore.Audio.Media._ID); add(MediaStore.Audio.Media.TITLE); add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM); add(MediaStore.Audio.Media.DURATION); add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_MODIFIED); add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= 29) { add(MediaStore.Audio.Media.RELATIVE_PATH); add(MediaStore.Audio.Media.VOLUME_NAME) }
            else add(@Suppress("DEPRECATION") MediaStore.Audio.Media.DATA)
        }
        val tracks = mutableListOf<Track>()
        context.contentResolver.query(collection, columns.toTypedArray(), "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null)?.use { c ->
            fun col(name: String) = c.getColumnIndexOrThrow(name)
            val id = col(MediaStore.Audio.Media._ID); val title = col(MediaStore.Audio.Media.TITLE)
            val artist = col(MediaStore.Audio.Media.ARTIST); val album = col(MediaStore.Audio.Media.ALBUM)
            val duration = col(MediaStore.Audio.Media.DURATION); val size = col(MediaStore.Audio.Media.SIZE)
            val modified = col(MediaStore.Audio.Media.DATE_MODIFIED); val name = col(MediaStore.Audio.Media.DISPLAY_NAME)
            val root = Environment.getExternalStorageDirectory().path
            while (c.moveToNext()) {
                val fileName = c.getString(name) ?: continue
                val path = if (Build.VERSION.SDK_INT >= 29) {
                    libraryPath(c.getString(col(MediaStore.Audio.Media.VOLUME_NAME)), c.getString(col(MediaStore.Audio.Media.RELATIVE_PATH)), fileName)
                } else {
                    libraryPath(c.getString(col(@Suppress("DEPRECATION") MediaStore.Audio.Media.DATA)) ?: continue, root)
                }
                tracks += Track(
                    path = path,
                    title = c.getString(title)?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.'),
                    artist = tag(c.getString(artist)),
                    album = tag(c.getString(album)),
                    durationMs = c.getLong(duration),
                    isPlayable = fileName.substringAfterLast('.', "").lowercase() !in unplayableTypes,
                    size = c.getLong(size),
                    modified = c.getLong(modified),
                    uri = ContentUris.withAppendedId(collection, c.getLong(id)).toString(),
                )
            }
        }
        // One entry per path: a file seen on two volumes' views must not show twice.
        return tracks.distinctBy { it.path }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.path })
    }
}

/** Whether `path` is one of `hidden` or lies inside a hidden folder. */
fun isHidden(path: String, hidden: Set<String>): Boolean {
    var p = path
    while (true) {
        if (p in hidden) return true
        val slash = p.lastIndexOf('/')
        if (slash < 0) return false
        p = p.substring(0, slash)
    }
}

/** MediaStore's stand-in for a missing tag. */
private fun tag(value: String?) = value?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }

/**
 * A library path from MediaStore's columns (Android 10 and later): relative to the storage root on
 * the phone's own storage, and under the volume's name on any other, such as an SD card.
 */
fun libraryPath(volume: String?, relativePath: String?, fileName: String): String {
    val folder = relativePath.orEmpty().trim('/')
    val inVolume = if (folder.isEmpty()) fileName else "$folder/$fileName"
    return if (volume == null || volume == "external_primary") inVolume else "$volume/$inVolume"
}

/** A library path from a file's absolute path (Android 8 and 9), given the phone's own storage root. */
fun libraryPath(absolute: String, primaryRoot: String): String {
    if (absolute.startsWith("$primaryRoot/")) return absolute.removePrefix("$primaryRoot/")
    // Another volume, such as /storage/1A2B-3C4D/Music/song.mp3: keep the volume's name in front.
    return absolute.removePrefix("/storage/").trimStart('/')
}
