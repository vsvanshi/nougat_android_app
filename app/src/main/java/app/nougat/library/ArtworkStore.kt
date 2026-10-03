package app.nougat.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads and caches cover art: embedded in the file first, then a `cover` or `folder` image beside it.
 * From Android 10 the system's media library supplies both (`loadThumbnail`), which also works with
 * audio-only access; before that Nougat reads the file and its folder itself.
 */
class ArtworkStore(context: Context) {
    private val context = context.applicationContext
    private val folder = File(context.cacheDir, "artwork").apply { mkdirs() }
    private val memory = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    /** Songs already found to have no art, so they are not read again while the app runs. */
    private val missing = ConcurrentHashMap.newKeySet<String>()

    /** The thumbnail if it is already in memory, so a row can draw it on its first frame. */
    fun cached(track: Track): Bitmap? = memory.get(key(track))

    /** A small image for lists, from memory, then the cache folder, then the file itself. */
    suspend fun thumbnail(track: Track): Bitmap? = withContext(Dispatchers.IO) {
        val key = key(track)
        memory.get(key)?.let { return@withContext it }
        if (key in missing) return@withContext null
        val file = File(folder, "$key.jpg")
        val image = BitmapFactory.decodeFile(file.path) ?: load(track, THUMBNAIL)?.also { small ->
            runCatching { file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 85, it) } }
        }
        if (image == null) missing += key else memory.put(key, image)
        image
    }

    /** A large image, for Now playing and the lock screen. Not cached. */
    suspend fun fullImage(track: Track): Bitmap? = withContext(Dispatchers.IO) { load(track, 1024) }

    private fun load(track: Track, side: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) {
            return runCatching { context.contentResolver.loadThumbnail(Uri.parse(track.uri), Size(side, side), null) }.getOrNull()
        }
        val embedded = runCatching {
            MediaMetadataRetriever().run {
                try { setDataSource(context, Uri.parse(track.uri)); embeddedPicture } finally { release() }
            }
        }.getOrNull()
        if (embedded != null) return decode(side) { BitmapFactory.decodeByteArray(embedded, 0, embedded.size, it) }
        val dir = fileOf(track).parentFile ?: return null
        val cover = dir.listFiles()?.firstOrNull { it.name.lowercase() in coverNames } ?: return null
        return decode(side) { BitmapFactory.decodeFile(cover.path, it) }
    }

    /** Decodes at the smallest power-of-two reduction that still covers `side` pixels. */
    private fun decode(side: Int, read: (BitmapFactory.Options?) -> Bitmap?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        read(bounds)
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= side) sample *= 2
        return read(BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** The file on Android 8 and 9, where paths come from the file system. */
    private fun fileOf(track: Track): File {
        val primary = File(Environment.getExternalStorageDirectory(), track.path)
        return if (primary.exists()) primary else File("/storage", track.path)
    }

    /** The date is part of the key, so replacing a file drops its old thumbnail. */
    private fun key(track: Track) = MessageDigest.getInstance("SHA-1")
        .digest("${track.path}|${track.modified}".toByteArray()).take(12).joinToString("") { "%02x".format(it) }

    private companion object {
        /** Pixels: a 40 dp thumbnail on a 3x screen. */
        const val THUMBNAIL = 120
        val coverNames = setOf("cover.jpg", "folder.jpg", "cover.jpeg", "folder.jpeg", "cover.png", "folder.png")
    }
}
