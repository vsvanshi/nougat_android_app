package app.nougat.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp

/** Sizes from the iPhone DESIGN.md section 4, in dp. */
object Metrics {
    val grid = 8.dp
    val margin = 16.dp
    val keyline = 72.dp
    val rowTwoLine = 72.dp
    val rowOneLine = 56.dp
    val subheader = 48.dp
    val touchTarget = 48.dp
    val icon = 24.dp
    val thumbnail = 40.dp
    val headerBottomPadding = 26.dp
    val headerPlayButton = 56.dp
    val nowPlayingPlayButton = 64.dp
    val buttonHeight = 40.dp
    val chipHeight = 32.dp

    /** The filled circle behind a toggle icon that is on, such as shuffle. */
    val toggleCircle = 40.dp
}

/** Corner radii from section 5. Capsules and circles use the shapes directly. */
object Radius {
    val thumbnail = 6.dp
    val card = 12.dp
}

/** Motion from section 5, for our own animations only. The curve (0.4, 0, 0.2, 1) is Compose's FastOutSlowIn. */
object Motion {
    val easing = FastOutSlowInEasing
    const val standard = 300
    const val entering = 225
    const val leaving = 195

    /** What our animations become with animations removed. */
    const val fade = 150
}
