package app.nougat.playback

import kotlin.random.Random

enum class RepeatMode { Off, All, One }

/** The order songs play in. Pure data: no audio and no files. A song is its library path. */
class PlayQueue(items: List<String> = emptyList(), start: Int = 0) {
    /** Songs in the order they will play. */
    var items: List<String> = items
        private set
    var index = start.coerceIn(0, maxOf(0, items.size - 1))
        private set
    var isShuffled = false
        private set
    var repeatMode = RepeatMode.Off
    /** The order before shuffle was turned on, so that turning it off restores it. */
    private var unshuffled = items

    /** What the player should do after the queue changes. */
    sealed interface Step {
        data class Play(val path: String) : Step
        /** Start the current song again from the beginning. */
        data object Restart : Step
        /** Nothing left to play. */
        data object Stop : Step
    }

    val current get() = items.getOrNull(index)

    /**
     * Moves to the following song. `auto` is true when the current song simply ended,
     * which is the only time "repeat one" repeats.
     */
    fun next(auto: Boolean = false): Step {
        if (current == null) return Step.Stop
        if (auto && repeatMode == RepeatMode.One) return Step.Restart
        if (index + 1 < items.size) return Step.Play(items[++index])
        if (repeatMode == RepeatMode.Off) return Step.Stop
        index = 0
        return Step.Play(items[0])
    }

    /** Goes back. More than 3 seconds into a song, "previous" restarts it instead. */
    fun previous(elapsedMs: Long): Step {
        if (current == null) return Step.Stop
        if (elapsedMs > 3000) return Step.Restart
        if (index > 0) return Step.Play(items[--index])
        if (repeatMode != RepeatMode.All) return Step.Restart
        index = items.size - 1
        return Step.Play(items[index])
    }

    fun jump(position: Int): Step {
        if (position !in items.indices) return Step.Stop
        index = position
        return Step.Play(items[position])
    }

    /** Shuffles what is left around the current song, which keeps playing and becomes first. */
    fun setShuffled(on: Boolean, random: Random = Random.Default) {
        if (on == isShuffled) return
        isShuffled = on
        val current = current ?: return
        if (on) {
            unshuffled = items
            items = listOf(current) + items.filterIndexed { i, _ -> i != index }.shuffled(random)
            index = 0
        } else {
            items = unshuffled
            index = items.indexOf(current).coerceAtLeast(0)
        }
    }

    fun insert(songs: List<String>, position: Int) {
        val at = position.coerceIn(0, items.size)
        if (items.isNotEmpty() && at <= index) index += songs.size
        items = items.take(at) + songs + items.drop(at)
        unshuffled = if (isShuffled) unshuffled + songs else items
    }

    /** Removes a song. Returns a step only when the removed song was the current one. */
    fun remove(position: Int): Step? {
        if (position !in items.indices) return null
        val removed = items[position]
        items = items.filterIndexed { i, _ -> i != position }
        unshuffled = if (isShuffled) unshuffled.toMutableList().apply { remove(removed) } else items
        if (position < index) {
            index--
            return null
        }
        if (position != index) return null
        if (items.isEmpty()) {
            index = 0
            return Step.Stop
        }
        // The song that followed slides into this position; past the end, wrap to the start.
        if (index >= items.size) index = 0
        return Step.Play(items[index])
    }

    /**
     * Removes every song for which `gone` is true, such as files that were deleted.
     * Returns a step only when the current song was among them.
     */
    fun removeAll(gone: (String) -> Boolean): Step? {
        var step: Step? = null
        // Back to front, so positions still to be checked do not shift.
        for (position in items.indices.reversed()) {
            if (gone(items[position])) remove(position)?.let { step = it }
        }
        return step
    }

    /** Moves the song at `from` to `to`. The current song stays current. */
    fun move(from: Int, to: Int) {
        if (from !in items.indices) return
        val playing = current
        val list = items.toMutableList()
        list.add(to.coerceIn(0, list.size - 1), list.removeAt(from))
        items = list
        if (playing != null) index = items.indexOf(playing)
        if (!isShuffled) unshuffled = items
    }

    /** Everything needed to rebuild the queue after a restart. */
    data class Saved(val items: List<String>, val index: Int, val shuffled: Boolean, val repeat: RepeatMode, val unshuffled: List<String>)

    fun save() = Saved(items, index, isShuffled, repeatMode, unshuffled)

    companion object {
        fun restore(saved: Saved) = PlayQueue(saved.items, saved.index).apply {
            isShuffled = saved.shuffled
            repeatMode = saved.repeat
            unshuffled = saved.unshuffled
        }
    }
}
