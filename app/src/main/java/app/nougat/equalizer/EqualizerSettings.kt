package app.nougat.equalizer

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.ln

/** The equalizer's settings, saved as `equalizer.json`, and applied to the sound on every change. */
class EqualizerSettings(private val file: File) {
    var state by mutableStateOf(read())
        private set

    /** Set by the player; called with the new state on every change. */
    var onChange: (EqualizerState) -> Unit = {}

    /** Changes the settings and applies them at once. Pass `persist = false` while a slider is dragged, then call `persist()`. */
    fun update(persist: Boolean = true, change: (EqualizerState) -> EqualizerState) {
        state = change(state)
        onChange(state)
        if (persist) persist()
    }

    fun persist() {
        val s = state
        val json = JSONObject().put("isOn", s.isOn).put("gains", JSONArray(s.gains)).put("preamp", s.preamp)
            .put("presetName", s.presetName)
            .put("saved", JSONArray(s.saved.map { JSONObject().put("name", it.name).put("gains", JSONArray(it.gains)) }))
        runCatching { file.writeText(json.toString()) }
    }

    private fun read(): EqualizerState = runCatching {
        val o = JSONObject(file.readText())
        fun gains(a: JSONArray) = List(a.length()) { a.getDouble(it) }
        val saved = o.getJSONArray("saved")
        EqualizerState(
            o.getBoolean("isOn"), gains(o.getJSONArray("gains")), o.getDouble("preamp"), o.getString("presetName"),
            List(saved.length()) { saved.getJSONObject(it).let { p -> EqualizerPreset(p.getString("name"), gains(p.getJSONArray("gains"))) } },
        )
    }.getOrDefault(EqualizerState())
}

/**
 * Applies the equalizer to the player's audio session. Android 9 and later: `DynamicsProcessing`,
 * five bands split halfway (in octaves) between Nougat's band frequencies, and an input gain for the
 * preamp and headroom (D43). Android 8: the platform `Equalizer`, each of its bands following the
 * nearest of Nougat's, with the preamp added to every band since it has no gain of its own.
 */
class SoundEffects(sessionId: Int) {
    private val dynamics: DynamicsProcessing? = if (Build.VERSION.SDK_INT >= 28) runCatching {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2, true, BANDS, false, 0, false, 0, false,
        ).build()
        DynamicsProcessing(0, sessionId, config)
    }.onFailure { Log.w("Nougat", "No DynamicsProcessing", it) }.getOrNull() else null

    private val legacy: Equalizer? = if (dynamics == null) runCatching { Equalizer(0, sessionId) }.getOrNull() else null

    fun apply(state: EqualizerState) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) dynamics?.let { dp ->
                dp.setPreEqAllChannelsTo(DynamicsProcessing.Eq(true, true, BANDS))
                for (band in 0 until BANDS) {
                    dp.setPreEqBandAllChannelsTo(band, DynamicsProcessing.EqBand(true, CUTOFFS[band], state.gains[band].toFloat()))
                }
                dp.setInputGainAllChannelsTo(state.outputGain.toFloat())
                dp.enabled = state.isOn
            }
            legacy?.let { eq ->
                val (low, high) = eq.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
                for (band in 0 until eq.numberOfBands) {
                    val hz = eq.getCenterFreq(band.toShort()) / 1000.0
                    val ours = EqualizerState.frequencies.indices.minBy { abs(ln(EqualizerState.frequencies[it] / hz)) }
                    val millibels = ((state.gains[ours] + state.outputGain) * 100).toInt().coerceIn(low, high)
                    eq.setBandLevel(band.toShort(), millibels.toShort())
                }
                eq.enabled = state.isOn
            }
        }.onFailure { Log.w("Nougat", "Equalizer not applied", it) }
    }

    private companion object {
        const val BANDS = 5
        /** Upper edge of each band: the geometric middle between neighbouring band frequencies, then 20 kHz. */
        val CUTOFFS = floatArrayOf(117f, 457f, 1810f, 7100f, 20_000f)
    }
}
