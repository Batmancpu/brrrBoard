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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.settings.getTransitionAnimationScale

/** Glass-style switch, Android toggle semantics, 48dp touch target, RTL-aware travel. */
@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val dark = isSystemInDarkTheme()
    val reduceMotion = getTransitionAnimationScale(LocalContext.current) <= 0f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val track by animateColorAsState(
        targetValue = (if (checked) {
            if (dark) Color(0xFF30D158) else Color(0xFF248A3D)
        } else if (dark) Color(0xFF515159) else Color(0xFFD1D1D6)).let {
            if (enabled) it else it.copy(alpha = .45f)
        }, animationSpec = if (reduceMotion) snap() else tween(180), label = "switch-track")
    val thumbWidth by animateDpAsState(if (pressed && enabled) 30.dp else 26.dp,
        animationSpec = if (reduceMotion) snap() else tween(150), label = "switch-press")
    val position by animateDpAsState(if (checked) 48.dp - thumbWidth else 2.dp,
        animationSpec = if (reduceMotion) snap() else tween(180), label = "switch-slide")
    Box(modifier.size(width = 52.dp, height = 48.dp).then(
        if (onCheckedChange == null) Modifier else Modifier.toggleable(
            value = checked, enabled = enabled, role = Role.Switch,
            interactionSource = interaction, indication = null, onValueChange = onCheckedChange)
    ), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 50.dp, height = 30.dp).clip(CircleShape).background(track)) {
            Box(Modifier.align(Alignment.CenterStart).offset(x = position)
                .size(width = thumbWidth, height = 26.dp)
                .shadow(if (enabled) 2.dp else 0.dp, CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 1f else .65f), CircleShape)
                .border(.5.dp, Color.Black.copy(alpha = .07f), CircleShape))
        }
    }
}
