package app.nougat.design

import android.content.Context
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The accent the user picks in Settings. Values are the iPhone DESIGN.md section 2, "Accents". */
enum class Accent(val spec: Spec?) {
    Teal(Spec(0x009688, 0x00796B, 0x00796B, 0xFCFDFD, 0x009688, 0x80CBC4, 0x0E2A27, 0x26A69A)),
    Amber(Spec(0xFFC107, 0x8A5D00, 0xFFC107, 0x2B2000, 0xFFB300, 0xFFD54F, 0x2B2000, 0xFFB300)),
    Orange(Spec(0xFF5722, 0xBF360C, 0xBF360C, 0xFCFDFD, 0xFF5722, 0xFFAB91, 0x3B1206, 0xF4511E)),
    Pink(Spec(0xE91E63, 0xC2185B, 0xC2185B, 0xFCFDFD, 0xE91E63, 0xF48FB1, 0x3D0A20, 0xEC407A)),
    Indigo(Spec(0x7986CB, 0x3F51B5, 0x3F51B5, 0xFCFDFD, 0x3F51B5, 0x9FA8DA, 0x121A45, 0x5C6BC0)),
    Blue(Spec(0x1E88E5, 0x1565C0, 0x1565C0, 0xFCFDFD, 0x1E88E5, 0x90CAF9, 0x0B2A4A, 0x1E88E5)),

    /** The wallpaper colour (Material You), Android 12 and later only (decision A14). */
    System(null);

    /** Hex values. In dark mode accent, text and fill share one colour. */
    class Spec(
        val accent: Int, val text: Int, val fill: Int, val onAccent: Int, val switchOn: Int,
        val darkAccent: Int, val darkOnAccent: Int, val darkSwitchOn: Int,
    ) {
        fun colors(dark: Boolean) = if (dark) {
            AccentColors(rgb(darkAccent), rgb(darkAccent), rgb(darkAccent), rgb(darkOnAccent), rgb(darkSwitchOn), rgb(text))
        } else {
            // The snackbar is the inverse surface, so it takes the other mode's accent.
            AccentColors(rgb(accent), rgb(text), rgb(fill), rgb(onAccent), rgb(switchOn), rgb(darkAccent))
        }

        companion object {
            /**
             * An accent from a Material tonal palette, where tone is lightness (L*) from 0 to 100.
             * Contrast depends only on the tone gap, so these tones meet the same rules as the six
             * fixed accents whatever the hue (checked in the tests).
             */
            fun fromTones(tone: (Int) -> Int) = Spec(
                accent = tone(60), text = tone(40), fill = tone(40), onAccent = tone(100), switchOn = tone(50),
                darkAccent = tone(80), darkOnAccent = tone(20), darkSwitchOn = tone(60),
            )
        }
    }

    fun spec(context: Context): Spec = spec ?: systemSpec(context)

    companion object {
        val available get() = entries.filter { it != System || Build.VERSION.SDK_INT >= 31 }

        private const val KEY = "accent"

        fun load(context: Context): Accent {
            val name = prefs(context).getString(KEY, null)
            return available.firstOrNull { it.name == name } ?: Teal
        }

        fun save(context: Context, accent: Accent) = prefs(context).edit().putString(KEY, accent.name).apply()

        private fun prefs(context: Context) = context.getSharedPreferences("nougat", Context.MODE_PRIVATE)

        private fun systemSpec(context: Context): Spec {
            if (Build.VERSION.SDK_INT < 31) return Teal.spec!!
            // system_accent1_N holds tone 100 - N / 10.
            val ids = mapOf(
                20 to android.R.color.system_accent1_800, 40 to android.R.color.system_accent1_600,
                50 to android.R.color.system_accent1_500, 60 to android.R.color.system_accent1_400,
                80 to android.R.color.system_accent1_200, 100 to android.R.color.system_accent1_0,
            )
            return Spec.fromTones { context.getColor(ids.getValue(it)) and 0xFFFFFF }
        }
    }
}

/** The accent tokens of the design, resolved for the chosen accent and the current appearance. */
class AccentColors(
    val accent: Color, val text: Color, val fill: Color, val onAccent: Color, val switchOn: Color, val onInverse: Color,
) {
    val named get() = listOf(
        "accent" to accent, "accentText" to text, "accentFill" to fill,
        "onAccent" to onAccent, "switchOn" to switchOn, "accentOnInverse" to onInverse,
    )
}

val LocalAccent = staticCompositionLocalOf { Accent.Teal.spec!!.colors(dark = false) }
