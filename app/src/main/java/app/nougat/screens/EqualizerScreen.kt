package app.nougat.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.Chip
import app.nougat.design.FilledButton
import app.nougat.design.LocalColors
import app.nougat.design.Metrics
import app.nougat.design.Slider
import app.nougat.design.Subheader
import app.nougat.design.TextButton
import app.nougat.design.Toggle
import app.nougat.design.Type
import app.nougat.equalizer.EqualizerSettings
import app.nougat.equalizer.EqualizerState
import kotlinx.coroutines.launch

val LocalEqualizer = staticCompositionLocalOf<EqualizerSettings> { error("No equalizer") }

private val bandNames = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
private val spokenBandNames = listOf("60 hertz", "230 hertz", "910 hertz", "3.6 kilohertz", "14 kilohertz")
private val sliderHeight = 240.dp

/** The scale and band names sit in narrow columns, so they stop growing a little above normal size (iPhone D59). */
@Composable
private fun cappedCaption(): TextStyle {
    val scale = LocalDensity.current.fontScale
    return Type.caption.copy(fontSize = Type.caption.fontSize * (minOf(scale, 1.3f) / scale))
}

private fun decibels(value: Double, spoken: Boolean): String {
    val number = if (value == 0.0) "0" else "%+.1f".format(value).removeSuffix(".0")
    return if (spoken) "$number decibels" else "$number dB"
}

/** The equalizer, opened from Now playing: on and off, presets, five bands and a preamp. */
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val colors = LocalColors.current
    val equalizer = LocalEqualizer.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val state = equalizer.state
    var naming by remember { mutableStateOf(false) }

    fun setGain(gain: Double, band: Int) {
        val before = equalizer.state.gains[band]
        equalizer.update(persist = false) { it.setGain(gain, band) }
        val after = equalizer.state.gains[band]
        // A tick when a band reaches or crosses 0 dB, so flat can be found by feel.
        if (before != after && (after == 0.0 || (before < 0) != (after < 0))) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(Modifier.statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(R.drawable.ic_arrow_back, "Back", onBack)
                Text("Equalizer", style = Type.barTitle, color = colors.ink, modifier = Modifier.padding(start = Metrics.margin))
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            // One switch for TalkBack: the label, the state and the toggle together.
            Row(
                Modifier.fillMaxWidth().toggleable(state.isOn, role = Role.Switch) { on -> equalizer.update { it.copy(isOn = on) } }
                    .padding(horizontal = Metrics.margin).height(Metrics.rowTwoLine),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Equalizer", style = Type.rowTitle, color = colors.ink)
                    Text(if (state.isOn) "On, ${state.presetName}" else "Off", style = Type.body, color = colors.ink2)
                }
                Toggle(state.isOn, null)
            }
            Box {
                // Off: dimmed, and out of TalkBack's reach, like the iPhone's disabled controls.
                Column(Modifier.alpha(if (state.isOn) 1f else 0.5f).then(if (state.isOn) Modifier else Modifier.clearAndSetSemantics {})) {
                    Presets(state, equalizer)
                    Bands(state, ::setGain) { equalizer.persist() }
                    Subheader("Preamp", Modifier.padding(top = Metrics.grid))
                    Slider(
                        state.preamp.toFloat(), { v -> equalizer.update(persist = false) { it.setPreamp(v.toDouble()) } },
                        "Preamp", decibels(state.preamp, spoken = true), Modifier.padding(horizontal = Metrics.margin),
                        range = EqualizerState.preampRange.start.toFloat()..EqualizerState.preampRange.endInclusive.toFloat(), origin = 0f,
                        onEditingChanged = { if (!it) equalizer.persist() },
                    )
                    // Side by side, or stacked when the text is too large for one row (D59).
                    FlowRow(Modifier.fillMaxWidth().padding(Metrics.margin), horizontalArrangement = Arrangement.spacedBy(Metrics.grid, Alignment.End)) {
                        TextButton("Reset", { equalizer.update { it.reset() } })
                        FilledButton("Save preset", { naming = true })
                    }
                }
                // Off: the controls are shown dimmed and do not respond.
                if (!state.isOn) Box(
                    Modifier.matchParentSize().clearAndSetSemantics {}.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                    },
                )
            }
        }
    }
    if (naming) NameDialog("Save preset", "Save", onDismiss = { naming = false }) { name ->
        var saved = false
        equalizer.update { s -> s.save(name)?.also { saved = true } ?: s }
        if (!saved) scope.launch { notices.showSnackbar("Choose a different name") }
    }
}

@Composable
private fun Presets(state: EqualizerState, equalizer: EqualizerSettings) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Metrics.margin), horizontalArrangement = Arrangement.spacedBy(Metrics.grid)) {
        for (preset in state.presets) {
            var menu by remember { mutableStateOf(false) }
            Box {
                Chip(
                    preset.name, state.presetName == preset.name, { equalizer.update { it.select(preset) } },
                    onLongClick = if (preset in state.saved) ({ menu = true }) else null,
                )
                DropdownMenu(menu, { menu = false }, containerColor = LocalColors.current.surface) {
                    DropdownMenuItem(
                        text = { Text("Delete preset", style = Type.rowTitle, color = LocalColors.current.ink) },
                        onClick = { menu = false; equalizer.update { it.delete(preset) } },
                    )
                }
            }
        }
        if (state.presetName == EqualizerState.CUSTOM) Chip(EqualizerState.CUSTOM, true, {})
    }
}

/** Five vertical bands that fill from the 0 dB line, with the scale on the left. */
@Composable
private fun Bands(state: EqualizerState, setGain: (Double, Int) -> Unit, done: () -> Unit) {
    val colors = LocalColors.current
    val range = EqualizerState.gainRange.start.toFloat()..EqualizerState.gainRange.endInclusive.toFloat()
    Box(Modifier.padding(horizontal = Metrics.grid).padding(top = 12.dp)) {
        // The 0 dB line the bands fill from.
        Box(Modifier.padding(start = 32.dp, top = sliderHeight / 2).fillMaxWidth().height(1.dp).background(colors.hairline))
        Row {
            Column(Modifier.width(32.dp).height(sliderHeight).clearAndSetSemantics {}, verticalArrangement = Arrangement.SpaceBetween) {
                for (mark in listOf("+15", "0", "-15")) Text(mark, style = cappedCaption(), color = colors.ink2, maxLines = 1)
            }
            for (band in state.gains.indices) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Slider(
                        state.gains[band].toFloat(), { setGain(it.toDouble(), band) }, spokenBandNames[band],
                        decibels(state.gains[band], spoken = true), Modifier.height(sliderHeight), range = range,
                        vertical = true, origin = 0f, onEditingChanged = { if (!it) done() },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(bandNames[band], style = cappedCaption(), color = colors.ink2, maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
    }
}

