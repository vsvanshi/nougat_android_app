package app.nougat.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.DpOffset
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

/** Shadow levels from section 5. Light mode is tinted with the header colour, dark mode is black. */
enum class Depth {
    /** Filled buttons, cards. */
    One,
    /** Play buttons, snackbar. */
    Three,
}

/** Draws a design depth level under `shape`. The design quotes CSS blurs, which Compose's radius matches. */
@Composable
fun Modifier.depth(level: Depth, shape: Shape): Modifier {
    val tint = LightColors.header
    fun s(y: Int, blur: Int, color: Color, alpha: Float) = Shadow(blur.dp, color.copy(alpha = alpha), offset = DpOffset(0.dp, y.dp))
    val shadows = when (level to isSystemInDarkTheme()) {
        Depth.One to true -> listOf(s(1, 3, Color.Black, 0.4f))
        Depth.One to false -> listOf(s(1, 3, tint, 0.2f), s(1, 2, tint, 0.14f))
        Depth.Three to true -> listOf(s(4, 10, Color.Black, 0.5f))
        else -> listOf(s(3, 5, tint, 0.26f), s(6, 10, tint, 0.18f))
    }
    return shadows.fold(this) { m, shadow -> m.dropShadow(shape, shadow) }
}

/** True when "Remove animations" is on in Android's settings: our animations become a short fade, the visualizer stands still. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
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
