package app.nougat.library

/**
 * One audio file in the library. `path` is relative to the storage root (for example
 * "Music/Road Trip/Kite Season.mp3") and is the track's identity, as on iPhone.
 */
data class Track(
    val path: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    /** False for formats Android cannot decode, such as WMA. These are listed but greyed out. */
    val isPlayable: Boolean,
    val size: Long,
    /** Seconds since 1970, from the file. */
    val modified: Long,
    /** The `content://` address the player opens. */
    val uri: String,
)

/** Audio that Android's player cannot decode; Android plays OGG and Opus, unlike iOS. */
val unplayableTypes = setOf("wma", "ape", "wv")

/** "3:42", or "1:02:03" from an hour up. */
fun playbackTime(ms: Long): String {
    val total = (ms + 500) / 1000
    val (hours, minutes, seconds) = Triple(total / 3600, total / 60 % 60, total % 60)
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
