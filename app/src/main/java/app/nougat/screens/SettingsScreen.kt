package app.nougat.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nougat.R
import app.nougat.design.Accent
import app.nougat.design.BarHeight
import app.nougat.design.BarIcon
import app.nougat.design.ListRow
import app.nougat.design.LocalColors
import app.nougat.design.LocalOverHeader
import app.nougat.design.Metrics
import app.nougat.design.Page
import app.nougat.design.Subheader
import app.nougat.design.Type
import app.nougat.design.pressable
import app.nougat.library.count

/** Open-source work inside the app. They all use the Apache License 2.0, whose text is `res/raw/apache_2_0.txt`. */
val licences = listOf(
    "Material Icons" to "Apache License 2.0",
    "Android Jetpack (AndroidX, Compose, Media3)" to "Apache License 2.0",
    "Guava" to "Apache License 2.0",
    "Kotlin and kotlinx.coroutines" to "Apache License 2.0",
)

/** Settings, from the Folders menu: the accent, hidden folders and songs, the version and the licences. */
@Composable
fun SettingsScreen(accent: Accent, onAccent: (Accent) -> Unit, push: (Screen) -> Unit, back: () -> Unit) {
    val library = LocalLibrary.current
    val context = LocalContext.current
    val version = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION") "${info.versionName} (${info.versionCode})"
    }
    Page(
        title = "Settings",
        firstSubheader = "Accent colour",
        navigationIcon = { BarIcon(R.drawable.ic_arrow_back, "Back", back) },
    ) {
        item { AccentPicker(accent, onAccent, context) }
        item { Subheader("Library") }
        item {
            ListRow(
                "Hidden folders and songs",
                subtitle = if (library.hidden.isEmpty()) "Nothing hidden" else "${count(library.hidden.size, "item")}, tap to show again",
                onTap = { push(Screen.Hidden) },
            )
        }
        item { Subheader("About") }
        item { ListRow("Version", subtitle = version) }
        item { Subheader("Open-source licences") }
        for ((name, terms) in licences) item { ListRow(name, subtitle = terms, onTap = { push(Screen.Licence(name)) }) }
    }
}

/** The accents as swatches; the chosen one shows a check. "System" appears on Android 12 and later (A14). */
@Composable
fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit, context: Context) {
    Row(Modifier.padding(horizontal = Metrics.margin - 4.dp).defaultMinSize(minHeight = Metrics.rowOneLine), verticalAlignment = Alignment.CenterVertically) {
        for (option in Accent.available) {
            val c = option.spec(context).colors(dark = false)
            Box(
                Modifier.size(Metrics.touchTarget).pressable(CircleShape, { onSelect(option) }, role = Role.RadioButton)
                    .semantics { contentDescription = option.name; this.selected = option == selected },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(40.dp).background(c.accent, CircleShape), contentAlignment = Alignment.Center) {
                    if (option == selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.onAccent)
                }
            }
        }
    }
}

/** One licence's full text. */
@Composable
fun LicenceScreen(name: String, back: () -> Unit) {
    val colors = LocalColors.current
    val context = LocalContext.current
    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = false }
    val text = remember { context.resources.openRawResource(R.raw.apache_2_0).bufferedReader().use { it.readText() } }
    Column(Modifier.fillMaxSize().background(colors.paper)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            Row(Modifier.statusBarsPadding().height(BarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BarIcon(R.drawable.ic_arrow_back, "Back", back)
                Text(name, style = Type.barTitle, color = colors.ink, maxLines = 1, modifier = Modifier.padding(start = Metrics.margin))
            }
        }
        Text(
            text, style = Type.body, color = colors.ink,
            modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(Metrics.margin),
        )
    }
}

