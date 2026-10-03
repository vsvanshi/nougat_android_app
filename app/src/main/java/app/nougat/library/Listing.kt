package app.nougat.library

import java.text.Collator
import java.text.Normalizer

data class FolderSummary(
    val path: String,
    /** Songs in this folder and everything below it. */
    val songCount: Int,
    /** Date of the newest song inside, for sorting by date. */
    val modified: Long,
) {
    val name get() = path.substringAfterLast('/')
}

/** What one folder screen shows. */
data class FolderListing(
    val folders: List<FolderSummary> = emptyList(),
    val tracks: List<Track> = emptyList(),
    /** Songs in this folder and everything below it. */
    val songCount: Int = 0,
)

enum class LibrarySort { Name, DateAdded }

/**
 * Name order as people expect it, like iOS's `localizedStandardCompare`: case ignored and numbers
 * compared as numbers, so "Track 2" comes before "Track 10".
 */
val NameOrder: Comparator<String> = run {
    val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
    val chunks = Regex("\\d+|\\D+")
    Comparator { a, b ->
        val x = chunks.findAll(a).map { it.value }.iterator()
        val y = chunks.findAll(b).map { it.value }.iterator()
        while (x.hasNext() && y.hasNext()) {
            val p = x.next()
            val q = y.next()
            val c = if (p[0].isDigit() && q[0].isDigit()) {
                p.trimStart('0').length.compareTo(q.trimStart('0').length).takeIf { it != 0 } ?: p.trimStart('0').compareTo(q.trimStart('0'))
            } else collator.compare(p, q)
            if (c != 0) return@Comparator c
        }
        x.hasNext().compareTo(y.hasNext())
    }
}

/** Lower case with accents removed, for matching that ignores both. */
private fun folded(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

private fun parentOf(path: String) = path.substringBeforeLast('/', "")

/**
 * The subfolders and songs directly inside `folder` ("" is the top of the library).
 * The folder tree is derived from the track paths, so folders with no audio do not appear.
 */
fun List<Track>.listing(folder: String, sort: LibrarySort = LibrarySort.Name): FolderListing {
    val prefix = if (folder.isEmpty()) "" else "$folder/"
    val tracks = mutableListOf<Track>()
    val folders = mutableMapOf<String, FolderSummary>()
    var count = 0
    for (track in this) {
        if (!track.path.startsWith(prefix)) continue
        count++
        val slash = track.path.indexOf('/', prefix.length)
        if (slash < 0) {
            tracks += track
            continue
        }
        val path = track.path.substring(0, slash)
        val summary = folders[path] ?: FolderSummary(path, 0, Long.MIN_VALUE)
        folders[path] = summary.copy(songCount = summary.songCount + 1, modified = maxOf(summary.modified, track.modified))
    }
    return when (sort) {
        LibrarySort.Name -> FolderListing(
            folders.values.sortedWith(compareBy(NameOrder) { it.name }),
            tracks.sortedWith(compareBy(NameOrder) { it.title }), count,
        )
        LibrarySort.DateAdded -> FolderListing(
            folders.values.sortedByDescending { it.modified }, tracks.sortedByDescending { it.modified }, count,
        )
    }
}

/**
 * Songs whose title, artist or album match `query`, and folders whose name matches,
 * ignoring case and accents. An empty query matches nothing.
 */
fun List<Track>.search(query: String): FolderListing {
    val term = folded(query.trim())
    if (term.isEmpty()) return FolderListing()
    val tracks = mutableListOf<Track>()
    val folders = mutableMapOf<String, FolderSummary>()
    for (track in this) {
        if (listOfNotNull(track.title, track.artist, track.album).any { term in folded(it) }) tracks += track
        // Every folder above the track whose own name matches.
        var path = parentOf(track.path)
        while (path.isNotEmpty()) {
            if (term in folded(path.substringAfterLast('/'))) {
                val summary = folders[path] ?: FolderSummary(path, 0, Long.MIN_VALUE)
                folders[path] = summary.copy(songCount = summary.songCount + 1, modified = maxOf(summary.modified, track.modified))
            }
            path = parentOf(path)
        }
    }
    val sorted = tracks.sortedWith(compareBy(NameOrder) { it.title })
    return FolderListing(folders.values.sortedWith(compareBy(NameOrder) { it.name }), sorted, sorted.size)
}

/** Every song in `folder` and below it, in library order. */
fun List<Track>.under(folder: String) = if (folder.isEmpty()) this else filter { it.path.startsWith("$folder/") }

/**
 * The deepest folder that holds every song, shown as the top of the Folders tab, so a library that
 * lives entirely in "Music" opens inside it rather than on a lone "Music" folder (decision A17).
 */
fun List<Track>.commonFolder(): String {
    if (isEmpty()) return ""
    var common = parentOf(first().path)
    for (track in this) {
        while (common.isNotEmpty() && !track.path.startsWith("$common/")) common = parentOf(common)
        if (common.isEmpty()) break
    }
    return common
}

/** "1 song", "2 songs". */
fun count(number: Int, noun: String) = "$number $noun${if (number == 1) "" else "s"}"
