package app.nougat.design

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import app.nougat.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Debug catalogue of the design system, the Android counterpart of the iPhone Gallery.swift.
 * Every swatch shows its light and dark value side by side, so one screen covers both.
 */
@Composable
fun Gallery(accent: Accent, onAccent: (Accent) -> Unit) {
    val colors = LocalColors.current
    val context = LocalContext.current
    val spec = accent.spec(context)
    LazyColumn(
        Modifier.fillMaxSize().background(colors.paper),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        item {
            Text(
                "Design gallery", color = colors.onHeader, style = Type.title,
                modifier = Modifier.fillMaxWidth().background(colors.header).statusBarsPadding().padding(16.dp),
            )
        }
        item { Subheader("Accent") }
        item { AccentPicker(accent, onAccent, context) }
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
private fun Subheader(title: String) {
    Text(
        title, color = LocalAccent.current.text, style = Type.bodyStrong,
        modifier = Modifier.padding(start = Metrics.margin, end = Metrics.margin, top = 24.dp, bottom = 8.dp),
    )
}

/** The accents as swatches; the chosen one shows a check. */
@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit, context: Context) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in Accent.available) {
            val c = option.spec(context).colors(dark = false)
            Box(
                Modifier.size(40.dp).background(c.accent, CircleShape).clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                if (option == selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.onAccent)
            }
        }
    }
}

@Composable
private fun SwatchRow(name: String, light: Color, dark: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
