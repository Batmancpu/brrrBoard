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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.brain.SwipeCorrectionLearner
import helium314.keyboard.latin.brain.BrainPerformanceMetrics
import helium314.keyboard.keyboard.emoji.BrrSmartEmojiEngine
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.dialogs.SlideActionDialog
import helium314.keyboard.settings.SearchSettingsScreen
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun BrainDashboardScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmEraseEmoji by remember { mutableStateOf(false) }
    var erasingEmoji by remember { mutableStateOf(false) }
    var trialText by remember { mutableStateOf("") }
    // Settings can stay open alongside the IME. Refresh RAM-only diagnostic counters while visible.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_800L)
            refresh++
        }
    }
    val learner = remember(context) { SwipeCorrectionLearner.get(context) }
    val snapshot = remember(refresh) { learner.snapshot() }
    val emoji = remember(refresh) { readEmojiUsage(context) }
    val activity = remember(refresh) { BrainPerformanceMetrics.activity() }
    val typingSpeed = remember(refresh) { BrainPerformanceMetrics.snapshot(BrainPerformanceMetrics.Stage.TYPING) }
    val wordSpeed = remember(refresh) { BrainPerformanceMetrics.snapshot(BrainPerformanceMetrics.Stage.NEXT_WORD) }
    val swipeSpeed = remember(refresh) { BrainPerformanceMetrics.snapshot(BrainPerformanceMetrics.Stage.SWIPE) }
    val emojiSpeed = remember(refresh) { BrainPerformanceMetrics.snapshot(BrainPerformanceMetrics.Stage.SMART_EMOJI) }
    val enabled = context.prefs().getBoolean(
        Settings.PREF_LEARN_SWIPE_CORRECTIONS, Defaults.PREF_LEARN_SWIPE_CORRECTIONS
    ) && context.prefs().getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)

    SearchSettingsScreen(onClickBack = onClickBack,
        title = "BR Brain", settings = emptyList()) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val palette = MaterialTheme.colorScheme
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(21.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Local intelligence", style = MaterialTheme.typography.titleLarge,
                    color = palette.onSurface, fontWeight = FontWeight.SemiBold)
                Text(if (enabled) "Learning ON in eligible text fields"
                     else "Learning paused by your settings",
                    color = palette.onSurface,
                    style = MaterialTheme.typography.titleMedium)
                Text("Incognito, protected and no-personalized-learning fields are excluded.",
                    color = palette.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }

            Text("Real-time processing speed",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Measured locally per suggestion request. These are computation times, not key-to-screen delay, model accuracy, or cloud latency. The latest, average and 95th-percentile values update every 1.8 seconds while this page is open.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LatencyMetric("Typing", typingSpeed, Modifier.weight(1f))
                LatencyMetric("Next word", wordSpeed, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LatencyMetric("Swipe", swipeSpeed, Modifier.weight(1f))
                LatencyMetric("Smart Emoji", emojiSpeed, Modifier.weight(1f))
            }
            Text("The 95th percentile uses up to the most recent 128 eligible requests. A dash means this process hasn't measured that activity yet. Protected and incognito fields are not counted.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

            Text("Voice noise reduction · crash-safe mode",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GTCRN filtering paused", fontWeight = FontWeight.SemiBold)
                    Text("A reported keyboard crash shortly after tapping the mic is being investigated. The last known working private 16-kHz capture and Parakeet transcription path is restored. GTCRN weights remain included in the APK for future verified activation, but no native noise-filter code is called in this build.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Speech still stays on-device. This is a stability rollback, not evidence that filtering improved or worsened recognition accuracy.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Suggestions · actual activity",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceMetric("Batches", activity.suggestionBatches.toString(),
                    "Prediction requests", Modifier.weight(1f))
                EvidenceMetric("Candidates", activity.candidatesReturned.toString(),
                    "Returned, not all displayed", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceMetric("Word taps", activity.textSuggestionTaps.toString(),
                    "Manually selected", Modifier.weight(1f))
                EvidenceMetric("Emoji taps", activity.emojiSuggestionTaps.toString(),
                    "Selected from strip", Modifier.weight(1f))
            }
            Text("A tap means you chose a suggestion; it does not prove the suggestion was correct. Accuracy remains unmeasured until we have verified ground-truth test data.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Try it yourself", style = MaterialTheme.typography.titleMedium)
                    Text("Tap the field and type “kal office jaana hai” or “happy birthday”. Watch the keyboard's next-word suggestions and Emoji chip, then return here to see measured requests and time. The dashboard keeps this trial text only in screen memory.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = trialText,
                        onValueChange = { trialText = it.take(240) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Practice typing · English / Hinglish") },
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("What powers your suggestions", fontWeight = FontWeight.SemiBold,
                        color = palette.onTertiaryContainer)
                    Text("Typing and next word: existing offline dictionaries and native word history. Swipe: native gesture candidates, n-gram reranking and the existing confirmed-correction learner. Emoji: reviewed phrase cues, Recents and encrypted local associations. Bilingual neural ContextLM: not yet trained or installed.",
                        color = palette.onTertiaryContainer, style = MaterialTheme.typography.bodySmall)
                    Text("How BR Brain learns: deliberate word/swipe corrections and Emoji selections in eligible fields. It never uses protected fields, clipboard or voice transcripts as training input.",
                        color = palette.onTertiaryContainer, style = MaterialTheme.typography.bodySmall)
                }
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

            Text("Smart Emoji · learned on your device",
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
                    Text("Actual Emoji frequency, combined with BR Brain's encrypted app/field context patterns.",
                        color = palette.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(if (BrrSmartEmojiEngine.isLoaded())
                    "Loaded context associations: " + BrrSmartEmojiEngine.learnedPatternCount() +
                        " encrypted phrase/Emoji patterns. No full messages or app names are saved."
                else "Encrypted Emoji associations are still loading; zero must not be mistaken for no learning.",
                style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
            OutlinedButton(onClick = { confirmEraseEmoji = true }, enabled = !erasingEmoji) {
                Text(if (erasingEmoji) "Erasing Emoji patterns…" else "Erase Smart Emoji patterns")
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
                OutlinedButton(onClick = {
                    BrainPerformanceMetrics.clearSession()
                    refresh++
                }) { Text("Reset speed stats") }
            }
            OutlinedButton(onClick = { confirmErase = true }) { Text("Erase swipe learning") }
            Text("Swipe correction storage: " + snapshot.storageStatus + ". This dashboard never records raw typing. Session counters restart when the keyboard service restarts.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmEraseEmoji) {
        SlideActionDialog(
            title = "Erase Smart Emoji patterns?",
            explanation = "Deletes BR Brain's encrypted phrase, app and field Emoji associations. Normal Emoji Recents and frequency remain.",
            instruction = "slide to erase patterns",
            onDismiss = { confirmEraseEmoji = false },
            onConfirmed = {
                erasingEmoji = true
                confirmEraseEmoji = false
                BrrSmartEmojiEngine.forgetLearnedPatterns(context) {
                    erasingEmoji = false
                    refresh++
                }
            },
        )
    }
    if (confirmErase) {
        SlideActionDialog(
            title = "Erase learned swipe corrections?",
            explanation = "This deletes stored swipe correction pairs on this device. It does not erase your system dictionary or Emoji history.",
            instruction = "slide to erase learning",
            onDismiss = { confirmErase = false },
            onConfirmed = { learner.clear(); refresh++; confirmErase = false },
        )
    }
}

private fun latencyDisplay(milliseconds: Double): String =
    if (milliseconds < 1.0) String.format(Locale.US, "%.2f ms", milliseconds)
    else String.format(Locale.US, "%.1f ms", milliseconds)

@Composable
private fun LatencyMetric(
    title: String, metric: BrainPerformanceMetrics.Snapshot, modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = colors.onPrimaryContainer)
            Text(if (metric.totalRequests == 0L) "—" else latencyDisplay(metric.lastMs),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer)
            Text(if (metric.totalRequests == 0L) "Awaiting samples"
                else "Average " + latencyDisplay(metric.averageMs),
                style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
            Text(if (metric.windowRequests < 20) "P95: collecting samples"
                else "P95 " + latencyDisplay(metric.p95Ms),
                style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
            Text(metric.totalRequests.toString() + " requests",
                style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
        }
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
