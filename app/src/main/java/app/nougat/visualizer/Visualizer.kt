package app.nougat.visualizer

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.nougat.design.LightColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The looks the visualizer can take. Tapping the cover on Now playing steps through them (D52). */
enum class VisualizerStyle(val title: String) {
    Spectrum("Spectrum"), Led("LED meter"), Oscilloscope("Oscilloscope"), Ring("Ring"), Cassette("Cassette"), Record("Record");

    /** Over a cover there is room only for a low strip, where the round and drawn looks fall back to bars. */
    val strip get() = if (this == Led || this == Oscilloscope) this else Spectrum

    val next get() = entries[(ordinal + 1) % entries.size]

    /** The next look that fits a strip. */
    val nextStrip get() = listOf(Spectrum, Led, Oscilloscope).let { it[(it.indexOf(strip) + 1) % it.size] }
}

/**
 * Moves with the music (iPhone DESIGN.md section 7, D50, D52). On a cover it is a low strip of bars,
 * an LED meter or an oscilloscope; filling the cover area it can also be a ring, a cassette or a record.
 * With animations removed in Android's settings, nothing moves.
 */
@Composable
fun Visualizer(
    spectrum: Spectrum,
    isPlaying: Boolean,
    style: VisualizerStyle,
    color: Color,
    capColor: Color,
    modifier: Modifier = Modifier,
    /** Fills a large area on the header colour, rather than a strip over a cover. */
    stage: Boolean = false,
    /** The song's title, for the cassette label, the record and the ring. */
    title: String = "",
    /** How far through the song, 0 to 1. Read on every frame, so it must be cheap. */
    progress: () -> Float = { 0f },
) {
    val context = LocalContext.current
    val still = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val motion = remember { Motion() }
    var frame by remember { mutableLongStateOf(0L) }
    // Keeps the clock running for a moment after a pause, so things can come to rest.
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) return@LaunchedEffect
        settling = true
        delay(1500)
        settling = false
    }
    LaunchedEffect(still, isPlaying || settling) {
        if (still || !(isPlaying || settling)) return@LaunchedEffect
        while (true) withFrameNanos { now ->
            motion.step(spectrum, isPlaying, progress(), now)
            frame = now
        }
    }
    val text = rememberTextMeasurer()
    Canvas(modifier.clearAndSetSemantics {}) {
        @Suppress("UNUSED_EXPRESSION") frame // redraw on every frame
        val shown = if (stage) style else style.strip
        when (shown) {
            VisualizerStyle.Spectrum -> drawBars(motion, color, capColor, stage)
            VisualizerStyle.Led -> drawLed(motion, color, capColor, stage)
            VisualizerStyle.Oscilloscope -> drawWave(motion, color, capColor, stage)
            VisualizerStyle.Ring -> drawRing(motion, color, capColor, title, text)
            VisualizerStyle.Cassette -> drawCassette(motion, color, title, text, progress())
            VisualizerStyle.Record -> drawRecord(motion, color, title, text, progress())
        }
    }
}

private val paper = LightColors.onHeader
private val dark = LightColors.header

private fun DrawScope.pt(v: Float) = v * density

/** Text centred on a point, at a fixed pixel size. */
private fun DrawScope.centredText(measurer: TextMeasurer, text: String, at: Offset, sizePx: Float, color: Color) {
    if (text.isEmpty()) return
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = (sizePx / fontScale / density).sp, fontWeight = FontWeight.Medium), maxLines = 1)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

// Spectrum: bars with caps that hang, then fall

private fun DrawScope.drawBars(m: Motion, color: Color, capColor: Color, stage: Boolean) {
    val count = m.bars.size
    val gap = pt(if (stage) 4f else 3f)
    val width = (size.width - gap * (count - 1)) / count
    val baseline = if (stage) size.height * 0.7f else size.height
    val cap = pt(3f)
    val tallest = baseline - cap * 2
    for (i in 0 until count) {
        val x = i * (width + gap)
        val height = max(pt(3f), m.bars[i] * tallest)
        drawRoundRect(color, Offset(x, baseline - height), Size(width, height), CornerRadius(pt(2f)))
        if (m.caps[i] > 0.03f) {
            val top = max(height, m.caps[i] * tallest) + cap * 2
            drawRect(capColor, Offset(x, baseline - top), Size(width, cap))
        }
        if (stage) drawRect(
            Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0f)), baseline, size.height),
            Offset(x, baseline + pt(2f)), Size(width, height * 0.4f),
        )
    }
}

