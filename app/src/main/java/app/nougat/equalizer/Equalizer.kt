package app.nougat.equalizer

import app.nougat.library.folded
import kotlin.math.roundToInt

data class EqualizerPreset(
    val name: String,
    /** Decibels for the five bands, lowest frequency first. */
    val gains: List<Double>,
)

/** Everything the equalizer remembers. Pure data, so the rules can be tested without audio (D44). */
data class EqualizerState(
    val isOn: Boolean = false,
    val gains: List<Double> = listOf(0.0, 0.0, 0.0, 0.0, 0.0),
    val preamp: Double = 0.0,
    val presetName: String = "Flat",
    /** Presets the user saved. */
    val saved: List<EqualizerPreset> = emptyList(),
) {
    val presets get() = builtIn + saved

    /**
     * The gain for the whole signal: the user's preamp, lowered by as much as the strongest boost (D43).
     * Music is mastered close to full scale, so a boosted band with no headroom would distort.
     */
    val outputGain get() = preamp - maxOf(0.0, gains.maxOrNull() ?: 0.0)

    fun select(preset: EqualizerPreset) = copy(gains = preset.gains, presetName = preset.name)

    /** Moves one band, in half-decibel steps. The preset becomes whichever one the bands now match, or "Custom". */
    fun setGain(gain: Double, band: Int): EqualizerState {
        if (band !in gains.indices) return this
        val moved = gains.toMutableList().also { it[band] = step(gain, gainRange) }
        return copy(gains = moved, presetName = presets.firstOrNull { it.gains == moved }?.name ?: CUSTOM)
    }

    fun setPreamp(value: Double) = copy(preamp = step(value, preampRange))

    fun reset() = select(builtIn[0]).copy(preamp = 0.0)

    /**
     * Saves the current bands under `name`, replacing a saved preset of that name.
     * Returns null for a blank name, "Custom", or the name of a built-in preset.
     */
    fun save(name: String): EqualizerState? {
        val trimmed = name.trim()
        val taken = builtIn.map { it.name } + CUSTOM
        if (trimmed.isEmpty() || taken.any { same(it, trimmed) }) return null
        return copy(saved = saved.filterNot { same(it.name, trimmed) } + EqualizerPreset(trimmed, gains), presetName = trimmed)
    }

    fun delete(preset: EqualizerPreset): EqualizerState {
        val left = copy(saved = saved - preset)
        return if (presetName != preset.name) left
        else left.copy(presetName = left.presets.firstOrNull { it.gains == gains }?.name ?: CUSTOM)
    }

    companion object {
        val frequencies = listOf(60.0, 230.0, 910.0, 3_600.0, 14_000.0)
        val gainRange = -15.0..15.0
        val preampRange = -12.0..12.0
        /** What the bands are called when they match no preset. */
        const val CUSTOM = "Custom"
        val builtIn = listOf(
            EqualizerPreset("Flat", listOf(0.0, 0.0, 0.0, 0.0, 0.0)),
            EqualizerPreset("Bass boost", listOf(6.0, 4.0, 0.0, 1.0, 3.0)),
            EqualizerPreset("Rock", listOf(5.0, 3.0, -1.0, 3.0, 5.0)),
            EqualizerPreset("Vocal", listOf(-2.0, 0.0, 4.0, 3.0, 1.0)),
        )

        private fun step(value: Double, range: ClosedFloatingPointRange<Double>) =
            ((value * 2).roundToInt() / 2.0).coerceIn(range) + 0.0   // "+ 0.0" turns -0.0 into 0.0

        private fun same(a: String, b: String) = folded(a) == folded(b)
    }
}
