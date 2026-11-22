// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.EmojiGeometryReport
import helium314.keyboard.settings.SearchSettingsScreen
import kotlinx.coroutines.delay

@Composable
fun DeviceCheckupScreen(onClickBack: () -> Unit, onOpenVoiceLab: () -> Unit) {
    val context = LocalContext.current
    DeviceCheckupSession.beginIfNeeded()
    val startedAtMs = DeviceCheckupSession.startTimeMs

    var testText by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { testText = "" } }

    var emojiGap by rememberSaveable { mutableStateOf(DeviceCheckOutcome.PENDING) }
    var suggestions by rememberSaveable { mutableStateOf(DeviceCheckOutcome.PENDING) }
    var toolbar by rememberSaveable { mutableStateOf(DeviceCheckOutcome.PENDING) }
    var clipboardGlass by rememberSaveable { mutableStateOf(DeviceCheckOutcome.PENDING) }
    var voice by rememberSaveable { mutableStateOf(DeviceCheckOutcome.PENDING) }
    var geometryCaptured by remember(startedAtMs) {
        mutableStateOf(EmojiGeometryReport.hasReportAfter(context, startedAtMs))
    }
    var copied by remember { mutableStateOf(false) }
    var copyFailed by remember { mutableStateOf(false) }

    LaunchedEffect(context, startedAtMs) {
        while (true) {
            delay(900)
            if (EmojiGeometryReport.hasReportAfter(context, startedAtMs)) {
                geometryCaptured = true
                break
            }
        }
    }

    val voiceMetrics = DeviceCheckupSession.voiceMetrics
    val results = DeviceCheckupReport.CheckResults(
        emojiGap = emojiGap,
        smartEmojiAndMotion = suggestions,
        toolbar = toolbar,
        clipboardGlass = clipboardGlass,
        voiceLab = voice,
    )
    val complete = results.complete()
    val reportReady = complete && (voice != DeviceCheckOutcome.GOOD || voiceMetrics != null)
    val colors = MaterialTheme.colorScheme

    SearchSettingsScreen(onClickBack = {
        DeviceCheckupSession.finish()
        onClickBack()
    }, title = "Device checkup", settings = emptyList()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Five checks · one report", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("Follow these steps on this screen. Mark each result once; the final button copies one plain-text report.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("The report contains device/layout measurements, check outcomes and optional voice timing/WER numbers. It excludes typed text, clipboard contents, reference phrases, transcripts, audio, screenshots and free-form notes.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Temporary keyboard test field", fontWeight = FontWeight.SemiBold)
                    Text("Tap here to open brrBoard. Use only the harmless sample phrase below; this field is temporary and never appears in the report.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it.take(160) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Keyboard test") },
                        placeholder = { Text("Happy birthday") },
                        keyboardOptions = KeyboardOptions.Default,
                    )
                }
            }

            CheckCard(
                number = 1,
                title = "Emoji Browse top gap",
                instruction = "With the test field focused, open Emoji Browse. Check that the category strip and first Emoji row start at the top without a blank band. Geometry is collected automatically.",
                selected = emojiGap,
                options = listOf(
                    DeviceCheckOutcome.GOOD to "Gap gone",
                    DeviceCheckOutcome.ISSUE to "Gap remains",
                    DeviceCheckOutcome.SKIPPED to "Couldn't test",
                ),
                onSelect = { emojiGap = it; copied = false; copyFailed = false },
            ) {
                Text(if (geometryCaptured) "Geometry trace: captured" else "Geometry trace: waiting for Emoji Browse",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }

            CheckCard(
                number = 2,
                title = "Smart Emoji and suggestion motion",
                instruction = "Type “Happy birthday” or “Great job”. Check the word/Emoji suggestions are relevant and change smoothly as you type.",
                selected = suggestions,
                options = listOf(
                    DeviceCheckOutcome.GOOD to "Relevant, smooth",
                    DeviceCheckOutcome.ISSUE to "Wrong or missing",
                    DeviceCheckOutcome.SKIPPED to "Couldn't test",
                ),
                onSelect = { suggestions = it; copied = false; copyFailed = false },
            )

            CheckCard(
                number = 3,
                title = "Toolbar layout and actions",
                instruction = "Open Toolbar. Check four columns with three rows visible, vertical scrolling, Settings first, and the footer action/icons switch correctly.",
                selected = toolbar,
                options = listOf(
                    DeviceCheckOutcome.GOOD to "All correct",
                    DeviceCheckOutcome.ISSUE to "Layout/action wrong",
                    DeviceCheckOutcome.SKIPPED to "Couldn't test",
                ),
                onSelect = { toolbar = it; copied = false; copyFailed = false },
            )

            CheckCard(
                number = 4,
                title = "Clipboard and panel glass",
                instruction = "Copy only the harmless sample phrase from the test field, open Clipboard, and scroll. Check its top blur changes progressively; also watch Emoji/Toolbar panels for seams or flicker.",
                selected = clipboardGlass,
                options = listOf(
                    DeviceCheckOutcome.GOOD to "Smooth",
                    DeviceCheckOutcome.ISSUE to "Seam, gap or flicker",
                    DeviceCheckOutcome.SKIPPED to "Couldn't test",
                ),
                onSelect = { clipboardGlass = it; copied = false; copyFailed = false },
            )

            CheckCard(
                number = 5,
                title = "One-take Parakeet comparison",
                instruction = "Open Voice Lab, record one short phrase, set the reference to match, install Ultra if needed, then compare both models on the same take. Only numeric accuracy/speed/memory results return here.",
                selected = voice,
                options = listOf(
                    DeviceCheckOutcome.GOOD to "Both completed",
                    DeviceCheckOutcome.ISSUE to "Failed or crashed",
                    DeviceCheckOutcome.SKIPPED to "Couldn't test",
                ),
                onSelect = { voice = it; copied = false; copyFailed = false },
            ) {
                Button(onClick = onOpenVoiceLab, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Voice Lab")
                }
                Text(voiceMetrics?.let { metricsSummary(it) }
                    ?: "Voice metrics have not returned. The lab's separate transcript copy is not needed for this checkup.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }

            Button(
                onClick = {
                    val geometry = if (geometryCaptured) EmojiGeometryReport.readLatest(context) else null
                    val report = DeviceCheckupReport.format(context, results, voiceMetrics, geometry)
                    copied = copyReport(context, report)
                    copyFailed = !copied
                },
                enabled = reportReady && !copied,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            ) {
                Text(when {
                    copied -> "Report copied"
                    reportReady -> "Copy final report"
                    complete && voice == DeviceCheckOutcome.GOOD -> "Wait for voice metrics"
                    complete -> "Copy final report"
                    else -> "Complete the five checks"
                })
            }
            if (copyFailed) Text("Could not access the clipboard. Tap the same button to retry.",
                style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
    }
}

@Composable
private fun CheckCard(
    number: Int,
    title: String,
    instruction: String,
    selected: DeviceCheckOutcome,
    options: List<Pair<DeviceCheckOutcome, String>>,
    onSelect: (DeviceCheckOutcome) -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("$number. $title", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(instruction, style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (outcome, label) ->
                    FilterChip(
                        selected = selected == outcome,
                        onClick = { onSelect(outcome) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
            extra?.invoke()
        }
    }
}

private fun metricsSummary(metrics: DeviceCheckupSession.VoiceMetrics): String {
    fun model(name: String, score: DeviceCheckupSession.ModelMetrics?) = score?.let {
        "$name WER ${it.werPercent?.let { value -> "$value%" } ?: "—"}, decode ${it.decodeMs} ms, peak PSS ${it.peakPssKb / 1024} MiB"
    } ?: "$name unavailable"
    return "Metrics only · ${metrics.audioDurationMs} ms audio · " +
        model("110M", metrics.current110m) + " · " + model("Ultra", metrics.ultra)
}

private fun copyReport(context: Context, report: String): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("brrBoard device checkup", report))
        true
    } catch (_: RuntimeException) {
        false
    }
}
