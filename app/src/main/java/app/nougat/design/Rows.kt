package app.nougat.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.nougat.R

/** One entry of a row's menu. The same list fills the `more_vert` menu, the long-press menu and TalkBack's actions. */
class RowAction(val label: String, val onClick: () -> Unit)

/**
 * A song, folder or playlist row. Tap runs `onTap`; long press and `more_vert` open the same menu.
 * Rows grow when text scales; at very large font sizes the text wraps instead of truncating.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    highlighted: Boolean = false,
    /** False dims the row and ignores taps (its menu still opens), for a song Android cannot play. */
    enabled: Boolean = true,
    spokenState: String? = null,
    actions: List<RowAction> = emptyList(),
    onTap: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    /** False where a long press starts a drag instead; `more_vert` still opens the menu. */
    menuOnLongPress: Boolean = true,
) {
    val colors = LocalColors.current
    val accent = LocalAccent.current
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val large = LocalDensity.current.fontScale >= 1.5f

    Box(modifier.alpha(if (enabled) 1f else 0.5f)) {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(
                    interaction, indication = null, enabled = (enabled && onTap != null) || actions.isNotEmpty(),
                    onLongClick = if (actions.isEmpty() || !menuOnLongPress) null else ({ menu = true }),
                    onClick = { if (enabled) onTap?.invoke() },
                )
                .background(colors.ink.copy(alpha = if (pressed) 0.12f else 0f))
                .defaultMinSize(minHeight = if (subtitle == null) Metrics.rowOneLine else Metrics.rowTwoLine)
                .padding(start = Metrics.margin, end = if (actions.isEmpty()) Metrics.margin else Metrics.grid / 2)
                .padding(vertical = if (large) Metrics.grid else Metrics.grid / 2)
                .semantics {
                    spokenState?.let { stateDescription = it }
                    customActions = actions.map { CustomAccessibilityAction(it.label) { it.onClick(); true } }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Box(Modifier.size(Metrics.margin))
            }
            Column(Modifier.weight(1f)) {
                val lines = if (large) 3 else 1
                Text(title, titleModifier, style = Type.rowTitle, color = if (highlighted) accent.text else colors.ink, maxLines = lines, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = Type.body, color = colors.ink2, maxLines = lines, overflow = TextOverflow.Ellipsis)
            }
            if (detail != null) Text(detail, style = Type.caption, color = colors.ink2, modifier = Modifier.padding(start = Metrics.grid))
            if (actions.isNotEmpty()) MoreButton { menu = true }
        }
        RowMenu(actions, expanded = menu, onDismiss = { menu = false })
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    Box(
        Modifier.size(Metrics.touchTarget).pressable(CircleShape, onClick).semantics { contentDescription = "More" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null, tint = LocalColors.current.ink2)
    }
}

@Composable
private fun BoxScope.RowMenu(actions: List<RowAction>, expanded: Boolean, onDismiss: () -> Unit) {
    Box(Modifier.align(Alignment.TopEnd)) {
        DropdownMenu(expanded, onDismiss, containerColor = LocalColors.current.surface) {
            for (action in actions) {
                DropdownMenuItem(
                    text = { Text(action.label, style = Type.rowTitle, color = LocalColors.current.ink) },
                    onClick = { onDismiss(); action.onClick() },
                )
            }
        }
    }
}

/** Square artwork thumbnail; a letter tile while there is none. */
@Composable
fun LetterTile(title: String, modifier: Modifier = Modifier) {
    val colors = LocalColors.current
    val size = Metrics.thumbnail
    Box(
        modifier.size(size).clip(RoundedCornerShape(Radius.thumbnail)).background(colors.fill).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // Fixed size: the tile does not grow with the font size.
        val letter = with(LocalDensity.current) { (size * 0.45f).toSp() }
        Text(title.take(1).uppercase(), color = colors.ink2, fontSize = letter, fontWeight = FontWeight.Medium)
    }
}

/** The circle with a folder icon that leads a folder row. */
@Composable
fun FolderAvatar(modifier: Modifier = Modifier) {
    val colors = LocalColors.current
    Box(
        modifier.size(Metrics.thumbnail).background(colors.avatar, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = colors.onAvatar)
    }
}

/** Section label inside a list. */
@Composable
fun Subheader(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().defaultMinSize(minHeight = Metrics.subheader).padding(horizontal = Metrics.margin), contentAlignment = Alignment.CenterStart) {
        Text(title, style = Type.bodyStrong, color = LocalColors.current.ink2, modifier = Modifier.semantics { heading() })
    }
}
