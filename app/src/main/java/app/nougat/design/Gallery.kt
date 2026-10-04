package app.nougat.design

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.screens.AccentPicker
import app.nougat.screens.LocalNotices
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Debug catalogue of the design system, the Android counterpart of the iPhone Gallery.swift.
 * It is itself a header page. Every swatch shows its light and dark value side by side.
 */
@Composable
fun Gallery(accent: Accent, onAccent: (Accent) -> Unit, onBack: () -> Unit) {
    val colors = LocalColors.current
    val context = LocalContext.current
    val spec = accent.spec(context)
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    fun notify(message: String, action: String? = null) = scope.launch { notices.showSnackbar(message, action) }

    Page(
            title = "Design gallery",
            subtitle = "Every token and component",
            action = HeaderAction("Show a snackbar") { notify("Added to Sunday Slow", "Undo") },
            firstSubheader = "Accent",
            navigationIcon = { BarIcon(R.drawable.ic_arrow_back, "Back", onBack) },
        ) {
            item { AccentPicker(accent, onAccent, context) }
            item { Subheader("Components") }
            item { Components(::notify) }
            item { Subheader("Type") }
            items(styles) { (name, style) ->
                Text(
                    name, style = style, color = if (style == Type.button) LocalAccent.current.text else colors.ink,
                    modifier = Modifier.padding(horizontal = Metrics.margin, vertical = 4.dp),
                )
            }
            item { Subheader("Icons") }
            item {
                FlowRow(Modifier.padding(horizontal = Metrics.margin), horizontalArrangement = Arrangement.spacedBy(Metrics.margin)) {
                    for (icon in icons) {
                        Icon(painterResource(icon), contentDescription = null, tint = colors.ink, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
            item { Subheader("Accent colours: ${accent.name}") }
            items(spec.colors(false).named.zip(spec.colors(true).named)) { (light, dark) ->
                SwatchRow(light.first, light.second, dark.second)
            }
            item { Subheader("Fixed colours") }
            items(LightColors.named.zip(DarkColors.named)) { (light, dark) ->
                SwatchRow(light.first, light.second, dark.second)
            }
        }
}

@Composable
private fun Components(notify: (String, String?) -> Unit) {
    var chip by remember { mutableStateOf("Bass boost") }
    var eq by remember { mutableStateOf(true) }
    var shuffle by remember { mutableStateOf(true) }
    var repeat by remember { mutableStateOf(false) }
    var seek by remember { mutableFloatStateOf(0.45f) }
    var band by remember { mutableFloatStateOf(6f) }
    val colors = LocalColors.current

    Column {
        Row(Modifier.padding(horizontal = Metrics.margin), verticalAlignment = Alignment.CenterVertically) {
            FilledButton("Play folder", { notify("Play folder", null) })
            TextButton("Add songs", { notify("Add songs", null) })
            Box(Modifier.weight(1f))
            PlayButton("Play", { notify("Play", null) })
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Metrics.margin),
            horizontalArrangement = Arrangement.spacedBy(Metrics.grid),
        ) {
            for (name in listOf("Flat", "Bass boost", "Rock", "Vocal", "Custom")) Chip(name, chip == name, { chip = name })
        }
        Row(Modifier.padding(horizontal = Metrics.margin).height(Metrics.rowOneLine), verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", style = Type.rowTitle, color = colors.ink, modifier = Modifier.weight(1f))
            Toggle(eq, { eq = it })
        }
        Row(Modifier.padding(horizontal = Metrics.margin), horizontalArrangement = Arrangement.spacedBy(Metrics.margin)) {
            ToggleIcon(R.drawable.ic_shuffle, "Shuffle", shuffle, { shuffle = it })
            ToggleIcon(if (repeat) R.drawable.ic_repeat_one else R.drawable.ic_repeat, "Repeat", repeat, { repeat = it })
        }
        Row(Modifier.padding(horizontal = Metrics.margin), verticalAlignment = Alignment.CenterVertically) {
            Slider(seek, { seek = it }, "Position", "${(seek * 100).roundToInt()} percent", Modifier.weight(1f))
            Box(Modifier.width(24.dp))
            Slider(band, { band = it }, "60 hertz", "${band.roundToInt()} decibels", Modifier.height(120.dp), range = -15f..15f, vertical = true, origin = 0f)
        }
        ListRow(
            "Kite Season", subtitle = "Anouk Verma", detail = "2:58",
            actions = listOf(RowAction("Add to playlist") { notify("Add to playlist", null) }, RowAction("Hide") { notify("Hide", null) }),
            onTap = { notify("Kite Season", null) },
            leading = { LetterTile("Kite Season") },
        )
        ListRow(
            "Road Trip 2016", subtitle = "64 songs",
            actions = listOf(RowAction("Play") { notify("Play", null) }),
            onTap = { notify("Road Trip 2016", null) },
            leading = { FolderAvatar() },
        )
        Snackbar("Added to Sunday Slow", "Undo", Modifier.padding(Metrics.margin))
        EmptyState("No music here yet", "Copy music into the Music folder to start.", "Refresh", { notify("Refresh", null) }, Modifier.fillMaxWidth())
    }
}

private val styles = listOf(
    "Large title 34" to Type.largeTitle, "Title 22" to Type.title, "Bar title 17" to Type.barTitle,
    "Row title 16" to Type.rowTitle, "Body 14" to Type.body, "Body strong 14" to Type.bodyStrong,
    "Caption 12  3:42 1:11" to Type.caption, "BUTTON 14" to Type.button,
)

private val icons = listOf(
    R.drawable.ic_more_vert, R.drawable.ic_folder, R.drawable.ic_play_arrow, R.drawable.ic_pause,
    R.drawable.ic_skip_next, R.drawable.ic_skip_previous, R.drawable.ic_shuffle, R.drawable.ic_repeat,
    R.drawable.ic_repeat_one, R.drawable.ic_playlist_add, R.drawable.ic_equalizer, R.drawable.ic_add,
    R.drawable.ic_check, R.drawable.ic_library_music,
)

@Composable
private fun SwatchRow(name: String, light: Color, dark: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Metrics.margin, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = LocalColors.current.ink, style = Type.body, modifier = Modifier.weight(1f))
        Swatch(light, "light")
        Swatch(dark, "dark")
    }
}

@Composable
private fun Swatch(color: Color, label: String) {
    val colors = LocalColors.current
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 80.dp, height = 32.dp).background(color, RoundedCornerShape(4.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(4.dp)))
        Text("$label #%06X".format(color.toArgb() and 0xFFFFFF), color = colors.ink2, style = Type.caption)
    }
}
