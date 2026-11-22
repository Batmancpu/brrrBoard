// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.settings.SettingsResetManager
import helium314.keyboard.settings.dialogs.SlideActionRail
import kotlinx.coroutines.delay

/** The destructive reset lives with backup/restore and preserves non-settings user data. */
@Composable
fun ResetSettingsControl() {
    val context = LocalContext.current
    var resetUndoMs by remember { mutableStateOf(SettingsResetManager.remainingMs(context)) }
    LaunchedEffect(context, resetUndoMs > 0L) {
        while (resetUndoMs > 0L) {
            delay(60_000L)
            resetUndoMs = SettingsResetManager.remainingMs(context)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reset keyboard settings", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Restore preferences to their defaults. Dictionaries, clipboard history, recordings and installed voice models stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SlideActionRail(
                label = "Slide to reset settings",
                onConfirmed = {
                    if (SettingsResetManager.reset(context)) {
                        resetUndoMs = SettingsResetManager.remainingMs(context)
                        AudioAndHapticFeedbackManager.getInstance().refreshForSettings(context)
                        runCatching { KeyboardSwitcher.getInstance().reloadKeyboard() }
                        AudioAndHapticFeedbackManager.getInstance().performImportantActionConfirmation()
                    }
                },
                onDragStateChanged = { dragging ->
                    val feedback = AudioAndHapticFeedbackManager.getInstance()
                    if (dragging) feedback.startImportantActionHapticLoop()
                    else feedback.stopImportantActionHapticLoop()
                },
            )
            if (resetUndoMs > 0L) {
                val minutes = (resetUndoMs / 60_000L).coerceAtLeast(1L)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Settings reset · undo available for ${minutes}m",
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (SettingsResetManager.undo(context)) {
                            resetUndoMs = 0L
                            AudioAndHapticFeedbackManager.getInstance().refreshForSettings(context)
                            runCatching { KeyboardSwitcher.getInstance().reloadKeyboard() }
                        }
                    }) { Text("Undo changes") }
                }
            }
        }
    }
}
