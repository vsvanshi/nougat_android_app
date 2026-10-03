package app.nougat.design

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.roundToInt

// Ported from the iPhone NougatTests/DesignSystemTests.swift: these check the code against
// the design, so a wrong hex or a low-contrast accent fails the build.

private fun hex(color: androidx.compose.ui.graphics.Color) = color.toArgb() and 0xFFFFFF

/** WCAG relative luminance and contrast ratio. */
private fun luminance(hex: Int): Double {
    fun channel(shift: Int): Double {
        val c = ((hex shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

private fun contrast(a: Int, b: Int): Double {
    val (hi, lo) = maxOf(luminance(a), luminance(b)) to minOf(luminance(a), luminance(b))
    return (hi + 0.05) / (lo + 0.05)
}

/** A grey at a Material tone (CIE L*), to stand in for any hue at that tone. */
private fun greyAtTone(tone: Int): Int {
    val l = tone.toDouble()
    val y = if (l > 8) ((l + 16) / 116).pow(3) else l / 903.3
    val c = if (y <= 0.0031308) 12.92 * y else 1.055 * y.pow(1 / 2.4) - 0.055
    val v = (c * 255).roundToInt().coerceIn(0, 255)
    return (v shl 16) or (v shl 8) or v
}

class DesignSystemTests {
    @Test
    fun fixedColoursMatchTheDesign() {
        val expected = listOf(
            Triple("background", 0xECEFF1, 0x151C20), Triple("paper", 0xFCFDFD, 0x1C2529),
            Triple("surface", 0xFCFDFD, 0x263238), Triple("fill", 0xE3E8EB, 0x2F3D44),
            Triple("hairline", 0xCFD8DC, 0x37474F), Triple("ink", 0x1F272B, 0xECEFF1),
            Triple("ink2", 0x546E7A, 0xB0BEC5), Triple("ink3", 0x90A4AE, 0x78909C),
            Triple("header", 0x263238, 0x263238), Triple("onHeader", 0xF7F9FA, 0xF7F9FA),
            Triple("onHeader2", 0xB0BEC5, 0xB0BEC5), Triple("avatar", 0x78909C, 0x455A64),
            Triple("onAvatar", 0xFCFDFD, 0xCFD8DC), Triple("inverse", 0x323B40, 0xECEFF1),
            Triple("onInverse", 0xECEFF1, 0x1F272B),
        )
        assertEquals(expected.map { it.first }, LightColors.named.map { it.first })
        for ((i, e) in expected.withIndex()) {
            assertEquals("${e.first} light", e.second, hex(LightColors.named[i].second))
            assertEquals("${e.first} dark", e.third, hex(DarkColors.named[i].second))
        }
    }

    @Test
    fun fixedAccentsMeetTheContrastRules() {
        for (accent in Accent.entries) accent.spec?.let { checkContrast(accent.name, it) }
    }

    @Test
    fun systemAccentMeetsTheContrastRulesAtAnyHue() {
        checkContrast("System", Accent.Spec.fromTones(::greyAtTone))
    }

    @Test
    fun darkModeUsesOneColourForAccentTextAndFill() {
        val c = Accent.Teal.spec!!.colors(dark = true)
        assertEquals(0x80CBC4, hex(c.accent))
        assertEquals(c.accent, c.text)
        assertEquals(c.accent, c.fill)
        assertEquals("snackbar action takes the light text colour", 0x00796B, hex(c.onInverse))
        assertEquals(0x80CBC4, hex(Accent.Teal.spec!!.colors(dark = false).onInverse))
    }

    private fun checkContrast(name: String, s: Accent.Spec) {
        val paper = hex(LightColors.paper) to hex(DarkColors.paper)
        val header = hex(LightColors.header)
        val inverse = hex(LightColors.inverse) to hex(DarkColors.inverse)
        fun expect(ratio: Double, min: Double, what: String) =
            assertTrue("$name: $what is ${"%.2f".format(ratio)}, needs $min", ratio >= min)

        // Light mode
        expect(contrast(s.onAccent, s.accent), 3.0, "play glyph on accent")
        expect(contrast(s.onAccent, s.fill), 4.5, "label on filled button")
        expect(contrast(s.text, paper.first), 4.5, "accent text and sliders on paper")
        expect(contrast(s.accent, header), 3.0, "play button against the header")
        expect(contrast(s.darkAccent, inverse.first), 4.5, "snackbar action")
        // Dark mode
        expect(contrast(s.darkAccent, paper.second), 4.5, "accent text on dark paper")
        expect(contrast(s.darkOnAccent, s.darkAccent), 4.5, "label on dark accent")
        expect(contrast(s.darkAccent, header), 3.0, "play button against the header, dark")
        expect(contrast(s.text, inverse.second), 4.5, "snackbar action, dark")
    }
}

class SliderTests {
    @Test
    fun sliderMapsTouchesToValues() {
        // Seek bar: 0..1 over 200 dp
        assertEquals(0.25f, sliderValue(50f, 200f, 0f..1f))
        // Equalizer band: -15..15, the middle of the track is 0 dB
        assertEquals(0f, sliderValue(120f, 240f, -15f..15f))
        // Touches beyond either end clamp
        assertEquals(-15f, sliderValue(-30f, 240f, -15f..15f))
        assertEquals(15f, sliderValue(999f, 240f, -15f..15f))
        // A track with no length yet must not divide by zero
        assertEquals(0f, sliderValue(10f, 0f, 0f..1f))

        assertEquals(0.7f, sliderFraction(6f, -15f..15f), 1e-6f)
        assertEquals(1f, sliderFraction(40f, -15f..15f))
        assertEquals(0f, sliderFraction(1f, 5f..5f))
    }
}
