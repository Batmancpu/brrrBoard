// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.brain.SwipeCorrectionLearner
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import org.json.JSONObject

@Composable
fun BrainDashboardScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var confirmErase by remember { mutableStateOf(false) }
    val learner = remember(context) { SwipeCorrectionLearner.get(context) }
    val snapshot = remember(refresh) { learner.snapshot() }
    val emoji = remember(refresh) { readEmojiUsage(context) }
    val enabled = context.prefs().getBoolean(
        Settings.PREF_LEARN_SWIPE_CORRECTIONS, Defaults.PREF_LEARN_SWIPE_CORRECTIONS
    ) && context.prefs().getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)

    SearchSettingsScreen(onClickBack = onClickBack,
        title = "brrBrain · Learning dashboard", settings = emptyList()) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val palette = MaterialTheme.colorScheme
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(
                        palette.primaryContainer, palette.tertiaryContainer,
                        palette.secondaryContainer)), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Local intelligence", style = MaterialTheme.typography.titleLarge,
                    color = palette.onPrimaryContainer, fontWeight = FontWeight.Bold)
                Text(if (enabled) "Learning ON in eligible text fields"
                     else "Learning paused by your settings",
                    color = palette.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium)
                Text("Incognito, protected and no-personalized-learning fields are excluded.",
                    color = palette.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall)
            }

            Text("Swipe learning · measured",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceMetric("Swipes", snapshot.swipesThisSession.toString(),
                    "This IME session", Modifier.weight(1f))
                EvidenceMetric("Corrections", snapshot.confirmedCorrectionsThisSession.toString(),
                    "Taught this session", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceMetric("Patterns", snapshot.storedPairs.toString(),
                    "Stored word pairs", Modifier.weight(1f))
                EvidenceMetric("Ready", snapshot.readyPairs.toString(),
                    "Confirmed at least twice", Modifier.weight(1f))
            }
            EvidenceMetric("Recall suggestions offered",
                snapshot.recallOffersThisSession.toString(),
                "This session; offered does not mean accepted", Modifier.fillMaxWidth())

            Text("Review what has been learned",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (snapshot.recentAssociations.isEmpty()) {
                        Text("No swipe corrections recorded yet. Correct a swipe deliberately twice to make its alternative eligible.")
                    } else {
                        snapshot.recentAssociations.forEach { pair ->
                            Text(pair, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("Review for unwanted learning. These are observed correction pairs, not a guarantee of accuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant)
                }
            }

            Text("Emoji learning · actual saved usage",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceMetric("Selections", emoji.totalUses.toString(),
                    "Recorded Emoji choices", Modifier.weight(1f))
                EvidenceMetric("Variety", emoji.distinct.toString(),
                    "Different Emoji used", Modifier.weight(1f))
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Most-used Emoji", fontWeight = FontWeight.SemiBold,
                        color = palette.onSecondaryContainer)
                    Text(if (emoji.top.isEmpty()) "No Emoji usage recorded yet."
                        else emoji.top.joinToString("   ") { it.first + " ×" + it.second },
                        color = palette.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Directly from brrBoard's existing frequency records.",
                        color = palette.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Prediction accuracy", style = MaterialTheme.typography.titleMedium)
                    Text("Not measured yet", fontWeight = FontWeight.Bold)
                    Text("The keyboard cannot yet verify your intended word against every swipe. We do not invent accuracy percentages.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { refresh++ }) { Text("Refresh actual data") }
                OutlinedButton(onClick = { confirmErase = true }) { Text("Erase swipe learning") }
            }
            Text("Swipe correction storage: " + snapshot.storageStatus + ". This dashboard never records raw typing. Session counters restart when the keyboard service restarts.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase learned swipe corrections?") },
            text = { Text("This deletes stored swipe correction pairs on this device. It does not erase your system dictionary or Emoji history.") },
            confirmButton = { Button(onClick = {
                learner.clear(); refresh++; confirmErase = false
            }) { Text("Erase learning") } },
            dismissButton = { OutlinedButton(onClick = { confirmErase = false }) {
                Text("Cancel")
            } }
        )
    }
}

@Composable
private fun EvidenceMetric(label: String, number: String, note: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimaryContainer)
            Text(number, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
            Text(note, style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer)
        }
    }
}

private data class EmojiUsage(val totalUses: Long, val distinct: Int,
    val top: List<Pair<String, Int>>)

private fun readEmojiUsage(context: Context): EmojiUsage {
    return try {
        val root = JSONObject(context.prefs().getString("pref_adaptive_emoji_metadata", "{}") ?: "{}")
        val counts = ArrayList<Pair<String, Int>>()
        val keys = root.keys()
        var total = 0L
        while (keys.hasNext() && counts.size < 256) {
            val value = keys.next()
            val count = (root.optJSONObject(value)?.optInt("freq", 0) ?: 0).coerceIn(0, 1_000_000)
            if (count > 0) {
                counts.add(value to count)
                total += count
            }
        }
        EmojiUsage(total, counts.size, counts.sortedByDescending { it.second }.take(5))
    } catch (_: Exception) {
        EmojiUsage(0, 0, emptyList())
    }
}
