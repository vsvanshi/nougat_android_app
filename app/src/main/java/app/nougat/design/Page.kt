package app.nougat.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nougat.R

/** The round button on a page header's bottom edge. `label` is what TalkBack reads, such as "Play folder". */
class HeaderAction(val label: String, @DrawableRes val icon: Int = R.drawable.ic_play_arrow, val onClick: () -> Unit)

/**
 * A scrolling page that opens with the blue-grey header (iPhone DESIGN.md sections 7 and 9).
 * The header is the first list item and reaches up behind the status bar. The top app bar over it is
 * the header colour, so the large title slides under it and never behind the clock. Once the dark
 * block has gone up under the bar, the bar turns `paper`, its title fades in and the status-bar
 * icons follow the theme again.
 * Below the dark block a paper strip holds the first subheader and the lower half of the button (D24).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: HeaderAction? = null,
    firstSubheader: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    barActions: @Composable RowScope.() -> Unit = {},
    bottomPadding: PaddingValues = PaddingValues(),
    titleModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val colors = LocalColors.current
    val list = rememberLazyListState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }
    val barBottom = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + BarHeight).roundToPx() }
    // The dark block's bottom edge has reached the bottom of the bar.
    val solid by remember(barBottom) {
        derivedStateOf {
            headerHeight > 0 && (list.firstVisibleItemIndex > 0 || headerHeight - list.firstVisibleItemScrollOffset <= barBottom)
        }
    }
    val fade = tween<Float>(Motion.standard, easing = Motion.easing)
    val titleAlpha by animateFloatAsState(if (solid) 1f else 0f, fade)
    val barColor by animateColorAsState(if (solid) colors.paper else colors.header, tween(Motion.standard, easing = Motion.easing))
    val chrome = if (solid) colors.ink else colors.onHeader

    val overHeader = LocalOverHeader.current
    SideEffect { overHeader.value = !solid }

    val strip = when {
        firstSubheader != null -> Metrics.subheader
        action != null -> Metrics.headerPlayButton / 2
        else -> 0.dp
    }

    Box(modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = bottomPadding) {
            item(key = "header") {
                Box {
                    Column {
                        PageHeader(title, subtitle, Modifier.onSizeChanged { headerHeight = it.height }, titleModifier)
                        Box(Modifier.fillMaxWidth().height(strip)) {
                            if (firstSubheader != null) Subheader(firstSubheader)
                        }
                    }
                    if (action != null) {
                        val y = with(density) { headerHeight.toDp() } - Metrics.headerPlayButton / 2
                        PlayButton(
                            action.label, action.onClick, icon = action.icon,
                            modifier = Modifier.align(Alignment.TopEnd).offset(y = y).padding(end = Metrics.margin),
                        )
                    }
                }
            }
            content()
        }
        TopAppBar(
            title = { Text(title, style = Type.barTitle, modifier = Modifier.graphicsLayer { alpha = titleAlpha }) },
            navigationIcon = navigationIcon,
            actions = barActions,
            expandedHeight = BarHeight,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = barColor, scrolledContainerColor = barColor,
                titleContentColor = colors.ink, navigationIconContentColor = chrome, actionIconContentColor = chrome,
            ),
        )
    }
}

/**
 * Whether the screen in front has the header colour behind the status bar, so its icons must be light.
 * Pages write it; the app shell applies it to the window.
 */
val LocalOverHeader = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(true) }

/** Material's small top app bar height. */
val BarHeight = 64.dp

/** The dark block: large title and subtitle, starting under the status bar and the top bar. */
@Composable
fun PageHeader(title: String, subtitle: String?, modifier: Modifier = Modifier, titleModifier: Modifier = Modifier) {
    val colors = LocalColors.current
    Column(
        modifier.fillMaxWidth().background(colors.header)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(top = BarHeight + Metrics.grid, bottom = Metrics.headerBottomPadding)
            .padding(horizontal = Metrics.margin)
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(title, titleModifier, style = Type.largeTitle, color = colors.onHeader)
        if (subtitle != null) Text(subtitle, style = Type.rowTitle, color = colors.onHeader2)
    }
}
