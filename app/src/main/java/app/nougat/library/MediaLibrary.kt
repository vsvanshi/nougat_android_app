package app.nougat.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The permission that lets Nougat see audio files: READ_MEDIA_AUDIO from Android 13, storage before. */
val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

/**
 * The music library: every song in the system's media library (decision A9), read with MediaStore.
 * Nothing is copied. Compose screens read `tracks` and `hasAccess` and recompose when they change.
 */
class MediaLibrary(private val context: Context) {
    /** Sorted by path. */
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set

    /** The same songs sorted by title, made once per change (iPhone D60). */
    var tracksByTitle by mutableStateOf<List<Track>>(emptyList())
        private set

    var hasAccess by mutableStateOf(checkAccess())
        private set

    private fun checkAccess() = context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED

    /** Re-checks the permission and, with it, reads the media library again. */
    suspend fun refresh() {
        hasAccess = checkAccess()
        if (!hasAccess) {
            tracks = emptyList()
            tracksByTitle = emptyList()
            return
        }
        val started = System.currentTimeMillis()
        val read = withContext(Dispatchers.IO) { read() }
        Log.i("Nougat", "Read ${read.size} songs in ${System.currentTimeMillis() - started} ms")
        if (read != tracks) {
            tracks = read
            tracksByTitle = read.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }

    private fun read(): List<Track> {
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
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
