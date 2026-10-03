package app.nougat.visualizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The bar heights and wave the visualizer draws. Written on the audio thread, read on every frame;
 * each array is replaced whole, so a reader never sees half of one.
 */
class Spectrum {
    @Volatile var levels = FloatArray(BARS)
    /** The shape of the sound itself, -1 to 1, for the oscilloscope. */
    @Volatile var wave = FloatArray(WAVE)

    fun reset() {
        levels = FloatArray(BARS)
        wave = FloatArray(WAVE)
    }

    companion object {
        const val BARS = 24
        const val WAVE = 128
    }
}

/** Turns a slice of audio into bar heights with a Fourier transform, as on iPhone (`Spectrum.swift`). */
class SpectrumAnalyzer {
    private val window = FloatArray(SIZE) { (0.5 - 0.5 * cos(2 * PI * it / SIZE)).toFloat() }
    private val real = DoubleArray(SIZE)
    private val imaginary = DoubleArray(SIZE)

    /** `WAVE` points taken evenly from a slice of `SIZE` mono samples. */
    fun wave(samples: FloatArray) = FloatArray(Spectrum.WAVE) { samples[it * (SIZE / Spectrum.WAVE)] }

    /**
     * `samples` holds `SIZE` samples, the channels already summed; `channels` says how many were summed,
     * so a full-volume tone measures the same in mono and stereo.
     */
    fun levels(samples: FloatArray, channels: Int, sampleRate: Int): FloatArray {
        val half = SIZE / 2
        for (i in 0 until SIZE) {
            real[i] = (samples[i] * window[i]).toDouble()
            imaginary[i] = 0.0
        }
        fft(real, imaginary)
        // What a tone at full volume measures, so levels are in decibels below full volume.
        // (Half the window's sum times the amplitude; the Hann window sums to SIZE / 2.)
        val fullScale = SIZE / 4.0 * channels
        val levels = FloatArray(Spectrum.BARS)
        var lower = 1
        for (bar in 0 until Spectrum.BARS) {
            val top = LOWEST * SPAN.pow((bar + 1).toDouble() / Spectrum.BARS)
            // Every bar gets at least one frequency of its own, so the lowest bars do not repeat each other.
            val upper = min(half, max(lower + 1, (top * SIZE / sampleRate).toInt()))
            var peak = 0.0
            for (k in lower until upper) peak = max(peak, hypot(real[k], imaginary[k]))
            lower = upper
            if (peak <= 0) continue
            // Music holds far less energy in its high notes; the tilt of 3 dB an octave evens the bars out.
            val decibels = 20 * log10(peak / fullScale) + 3 * log2(top / LOWEST)
            // The curve keeps quiet passages low, so the beat stands out.
            levels[bar] = ((decibels + 48) / 48).coerceIn(0.0, 1.0).pow(1.4).toFloat()
        }
        return levels
    }

    companion object {
        /** Samples per slice: about 23 ms of music, short enough for the bars to follow the beat. */
        const val SIZE = 1024
        /** The bars cover 50 Hz to 16 kHz, each the same musical width. */
        private const val LOWEST = 50.0
        private const val SPAN = 320.0

        /** In-place radix-2 FFT. `re.size` must be a power of two. */
        fun fft(re: DoubleArray, im: DoubleArray) {
            val n = re.size
            var j = 0
            for (i in 1 until n) {
                var bit = n shr 1
                while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
                j = j xor bit
                if (i < j) {
                    re[i] = re[j].also { re[j] = re[i] }
                    im[i] = im[j].also { im[j] = im[i] }
                }
            }
            var length = 2
            while (length <= n) {
                val angle = -2 * PI / length
                val wr = cos(angle)
                val wi = sin(angle)
                for (start in 0 until n step length) {
                    var cr = 1.0
                    var ci = 0.0
                    for (k in 0 until length / 2) {
                        val a = start + k
                        val b = a + length / 2
                        val tr = re[b] * cr - im[b] * ci
                        val ti = re[b] * ci + im[b] * cr
                        re[b] = re[a] - tr; im[b] = im[a] - ti
                        re[a] += tr; im[a] += ti
                        val next = cr * wr - ci * wi
                        ci = cr * wi + ci * wr
                        cr = next
                    }
                }
                length = length shl 1
            }
        }
    }
}