// LED meter: columns of lamps, the top quarter lit in the cap colour

private fun DrawScope.drawLed(m: Motion, color: Color, capColor: Color, stage: Boolean) {
    val count = m.bars.size
    val gap = pt(if (stage) 4f else 3f)
    val width = (size.width - gap * (count - 1)) / count
    val lamp = pt(if (stage) 6f else 4f)
    val spacing = pt(if (stage) 3f else 2f)
    val rows = max(1, (size.height / (lamp + spacing)).toInt())
    val hotFrom = (rows * 0.75).toInt()
    for (i in 0 until count) {
        val x = i * (width + gap)
        val on = (m.bars[i] * rows).roundToInt()
        val capRow = (m.caps[i] * rows).roundToInt() - 1
        for (row in 0 until rows) {
            val top = size.height - (row + 1) * (lamp + spacing) + spacing
            val lit = row < on || (row == capRow && m.caps[i] > 0.03f)
            val fill = when {
                !lit -> color.copy(alpha = 0.12f)
                row >= hotFrom || row == capRow -> capColor
                else -> color
            }
            drawRect(fill, Offset(x, top), Size(width, lamp))
        }
    }
}

// Oscilloscope: the sound wave itself, with a glow

private fun DrawScope.drawWave(m: Motion, color: Color, capColor: Color, stage: Boolean) {
    val middle = size.height / 2
    val line = Path()
    m.wave.forEachIndexed { i, v ->
        val x = size.width * i / (m.wave.size - 1)
        val y = middle - (v * 1.6f).coerceIn(-1f, 1f) * middle * 0.9f
        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }
    if (stage) {
        drawLine(capColor.copy(alpha = 0.15f), Offset(0f, middle), Offset(size.width, middle), pt(1f))
        drawPath(line, color.copy(alpha = 0.25f), style = Stroke(pt(9f), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawPath(line, color, style = Stroke(pt(if (stage) 2.5f else 2f), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// Ring: bars around a circle that turns slowly, mirrored left and right

private fun DrawScope.drawRing(m: Motion, color: Color, capColor: Color, title: String, text: TextMeasurer) {
    val c = center
    val side = min(size.width, size.height)
    val radius = side * 0.25f
    val reach = side * 0.24f
    val spokes = m.bars.size * 2
    val bass = m.bars.take(4).sum() / 4
    drawCircle(color.copy(alpha = 0.18f), radius * (0.8f + bass * 0.12f), c)
    val width = (2 * PI * radius / spokes * 0.55).toFloat()
    for (spoke in 0 until spokes) {
        val bar = if (spoke < m.bars.size) spoke else spokes - 1 - spoke
        val angle = m.ringAngle + spoke.toDouble() / spokes * 2 * PI - PI / 2
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val length = max(pt(2f), m.bars[bar] * reach)
        drawLine(color, Offset(c.x + dx * radius, c.y + dy * radius), Offset(c.x + dx * (radius + length), c.y + dy * (radius + length)), width, StrokeCap.Round)
    }
    centredText(text, title.take(1).uppercase(), c, radius * 0.9f, capColor)
}

// Cassette: reels turn, and tape winds from left to right as the song plays

private fun DrawScope.drawCassette(m: Motion, color: Color, title: String, text: TextMeasurer, progress: Float) {
    val width = min(min(size.width, size.height * 1.5f), pt(360f))
    val height = width * 0.63f
    val body = Rect(Offset((size.width - width) / 2, (size.height - height) / 2), Size(width, height))
    drawRoundRect(paper.copy(alpha = 0.1f), body.topLeft, body.size, CornerRadius(width * 0.04f))
    drawRoundRect(paper.copy(alpha = 0.25f), body.topLeft, body.size, CornerRadius(width * 0.04f), style = Stroke(pt(1.5f)))
    val inset = pt(10f)
    for (corner in listOf(Offset(body.left + inset, body.top + inset), Offset(body.right - inset, body.top + inset), Offset(body.left + inset, body.bottom - inset), Offset(body.right - inset, body.bottom - inset))) {
        drawCircle(paper.copy(alpha = 0.3f), pt(3f), corner)
    }
    // The paper label, with a coloured band and the song written on it.
    val label = Rect(Offset(body.left + width * 0.06f, body.top + height * 0.08f), Size(width * 0.88f, height * 0.64f))
    drawRoundRect(paper, label.topLeft, label.size, CornerRadius(pt(6f)))
    val band = Rect(Offset(label.left, label.top + label.height * 0.1f), Size(label.width, label.height * 0.16f))
    drawRect(color, band.topLeft, band.size)
    centredText(text, "A", Offset(band.left + band.height * 0.7f, band.center.y), band.height * 0.8f, dark)
    val name = if (title.length > 30) title.take(29) + "…" else title
    centredText(text, name, Offset(label.center.x, band.bottom + label.height * 0.11f), max(pt(11f), label.height * 0.11f), dark)
    // The window, with the two reels.
    val window = Rect(Offset(label.center.x - label.width * 0.31f, label.top + label.height * 0.48f), Size(label.width * 0.62f, label.height * 0.42f))
    drawRoundRect(dark, window.topLeft, window.size, CornerRadius(window.height / 2))
    val hub = window.height * 0.22f
    val fullest = window.height * 0.46f
    val wound = progress.coerceIn(0f, 1f)
    val reels = listOf(
        Triple(Offset(window.left + window.width * 0.22f, window.center.y), 1 - wound, m.reelLeft),
        Triple(Offset(window.right - window.width * 0.22f, window.center.y), wound, m.reelRight),
    )
    for ((c, share, angle) in reels) {
        // The tape's area, not its radius, follows the share of the song on that reel.
        drawCircle(paper.copy(alpha = 0.22f), hub + (fullest - hub) * sqrt(share), c)
        drawCircle(paper, hub, c)
        translate(c.x, c.y) {
            rotateRad(angle.toFloat(), Offset.Zero) {
                for (tooth in 0 until 6) rotate(tooth * 60f, Offset.Zero) {
                    drawRect(dark, Offset(-hub * 0.12f, -hub * 0.75f), Size(hub * 0.24f, hub * 0.32f))
                }
                drawCircle(dark, hub * 0.35f, Offset.Zero)
            }
        }
    }
    // The bottom edge with its holes.
    val foot = Rect(Offset(body.center.x - width * 0.32f, body.bottom - height * 0.17f), Size(width * 0.64f, height * 0.17f))
    val trapezoid = Path().apply {
        moveTo(foot.left, foot.bottom)
        lineTo(foot.left + foot.height * 0.6f, foot.top)
        lineTo(foot.right - foot.height * 0.6f, foot.top)
        lineTo(foot.right, foot.bottom)
    }
    drawPath(trapezoid, paper.copy(alpha = 0.25f), style = Stroke(pt(1.5f)))
    for (hole in 0 until 4) drawCircle(paper.copy(alpha = 0.3f), pt(3f), Offset(foot.left + foot.width * (0.2f + 0.2f * hole), foot.center.y + pt(2f)))
}

// Record: a disc turning at 33⅓, the arm moving inward as the song plays

private fun DrawScope.drawRecord(m: Motion, color: Color, title: String, text: TextMeasurer, progress: Float) {
    val side = min(size.width, size.height)
    val radius = side * 0.47f
    val c = Offset(size.width / 2 - side * 0.08f, size.height / 2)
    drawCircle(Color.Black.copy(alpha = 0.6f), radius, c)
    var groove = radius * 0.38f
    while (groove <= radius * 0.97f) {
        drawCircle(paper.copy(alpha = 0.06f), groove, c, style = Stroke(pt(1f)))
        groove += pt(3f)
    }
    // Light falling on the grooves stays put while the disc turns.
    rotate(-35f, c) {
        drawCircle(
            Brush.sweepGradient(listOf(Color.Transparent, paper.copy(alpha = 0.12f), Color.Transparent, Color.Transparent, paper.copy(alpha = 0.12f), Color.Transparent, Color.Transparent), c),
            radius, c,
        )
    }
    val labelRadius = radius * 0.33f
    translate(c.x, c.y) {
        rotateRad(m.discAngle.toFloat(), Offset.Zero) {
            drawCircle(color, labelRadius, Offset.Zero)
            centredText(text, title.take(1).uppercase(), Offset(0f, -labelRadius * 0.5f), labelRadius * 0.7f, dark)
            centredText(text, "33⅓", Offset(0f, labelRadius * 0.55f), labelRadius * 0.22f, dark)
            drawCircle(dark, pt(3f), Offset.Zero)
        }
    }
    // The arm, from its pivot to a point that moves from the outer groove to the inner one.
    val pivot = Offset(c.x + radius * 1.05f, c.y - radius * 0.85f)
    val reach = radius * (0.92f - 0.5f * progress.coerceIn(0f, 1f))
    val tip = Offset(c.x + reach * cos(0.35f), c.y + reach * sin(0.35f))
    val arm = Path().apply {
        moveTo(pivot.x + (pivot.x - tip.x) * 0.12f, pivot.y + (pivot.y - tip.y) * 0.12f)
        lineTo(pivot.x, pivot.y)
        lineTo(tip.x, tip.y)
    }
    drawPath(arm, paper.copy(alpha = 0.9f), style = Stroke(pt(4f), cap = StrokeCap.Round, join = StrokeJoin.Round))
    translate(tip.x, tip.y) {
        rotateRad(atan2(tip.y - pivot.y, tip.x - pivot.x), Offset.Zero) {
            drawRoundRect(paper.copy(alpha = 0.9f), Offset(-pt(4f), -pt(6f)), Size(pt(12f), pt(12f)), CornerRadius(pt(2f)))
        }
    }
    drawCircle(paper.copy(alpha = 0.9f), pt(10f), pivot)
    drawCircle(dark, pt(4f), pivot)
}

/**
 * Everything that moves, advanced once a frame. Bars rise quickly and fall back slowly, with a cap
 * that hangs for a moment and then drops; turning things spin up and wind down like real ones.
 */
private class Motion {
    val bars = FloatArray(Spectrum.BARS)
    val caps = FloatArray(Spectrum.BARS)
    val wave = FloatArray(Spectrum.WAVE)
    var ringAngle = 0.0
    var discAngle = 0.0
    var reelLeft = 0.0
    var reelRight = 0.0
    private val hold = FloatArray(Spectrum.BARS)
    /** 0 when stopped, 1 at full speed. */
    private var spin = 0.0
    private var last = 0L

    fun step(spectrum: Spectrum, playing: Boolean, progress: Float, now: Long) {
        // After a pause in drawing, carry on as if one frame had passed.
        val elapsed = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceIn(0.0, 0.05)
        last = now
        val levels = spectrum.levels
        for (i in bars.indices) {
            val target = levels.getOrElse(i) { 0f }
            val speed = if (target > bars[i]) 32.0 else 7.0
            bars[i] += ((target - bars[i]) * (1 - exp(-elapsed * speed))).toFloat()
            when {
                bars[i] >= caps[i] -> { caps[i] = bars[i]; hold[i] = 0.35f }
                hold[i] > 0 -> hold[i] -= elapsed.toFloat()
                else -> caps[i] = max(bars[i], caps[i] - (elapsed * 0.7).toFloat())
            }
        }
        val samples = spectrum.wave
        for (i in wave.indices) wave[i] += ((samples.getOrElse(i) { 0f } - wave[i]) * (1 - exp(-elapsed * 25))).toFloat()

        spin += ((if (playing) 1.0 else 0.0) - spin) * (1 - exp(-elapsed * 2.5))
        ringAngle += elapsed * 0.15 * spin
        discAngle += elapsed * (100.0 / 3 / 60 * 2 * PI) * spin
        // Tape passes the head at one speed, so the emptier reel turns faster.
        val wound = progress.coerceIn(0f, 1f).toDouble()
        val hub = 0.45
        reelLeft += elapsed * 4 * spin / (hub + (1 - hub) * sqrt(1 - wound))
        reelRight += elapsed * 4 * spin / (hub + (1 - hub) * sqrt(wound))
    }
}
