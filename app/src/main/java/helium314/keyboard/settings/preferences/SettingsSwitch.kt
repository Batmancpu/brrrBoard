// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import helium314.keyboard.settings.SettingsVisuals
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.settings.getTransitionAnimationScale

/** Capsule switch, Android toggle semantics, 48dp touch target, RTL-aware travel. */
@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }) {
    val reduceMotion = getTransitionAnimationScale(LocalContext.current) <= 0f
    val pressed by interactionSource.collectIsPressedAsState()
    val palette = MaterialTheme.colorScheme
    val track by animateColorAsState(
        targetValue = (if (checked) palette.primary else palette.outline).let {
            if (enabled) it else it.copy(alpha = .42f)
        }, animationSpec = if (reduceMotion) snap() else tween(160), label = "switch-track")
    val thumbWidth by animateDpAsState(if (pressed && enabled) 29.dp else 25.dp,
        animationSpec = if (reduceMotion) snap() else tween(120), label = "switch-press")
    val position by animateDpAsState(if (checked) 47.dp - thumbWidth else 2.dp,
        animationSpec = if (reduceMotion) snap() else tween(160), label = "switch-slide")
    Box(modifier.size(width = 52.dp, height = SettingsVisuals.controlTarget).then(
        if (onCheckedChange == null) Modifier else Modifier.toggleable(
            value = checked, enabled = enabled, role = Role.Switch,
            interactionSource = interactionSource, indication = null, onValueChange = onCheckedChange)
    ), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 49.dp, height = 29.dp).clip(CircleShape).background(track)) {
            Box(Modifier.align(Alignment.CenterStart).offset(x = position)
                .size(width = thumbWidth, height = 25.dp)
                .shadow(if (enabled) 1.dp else 0.dp, CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 1f else .72f), CircleShape)
                .border(.5.dp, Color.Black.copy(alpha = .14f), CircleShape))
        }
    }
}
