package app.nougat.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The fixed colour tokens of the iPhone DESIGN.md section 2, for one appearance. */
class Colors(
    val background: Color, val paper: Color, val surface: Color, val fill: Color, val hairline: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val header: Color, val onHeader: Color, val onHeader2: Color,
    val avatar: Color, val onAvatar: Color, val inverse: Color, val onInverse: Color,
) {
    /** Every token with its name, for the gallery and the tests. */
    val named get() = listOf(
        "background" to background, "paper" to paper, "surface" to surface, "fill" to fill,
        "hairline" to hairline, "ink" to ink, "ink2" to ink2, "ink3" to ink3,
        "header" to header, "onHeader" to onHeader, "onHeader2" to onHeader2,
        "avatar" to avatar, "onAvatar" to onAvatar, "inverse" to inverse, "onInverse" to onInverse,
    )
}

/** An opaque colour from a 0xRRGGBB value, as the design writes them. */
fun rgb(hex: Int) = Color(0xFF000000.toInt() or hex)

val LightColors = Colors(
    background = rgb(0xECEFF1), paper = rgb(0xFCFDFD), surface = rgb(0xFCFDFD), fill = rgb(0xE3E8EB),
    hairline = rgb(0xCFD8DC), ink = rgb(0x1F272B), ink2 = rgb(0x546E7A), ink3 = rgb(0x90A4AE),
    header = rgb(0x263238), onHeader = rgb(0xF7F9FA), onHeader2 = rgb(0xB0BEC5),
    avatar = rgb(0x78909C), onAvatar = rgb(0xFCFDFD), inverse = rgb(0x323B40), onInverse = rgb(0xECEFF1),
)

val DarkColors = Colors(
    background = rgb(0x151C20), paper = rgb(0x1C2529), surface = rgb(0x263238), fill = rgb(0x2F3D44),
    hairline = rgb(0x37474F), ink = rgb(0xECEFF1), ink2 = rgb(0xB0BEC5), ink3 = rgb(0x78909C),
    header = rgb(0x263238), onHeader = rgb(0xF7F9FA), onHeader2 = rgb(0xB0BEC5),
    avatar = rgb(0x455A64), onAvatar = rgb(0xCFD8DC), inverse = rgb(0xECEFF1), onInverse = rgb(0x1F272B),
)

val LocalColors = staticCompositionLocalOf { LightColors }
