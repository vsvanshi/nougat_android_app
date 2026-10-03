package app.nougat.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the iPhone EqualizerTests.swift.

class EqualizerTests {
    private val flat = listOf(0.0, 0.0, 0.0, 0.0, 0.0)

    @Test
    fun choosingAPresetSetsTheBands() {
        var state = EqualizerState()
        assertEquals("Flat", state.presetName)
        assertEquals(flat, state.gains)
        state = state.select(EqualizerState.builtIn[1])
        assertEquals("Bass boost", state.presetName)
        assertEquals(listOf(6.0, 4.0, 0.0, 1.0, 3.0), state.gains)
    }

    @Test
    fun movingABandMakesItCustomUntilItMatchesAPresetAgain() {
        var state = EqualizerState().setGain(4.3, 0)
        assertEquals(4.5, state.gains[0], 0.0)              // half-decibel steps
        assertEquals("Custom", state.presetName)

        state = state.setGain(99.0, 1)
        assertEquals(15.0, state.gains[1], 0.0)             // clamped to the range
        state = state.setGain(-99.0, 1)
        assertEquals(-15.0, state.gains[1], 0.0)
        state = state.setGain(1.0, 7)                       // no such band: ignored
        assertEquals(5, state.gains.size)

        state = state.setGain(0.0, 0).setGain(-0.1, 1)
        assertEquals(flat, state.gains)
        assertEquals("Flat", state.presetName)              // back on a preset, so it is named again
    }

    @Test
    fun boostsAreGivenHeadroom() {
        var state = EqualizerState()
        assertEquals(0.0, state.outputGain, 0.0)
        state = state.select(EqualizerState.builtIn[1])     // strongest band is +6 dB
        assertEquals(-6.0, state.outputGain, 0.0)
        state = state.setPreamp(2.2)
        assertEquals(2.0, state.preamp, 0.0)
        assertEquals(-4.0, state.outputGain, 0.0)

        state = state.select(EqualizerPreset("Cuts", listOf(-3.0, -6.0, 0.0, -1.0, -2.0)))
        assertEquals(2.0, state.outputGain, 0.0)            // nothing boosted: only the preamp applies
        assertEquals(12.0, state.setPreamp(40.0).preamp, 0.0)
    }

    @Test
    fun presetsCanBeSavedReplacedAndDeleted() {
        var state = EqualizerState().setGain(3.0, 2)
        assertNull(state.save("  "))
        assertNull(state.save("flat"))                      // a built-in name
        assertNull(state.save("Custom"))
        state = state.save(" Car ")!!
        assertEquals("Car", state.presetName)
        assertEquals(listOf("Flat", "Bass boost", "Rock", "Vocal", "Car"), state.presets.map { it.name })

        state = state.setGain(5.0, 2)
        assertEquals("Custom", state.presetName)
        state = state.save("car")!!                         // same name: replaced, not duplicated
        assertEquals(1, state.saved.size)
        assertEquals(5.0, state.saved[0].gains[2], 0.0)

        state = state.delete(state.saved[0])
        assertTrue(state.saved.isEmpty())
        assertEquals("Custom", state.presetName)

        state = state.reset()
        assertEquals("Flat", state.presetName)
        assertEquals(0.0, state.preamp, 0.0)
    }
}
