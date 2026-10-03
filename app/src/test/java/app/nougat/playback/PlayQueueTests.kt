package app.nougat.playback

import app.nougat.playback.PlayQueue.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

// Ported from the iPhone PlaybackTests.swift.

class PlayQueueTests {
    @Test
    fun queueStepsForwardAndStopsAtTheEnd() {
        val queue = PlayQueue(listOf("a", "b", "c"), 1)
        assertEquals("b", queue.current)
        assertEquals(Step.Play("c"), queue.next())
        assertEquals(Step.Stop, queue.next())
        assertEquals("c", queue.current)          // stays on the last song
    }

    @Test
    fun repeatModesChangeWhatHappensAtTheEnd() {
        val all = PlayQueue(listOf("a", "b"), 1).apply { repeatMode = RepeatMode.All }
        assertEquals(Step.Play("a"), all.next(auto = true))

        val one = PlayQueue(listOf("a", "b")).apply { repeatMode = RepeatMode.One }
        assertEquals(Step.Restart, one.next(auto = true))   // the song ended: repeat it
        assertEquals(Step.Play("b"), one.next())            // the user pressed next: move on
    }

    @Test
    fun previousRestartsAfterThreeSeconds() {
        val queue = PlayQueue(listOf("a", "b", "c"), 1)
        assertEquals(Step.Restart, queue.previous(10_000))
        assertEquals("b", queue.current)
        assertEquals(Step.Play("a"), queue.previous(1000))
        assertEquals(Step.Restart, queue.previous(1000))    // nothing before the first song

        queue.repeatMode = RepeatMode.All
        assertEquals(Step.Play("c"), queue.previous(1000))  // with repeat all, wrap to the end
    }

    @Test
    fun shuffleKeepsTheCurrentSongAndUndoes() {
        val random = Random(42)
        val queue = PlayQueue(listOf("a", "b", "c", "d"), 2)
        queue.setShuffled(true, random)
        assertEquals("c", queue.current)
        assertEquals(0, queue.index)
        assertEquals(setOf("a", "b", "c", "d"), queue.items.toSet())
        assertNotEquals(listOf("a", "b", "c", "d"), queue.items)

        queue.setShuffled(false, random)
        assertEquals(listOf("a", "b", "c", "d"), queue.items)
        assertEquals("c", queue.current)
    }

    @Test
    fun queueEditsKeepTheRightSongCurrent() {
        val queue = PlayQueue(listOf("a", "b", "c", "d"), 2)

        assertNull(queue.remove(0))                          // a song before the current one
        assertEquals("c", queue.current)
        assertEquals(Step.Play("d"), queue.remove(1))        // the current song: the next one takes over
        assertEquals(listOf("b", "d"), queue.items)

        queue.insert(listOf("x"), 0)
        assertEquals("d", queue.current)
        queue.move(2, 0)                                     // drag "d" to the top
        assertEquals(listOf("d", "x", "b"), queue.items)
        assertEquals("d", queue.current)

        assertEquals(Step.Play("b"), queue.jump(2))
        assertEquals(Step.Stop, queue.jump(9))

        val last = PlayQueue(listOf("only"))
        assertEquals(Step.Stop, last.remove(0))
        assertNull(last.current)
    }

    @Test
    fun emptyQueueDoesNothing() {
        val queue = PlayQueue()
        assertNull(queue.current)
        assertEquals(Step.Stop, queue.next())
        assertEquals(Step.Stop, queue.previous(0))
        queue.setShuffled(true)
        assertEquals(emptyList<String>(), queue.items)
    }

    @Test
    fun queueSurvivesSavingAndLoading() {
        val queue = PlayQueue(listOf("a", "b", "c"), 1).apply { repeatMode = RepeatMode.All; setShuffled(true) }
        val loaded = PlayQueue.restore(queue.save())
        assertEquals(queue.save(), loaded.save())
        loaded.setShuffled(false)
        assertEquals(listOf("a", "b", "c"), loaded.items)   // the order before shuffle came back too
    }

    @Test
    fun deletedSongsLeaveTheQueue() {
        // A song before the current one and one after it are deleted: the current song carries on.
        var queue = PlayQueue(listOf("a", "b", "c", "d"), 1)
        assertNull(queue.removeAll { it == "a" || it == "d" })
        assertEquals(listOf("b", "c"), queue.items)
        assertEquals("b", queue.current)

        // The current song is deleted: the next surviving one takes over.
        queue = PlayQueue(listOf("a", "b", "c", "d"), 1)
        assertEquals(Step.Play("d"), queue.removeAll { it == "b" || it == "c" })
        assertEquals(listOf("a", "d"), queue.items)

        // The current song was the last one left standing after it: wrap to the first survivor.
        queue = PlayQueue(listOf("a", "b", "c"), 2)
        assertEquals(Step.Play("b"), queue.removeAll { it != "b" })
        assertEquals(listOf("b"), queue.items)

        // Everything is deleted, as when the playing folder is removed.
        queue = PlayQueue(listOf("a", "b"), 0)
        assertEquals(Step.Stop, queue.removeAll { true })
        assertNull(queue.current)
    }
}
