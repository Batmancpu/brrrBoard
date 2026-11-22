// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.SlideConfirmationPolicy
import kotlin.math.roundToInt

/** Reserved for irreversible or high-impact actions, never ordinary switches. */
@Composable
fun SlideActionDialog(title: String, explanation: String, instruction: String,
                      onDismiss: () -> Unit, onConfirmed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(explanation)
                SlideActionRail(instruction, onConfirmed)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The same iOS-inspired arrow rail as the IME, with a spring back on abort. */
@Composable
fun SlideActionRail(
    label: String,
    onConfirmed: () -> Unit,
    onDragStateChanged: (Boolean) -> Unit = {},
) {
    val latestConfirmed by rememberUpdatedState(onConfirmed)
    val latestDragState by rememberUpdatedState(onDragStateChanged)
    val density = LocalDensity.current
    val palette = MaterialTheme.colorScheme
    val railHeight = 55.dp
    val thumbDiameter = 49.dp
    var offset by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { latestDragState(false) }
    }
    val animated by animateFloatAsState(offset,
        animationSpec = spring(dampingRatio = .76f, stiffness = 440f), label = "slide-position")
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 3.dp)) {
        val travel = with(density) { (maxWidth - railHeight).toPx().coerceAtLeast(0f) }
        val thumbPx = with(density) { thumbDiameter.toPx() }
        Box(
            Modifier.fillMaxWidth().height(railHeight)
                .clip(RoundedCornerShape(railHeight / 2))
                .background(Brush.horizontalGradient(listOf(
                    palette.surfaceVariant, palette.surface, palette.surfaceVariant)))
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(
                        "Confirm: $label") { latestConfirmed(); true })
                }
                .pointerInput(travel) {
                    detectDragGestures(
                        onDragStart = { point: Offset ->
                            armed = point.x <= thumbPx + 12.dp.toPx()
                            dragging = armed
                            latestDragState(armed)
                            offset = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            if (armed) offset = (offset + drag.x).coerceIn(0f, travel)
                        },
                        onDragEnd = {
                            val confirm = SlideConfirmationPolicy.approved(armed, dragging,
                                false, if (travel > 0f) offset / travel else 0f)
                            armed = false
                            dragging = false
                            latestDragState(false)
                            if (confirm) latestConfirmed() else offset = 0f
                        },
                        onDragCancel = {
                            armed = false
                            dragging = false
                            latestDragState(false)
                            offset = 0f
                        },
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val labelOpacity = (1f - (if (travel > 0f) animated / travel else 0f))
                .coerceIn(.12f, 1f)
            Text(text = label,
                color = palette.onSurfaceVariant.copy(alpha = labelOpacity),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = railHeight, end = 7.dp))
            Box(modifier = Modifier.align(Alignment.CenterStart)
                .offset { IntOffset(animated.roundToInt() + with(density) { 3.dp.roundToPx() }, 0) }
                .size(thumbDiameter)
                .clip(CircleShape)
                .background(Brush.linearGradient(
                    listOf(Color(0xFFE4AB4D), Color(0xFFFFE2A8)))),
                contentAlignment = Alignment.Center) {
                Text("››", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color(0xFF32240F))
            }
        }
    }
}
