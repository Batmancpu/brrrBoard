// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import helium314.keyboard.latin.voice.VoiceDenoiseMetrics
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceNoiseChoice
import helium314.keyboard.latin.voice.VoiceNoiseLab
import helium314.keyboard.latin.voice.VoiceNoiseMode
import helium314.keyboard.settings.SearchSettingsScreen
import java.util.Locale
import kotlinx.coroutines.delay

/** One physical recording per trial. GTCRN reuses the SAME PCM for A/B playback. */
@Composable
fun VoiceNoiseLabScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    val lab = remember(context) { VoiceNoiseLab(context) }
    DisposableEffect(lab) { onDispose { lab.close() } }
    var refresh by remember { mutableIntStateOf(0) }
    var mode by remember(context) { mutableStateOf(VoiceNoiseChoice.selected(context)) }
    var input by remember { mutableStateOf(VoiceNoiseLab.Input.PHONE) }
    var hinglish by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf<VoiceNoiseLab.Key?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) permissionPending?.let(lab::record)
        permissionPending = null
    }
    LaunchedEffect(Unit) { while (true) { delay(300L); refresh++ } }
    val state = remember(refresh) { lab.snapshot() }
    val live = remember(refresh) { VoiceDenoiseMetrics.snapshot() }
    val reference = if (hinglish) HINGLISH_SENTENCE else ENGLISH_SENTENCE
    val key = VoiceNoiseLab.Key(input, mode == VoiceNoiseMode.ANDROID)
    val take = state.takes.firstOrNull()
    val busy = state.recording || state.playing || state.decoding || state.enhancing
    val palette = MaterialTheme.colorScheme

    SearchSettingsScreen(onClickBack = onClickBack, title = "Voice & Noise Reduction",
        settings = emptyList()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = palette.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Live blue-mic status", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(if (live.blueMicCapturing) "● Capturing now" else "○ Not capturing")
                    Text("Android effect: ${live.platformNoiseSuppression}")
                    Text("Blue mic captures this process: ${live.completedBlueCaptures}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Effect ACTIVE means Android accepted it, not that noise was measurably reduced.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Suppression for blue-mic dictation and this lab",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Select one engine. The usual keyboard microphone captures ONCE. Parakeet 110M still transcribes offline.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    mode = VoiceNoiseMode.ANDROID
                    VoiceNoiseChoice.select(context, mode)
                    lab.clear()
                }, enabled = !busy) { Text(if (mode == VoiceNoiseMode.ANDROID) "✓ Android" else "Android") }
                OutlinedButton(onClick = {
                    mode = VoiceNoiseMode.GTCRN
                    VoiceNoiseChoice.select(context, mode)
                    lab.clear()
                }, enabled = !busy) { Text(if (mode == VoiceNoiseMode.GTCRN) "✓ Our GTCRN" else "Our GTCRN") }
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (mode == VoiceNoiseMode.ANDROID)
                        "Android NoiseSuppressor (capture-time)" else "Bundled GTCRN (after capture)",
                        fontWeight = FontWeight.SemiBold)
                    Text(if (mode == VoiceNoiseMode.ANDROID)
                        "Android processes your microphone stream only when its effect is available. Earbuds may also apply firmware-level noise reduction."
                    else "Android's optional effect is OFF. Our bundled GTCRN gets the recorded PCM in a separate crash-contained process. If it fails, Parakeet receives untouched speech. GTCRN is limited to 12 seconds per take; longer dictation bypasses it to protect memory and latency.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(if (VoiceModelInstaller.isInstalled(context)) "Offline ASR model: installed"
                        else "Offline ASR model: install needed for scoring",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("One recording · honest listening test", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Choose a microphone and record ONCE. In GTCRN mode, Original and GTCRN use identical captured audio. Android suppression runs DURING capture; it cannot be applied afterward to the same buffer.",
                style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { input = VoiceNoiseLab.Input.PHONE; lab.clear() },
                    enabled = !busy) { Text(if (input == VoiceNoiseLab.Input.PHONE) "✓ Phone mic" else "Phone mic") }
                OutlinedButton(onClick = { input = VoiceNoiseLab.Input.EARBUDS; lab.clear() },
                    enabled = !busy) { Text(if (input == VoiceNoiseLab.Input.EARBUDS) "✓ Earbuds mic" else "Earbuds mic") }
            }
            if (input == VoiceNoiseLab.Input.EARBUDS) Text(
                "Connect your Nirvana Ion. A rejected, silenced or mismatched Android input will be reported, not falsely labeled as an earbuds recording.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { hinglish = false }, enabled = !busy) {
                    Text(if (!hinglish) "✓ English" else "English")
                }
                OutlinedButton(onClick = { hinglish = true }, enabled = !busy) {
                    Text(if (hinglish) "✓ Hinglish" else "Hinglish")
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.secondaryContainer)) {
                Text("Optional test phrase: “$reference”", modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold)
            }
            Text("Status: ${state.status}")
            if (state.recording) {
                Text("Live RMS level: ${state.levelPercent}% (not an improvement score)",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = lab::stopRecording, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop recording")
                }
            } else Button(onClick = {
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) lab.record(key)
                else { permissionPending = key; launcher.launch(Manifest.permission.RECORD_AUDIO) }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Record once (up to 12 seconds)")
            }
            if (take != null) Card(colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Actual source: ${take.source}", fontWeight = FontWeight.SemiBold)
                    Text(take.suppressionStatus)
                    Text("${take.durationMs / 1000.0} seconds · RMS ${take.signalLevelPercent}% · clipping " +
                        String.format(Locale.US, "%.2f%%", take.clippingPercent),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Model: ${take.enhanceStatus}" +
                        (take.enhanceMs?.let { " · ${it} ms" } ?: ""),
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { lab.play(take.key) }, enabled = !busy) { Text("▶ Original") }
                        if (!take.key.suppression) OutlinedButton(
                            onClick = { lab.play(take.key, enhanced = true) },
                            enabled = !busy && take.enhanceMs != null) { Text("▶ GTCRN") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { lab.transcribe(take.key, reference) },
                            enabled = !busy) { Text("Score original") }
                        if (!take.key.suppression) OutlinedButton(
                            onClick = { lab.transcribe(take.key, reference, enhanced = true) },
                            enabled = !busy && take.enhanceMs != null) { Text("Score GTCRN") }
                    }
                    if (take.transcript != null) Text("Original → Parakeet: ${take.transcript} · WER ${take.wordErrorPercent ?: "—"}% · decode ${take.decodeMs ?: "—"} ms")
                    if (take.enhancedTranscript != null) Text("GTCRN → Parakeet: ${take.enhancedTranscript} · WER ${take.enhancedWordErrorPercent ?: "—"}%")
                    if (take.transcript != null || take.enhancedTranscript != null)
                        Text("WER assumes you spoke the displayed phrase exactly.",
                            style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = lab::clear, enabled = !busy) { Text("Erase this trial") }
            Text("Privacy: recordings and transcripts remain only in volatile memory and are wiped when leaving. No files, cloud, audio logs or second microphones. GTCRN runs in an isolated child with a hard timeout; on failure, original audio remains available. Calls block capture. Playback follows Android's selected output.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val ENGLISH_SENTENCE = "Please meet me at the station tomorrow morning."
private const val HINGLISH_SENTENCE = "Kal office jaana hai, please call me at five."
