package app.nougat.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * The thin slider of section 7. Horizontal for seeking and preamp, vertical for equalizer bands.
 * `origin` is where the fill starts: null fills from the minimum; the equalizer passes 0 so bands
 * fill from the centre line. TalkBack adjusts it in twentieths of the range.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    vertical: Boolean = false,
    origin: Float? = null,
    onEditingChanged: (Boolean) -> Unit = {},
) {
    val accent = LocalAccent.current
    val track = LocalColors.current.hairline
    val change by rememberUpdatedState(onValueChange)
    val editing by rememberUpdatedState(onEditingChanged)
    val shaped = if (vertical) modifier.width(Metrics.touchTarget).fillMaxHeight() else modifier.height(Metrics.touchTarget).fillMaxWidth()

    Canvas(
        shaped
            .pointerInput(range, vertical) {
                fun at(p: Offset) = if (vertical) sliderValue(size.height - p.y, size.height.toFloat(), range)
                    else sliderValue(p.x, size.width.toFloat(), range)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    editing(true)
                    change(at(down.position))
                    drag(down.id) { change(at(it.position)); it.consume() }
                    editing(false)
                }
            }
            .semantics {
                contentDescription = label
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value, range)
                setProgress { change(it.coerceIn(range)); true }
            },
    ) {
        val length = if (vertical) size.height else size.width
        fun point(fraction: Float) = if (vertical) Offset(center.x, length * (1 - fraction)) else Offset(length * fraction, center.y)
        val trackWidth = 2.dp.toPx()
        val at = sliderFraction(value, range)
        val from = sliderFraction(origin ?: range.start, range)
        drawLine(track, point(0f), point(1f), trackWidth, StrokeCap.Round)
        drawLine(accent.text, point(from), point(at), trackWidth, StrokeCap.Round)
        drawCircle(accent.text, radius = 7.dp.toPx(), center = point(at))
    }
}

/** Position of a value along the track, 0 to 1. */
fun sliderFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    if (range.endInclusive <= range.start) return 0f
    return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

/** The value under a touch `position` from the start of a track `length` long. */
fun sliderValue(position: Float, length: Float, range: ClosedFloatingPointRange<Float>): Float {
    if (length <= 0f) return range.start
    return range.start + (position / length).coerceIn(0f, 1f) * (range.endInclusive - range.start)
}
