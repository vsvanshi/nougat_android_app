package app.nougat.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.nougat.R

/**
 * Shows snackbars from `state` in Nougat's look. Material's host gives the 4 s timeout
 * (longer when TalkBack needs time to reach the action) and announces each message.
 * Show one with `state.showSnackbar("Added to Sunday Slow", actionLabel = "Undo")`.
 */
@Composable
fun NoticeHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier.padding(horizontal = Metrics.margin, vertical = Metrics.grid)) { data ->
        Snackbar(data.visuals.message, data.visuals.actionLabel, onAction = data::performAction)
    }
}

/** The snackbar's look: inverse fill, 12 dp corners, depth 3. */
@Composable
fun Snackbar(message: String, actionLabel: String? = null, modifier: Modifier = Modifier, onAction: () -> Unit = {}) {
    val colors = LocalColors.current
    val shape = RoundedCornerShape(Radius.card)
    Row(
        modifier.depth(Depth.Three, shape).background(colors.inverse, shape)
            .defaultMinSize(minHeight = Metrics.touchTarget).padding(start = Metrics.margin, end = Metrics.grid),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(message, style = Type.body, color = colors.onInverse, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
        if (actionLabel != null) {
            Box(
                Modifier.defaultMinSize(Metrics.touchTarget, Metrics.touchTarget)
                    .pressable(RoundedCornerShape(Radius.thumbnail), onAction, overlay = colors.onInverse).padding(horizontal = Metrics.grid),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel.uppercase(), style = Type.button, color = LocalAccent.current.onInverse)
            }
        }
    }
}

/** Shown when a list has nothing in it, with the one action that fixes that. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    actionTitle: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_library_music,
) {
    val colors = LocalColors.current
    Column(modifier.padding(Metrics.margin), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.ink3, modifier = Modifier.size(48.dp))
        Text(title, style = Type.title, color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Metrics.grid))
        Text(message, style = Type.body, color = colors.ink2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        FilledButton(actionTitle, onAction, Modifier.padding(top = 12.dp))
    }
}
