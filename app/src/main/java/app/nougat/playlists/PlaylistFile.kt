package app.nougat.playlists

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Reads and writes the playlists as one small JSON file, with Android's built-in `org.json`. */
class PlaylistFile(private val file: File) {
    fun read(): List<Playlist> = runCatching {
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val songs = o.getJSONArray("songs")
            Playlist(o.getString("id"), o.getString("name"), List(songs.length()) { songs.getString(it) })
        }
    }.getOrDefault(emptyList())

    fun write(playlists: List<Playlist>) {
        val array = JSONArray(playlists.map { JSONObject().put("id", it.id).put("name", it.name).put("songs", JSONArray(it.songs)) })
        runCatching { file.writeText(array.toString()) }
    }
}
