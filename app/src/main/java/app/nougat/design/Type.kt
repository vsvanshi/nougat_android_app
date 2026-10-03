package app.nougat.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The text styles of the iPhone DESIGN.md section 3, in sp so they follow the system font size.
 * Roboto is the system font on Android, so nothing is bundled. Numbers use tabular figures.
 */
object Type {
    private fun style(size: Int, line: Int, medium: Boolean = false) = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontFeatureSettings = "tnum",
    )

    val largeTitle = style(34, 40)
    val title = style(22, 28, medium = true)
    val barTitle = style(17, 22, medium = true)
    val rowTitle = style(16, 24)
    val body = style(14, 20)
    val bodyStrong = style(14, 20, medium = true)
    val caption = style(12, 16)

    /** Button labels. Callers pass the text in capitals (`uppercase()`); Compose styles cannot. */
    val button = style(14, 20, medium = true).copy(letterSpacing = 0.04.em)
}
