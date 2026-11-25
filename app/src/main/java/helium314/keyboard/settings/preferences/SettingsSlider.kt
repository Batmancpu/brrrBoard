// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Preserve native slider gestures, semantics, range and snapping; replace only its drawing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(value: Float, onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }) {
    val active = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .4f)
    val inactive = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else .5f)
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    Slider(value = value, onValueChange = onValueChange, modifier = modifier,
        enabled = enabled, valueRange = valueRange, steps = steps,
        onValueChangeFinished = onValueChangeFinished, interactionSource = interactionSource,
        thumb = {
            Box(Modifier.size(28.dp).shadow(if (pressed || dragged) 4.dp else 2.dp, CircleShape)
                .background(Color.White.copy(alpha = if (enabled) 1f else .7f), CircleShape)
                .border(.5.dp, Color.Black.copy(alpha = .12f), CircleShape))
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val corner = CornerRadius(size.height / 2f)
                drawRoundRect(inactive, cornerRadius = corner)
                val length = valueRange.endInclusive - valueRange.start
                val fraction = if (length > 0f) ((state.value - valueRange.start) / length).coerceIn(0f, 1f) else 0f
                val filled = size.width * fraction
                if (filled > 0f) drawRoundRect(active,
                    topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - filled else 0f, 0f),
                    size = Size(filled, size.height), cornerRadius = corner)
            }
        })
}
