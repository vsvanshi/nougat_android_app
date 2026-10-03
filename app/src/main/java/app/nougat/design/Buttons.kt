package app.nougat.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nougat.R

/** The design's pressed state: a 12% overlay (ink on paper, onHeader on the header). No ripple. */
@Composable
fun Modifier.pressable(
    shape: Shape,
    onClick: () -> Unit,
    role: Role = Role.Button,
    overlay: Color = LocalColors.current.ink,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return clip(shape)
        .clickable(interaction, indication = null, role = role, onClick = onClick)
        .background(overlay.copy(alpha = if (pressed) 0.12f else 0f))
}

/** Capsule button in the accent fill. 20 dp corners, so a label that wraps at the largest sizes stays rounded (D59). */
@Composable
fun FilledButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(Metrics.buttonHeight / 2)
    Box(modifier.defaultMinSize(minHeight = Metrics.touchTarget), contentAlignment = Alignment.Center) {
        Box(
            Modifier.depth(Depth.One, shape).background(accent.fill, shape).pressable(shape, onClick)
                .defaultMinSize(minHeight = Metrics.buttonHeight).padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text.uppercase(), style = Type.button, color = accent.onAccent, textAlign = TextAlign.Center)
        }
    }
}

/** Accent-coloured label with no fill. */
@Composable
fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.defaultMinSize(minHeight = Metrics.touchTarget), contentAlignment = Alignment.Center) {
        Box(
            Modifier.pressable(RoundedCornerShape(50), onClick).defaultMinSize(minHeight = Metrics.buttonHeight)
                .padding(horizontal = Metrics.margin),
            contentAlignment = Alignment.Center,
        ) {
            Text(text.uppercase(), style = Type.button, color = LocalAccent.current.text)
        }
    }
}

/** The round accent button: 56 dp on a header, 64 dp on Now playing. */
@Composable
fun PlayButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_play_arrow,
    size: Dp = Metrics.headerPlayButton,
) {
    val accent = LocalAccent.current
    Box(
        modifier.size(size).depth(Depth.Three, CircleShape).background(accent.accent, CircleShape)
            .pressable(CircleShape, onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = accent.onAccent, modifier = Modifier.size(size * 0.5f))
    }
}

/** Capsule chip, for equalizer presets. */
@Composable
fun Chip(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val colors = LocalColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier.defaultMinSize(minHeight = Metrics.touchTarget)
            .selectable(selected, onClick = onClick, role = Role.Tab, indication = null, interactionSource = null),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.background(if (selected) accent.fill else colors.fill, shape)
                .defaultMinSize(minHeight = Metrics.chipHeight).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title, style = if (selected) Type.bodyStrong else Type.body,
                color = if (selected) accent.onAccent else colors.ink,
            )
        }
    }
}

/** Off: an ink2 icon. On: an onAccent icon in a 40 dp accentFill circle. Shuffle and repeat on Now playing. */
@Composable
fun ToggleIcon(@DrawableRes icon: Int, label: String, on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Box(
        modifier.size(Metrics.touchTarget).pressable(CircleShape, { onToggle(!on) }, role = Role.Switch)
            .semantics { contentDescription = label; toggleableState = ToggleableState(on) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(Metrics.toggleCircle).background(if (on) accent.fill else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = if (on) accent.onAccent else LocalColors.current.ink2)
        }
    }
}

/** The Material switch tinted switchOn; its knob is always light. */
@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalColors.current
    Switch(
        checked, onCheckedChange, modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = LocalAccent.current.switchOn, checkedThumbColor = LightColors.paper,
            uncheckedTrackColor = colors.fill, uncheckedThumbColor = colors.ink3, uncheckedBorderColor = colors.ink3,
        ),
    )
}

/** An icon button for a top app bar; it takes the bar's icon colour. */
@Composable
fun BarIcon(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(Metrics.touchTarget).pressable(CircleShape, onClick, overlay = LocalContentColor.current)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null)
    }
}
