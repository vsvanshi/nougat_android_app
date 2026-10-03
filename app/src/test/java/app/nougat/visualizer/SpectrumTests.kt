package app.nougat.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

// Ported from aToneLightsTheBarForItsPitch in the iPhone PlaybackTests.swift.

class SpectrumTests {
    @Test
    fun aToneLightsTheBarForItsPitch() {
        val analyzer = SpectrumAnalyzer()
        val rate = 44_100
        val tone = FloatArray(SpectrumAnalyzer.SIZE) { (0.5 * sin(2 * PI * 1_000 * it / rate)).toFloat() }
        val levels = analyzer.levels(tone, 1, rate)
        // 1 kHz falls in the thirteenth of 24 bars between 50 Hz and 16 kHz.
        assertEquals(12, levels.indices.maxBy { levels[it] })
        assertTrue("${levels[12]}", levels[12] > 0.5f)
        assertTrue(levels[2] == 0f && levels[22] == 0f)

        val silence = FloatArray(SpectrumAnalyzer.SIZE)
        assertTrue(analyzer.levels(silence, 2, rate).all { it == 0f })
    }

    @Test
    fun fftFindsASingleFrequency() {
        val n = 16
        val re = DoubleArray(n) { sin(2 * PI * 3 * it / n) }
        val im = DoubleArray(n)
        SpectrumAnalyzer.fft(re, im)
        val magnitudes = (0 until n / 2).map { kotlin.math.hypot(re[it], im[it]) }
        assertEquals(3, magnitudes.indices.maxBy { magnitudes[it] })
        assertEquals(n / 2.0, magnitudes[3], 1e-9)
    }
}
