// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.voice.VoiceDenoiseMetrics
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceModelChoice
import helium314.keyboard.latin.voice.VoiceNoiseChoice
import helium314.keyboard.latin.voice.VoiceNoiseLab
import helium314.keyboard.latin.voice.VoiceNoiseMode
import helium314.keyboard.latin.voice.UltraVoiceModelManifest
import helium314.keyboard.settings.SearchSettingsScreen
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var referencePhrase by remember(hinglish) {
        mutableStateOf(if (hinglish) HINGLISH_SENTENCE else ENGLISH_SENTENCE)
    }
    var showUltraInstallConfirm by remember { mutableStateOf(false) }
    var ultraInstallMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var permissionPending by remember { mutableStateOf<VoiceNoiseLab.Key?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) permissionPending?.let(lab::record)
        permissionPending = null
    }
    LaunchedEffect(Unit) { while (true) { delay(300L); refresh++ } }
    val state = remember(refresh) { lab.snapshot() }
    val live = remember(refresh) { VoiceDenoiseMetrics.snapshot() }
    val reference = referencePhrase
    val key = VoiceNoiseLab.Key(input, mode)
    val take = state.takes.firstOrNull()
    LaunchedEffect(take?.modelComparison) {
        take?.modelComparison?.let { DeviceCheckupSession.record(it, take.durationMs) }
    }
    val oldModelInstalled = remember(refresh / 3) { VoiceModelInstaller.isInstalled(context) }
    val ultraBundled = remember(context) {
        VoiceModelInstaller.hasBundled(context, UltraVoiceModelManifest.pack)
    }
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
            Text("Noise-reduction comparison mode",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("OFF is raw microphone, Android is device capture-time filtering, and brrClean is native GTCRN. Mode buttons affect only this lab. Keyboard dictation uses the model selected under Everyday tools → Offline voice.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    mode = VoiceNoiseMode.OFF
                    lab.clear()
                }, enabled = !busy) { Text(if (mode == VoiceNoiseMode.OFF) "✓ OFF" else "OFF") }
                OutlinedButton(onClick = {
                    mode = VoiceNoiseMode.ANDROID
                    lab.clear()
                }, enabled = !busy) { Text(if (mode == VoiceNoiseMode.ANDROID) "✓ Android" else "Android") }
                OutlinedButton(onClick = {
                    mode = VoiceNoiseMode.GTCRN
                    lab.clear()
                }, enabled = !busy) { Text(if (mode == VoiceNoiseMode.GTCRN) "✓ brrClean" else "brrClean") }
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(when (mode) {
                        VoiceNoiseMode.OFF -> "Raw microphone · no optional suppression"
                        VoiceNoiseMode.ANDROID -> "Android NoiseSuppressor (capture-time)"
                        VoiceNoiseMode.GTCRN -> "brrClean native GTCRN (after capture)"
                    },
                        fontWeight = FontWeight.SemiBold)
                    Text(when (mode) {
                        VoiceNoiseMode.OFF -> "No app-controlled effect or GTCRN. Phone hardware or earbuds firmware may still process audio. The lab can optionally process the same raw take through brrClean for A/B listening."
                        VoiceNoiseMode.ANDROID -> "Android processes your microphone stream only when its effect is available. Earbuds may also apply firmware-level noise reduction."
                        VoiceNoiseMode.GTCRN -> "Android's optional effect is OFF. Native C++ GTCRN processes a copy in a separate crash-contained process. If it fails, Parakeet receives untouched speech. Maximum 12 seconds; longer dictation safely bypasses it."
                    },
                        style = MaterialTheme.typography.bodySmall)
                    Text(VoiceModelChoice.active(context)?.let { pack ->
                        "Keyboard model: ${if (pack.id == UltraVoiceModelManifest.ID) "Ultra 0.6B" else "110M"} · installed"
                    } ?: "Offline ASR model: install needed for scoring",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("One recording · honest listening test", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Choose a microphone and record ONCE. Original and brrClean use identical captured audio when Android's effect is OFF. Android suppression runs DURING capture; it cannot be applied afterward to the same buffer.",
                style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { input = VoiceNoiseLab.Input.PHONE; lab.clear() },
                    enabled = !busy) { Text(if (input == VoiceNoiseLab.Input.PHONE) "✓ Phone mic" else "Phone mic") }
                OutlinedButton(onClick = { input = VoiceNoiseLab.Input.EARBUDS; lab.clear() },
                    enabled = !busy) { Text(if (input == VoiceNoiseLab.Input.EARBUDS) "✓ Earbuds mic" else "Earbuds mic") }
            }
            Text("Noise mode selection applies to this lab trial only; the keyboard's brrClean preference stays unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.onSurfaceVariant)
            if (input == VoiceNoiseLab.Input.EARBUDS) Text(
                "Connect your Nirvana Ion. Android must activate its communication mic route. Rejected, silenced, unverified or mismatched audio will be discarded, never called an earbuds recording.",
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
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reference transcript for accuracy scoring", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = referencePhrase, onValueChange = { referencePhrase = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3,
                        supportingText = { Text("Edit this to match exactly what you say. It stays on this screen.") })
                }
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
                        if (take.key.enhance) OutlinedButton(
                            onClick = { lab.play(take.key, enhanced = true) },
                            enabled = !busy && take.enhanceMs != null) { Text("▶ brrClean") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { lab.transcribe(take.key, reference) },
                            enabled = !busy) { Text("Score original") }
                        if (take.key.enhance) OutlinedButton(
                            onClick = { lab.transcribe(take.key, reference, enhanced = true) },
                            enabled = !busy && take.enhanceMs != null) { Text("Score brrClean") }
                    }
                    if (take.transcript != null) Text("Original → Parakeet: ${take.transcript} · WER ${take.wordErrorPercent ?: "—"}% · decode ${take.decodeMs ?: "—"} ms")
                    if (take.enhancedTranscript != null) Text("GTCRN → Parakeet: ${take.enhancedTranscript} · WER ${take.enhancedWordErrorPercent ?: "—"}%")
                    if (take.transcript != null || take.enhancedTranscript != null)
                        Text("WER assumes you spoke the displayed phrase exactly.",
                            style = MaterialTheme.typography.bodySmall)
                    val ultraInstalled = remember(refresh / 3) {
                        VoiceModelInstaller.isInstalled(context, UltraVoiceModelManifest.pack)
                    }
                    val installProgress = remember(refresh) {
                        VoiceModelInstaller.progress(UltraVoiceModelManifest.pack)
                    }
                    Text("Parakeet Ultra 0.6B · optional side-by-side test" +
                        if (ultraInstalled) " · installed" else if (ultraBundled) " · available in this update" else " · not bundled",
                        fontWeight = FontWeight.SemiBold)
                    Text("Keyboard dictation uses ${VoiceModelChoice.active(context)?.let { if (it.id == UltraVoiceModelManifest.ID) "Ultra 0.6B" else "110M" } ?: "no installed model"}. Comparison runs both models offline, cold and one at a time on this exact recording.",
                        style = MaterialTheme.typography.bodySmall)
                    if (installProgress.state == VoiceModelInstaller.InstallState.INSTALLING) {
                        LinearProgressIndicator(progress = { installProgress.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("Installing private Ultra model: ${installProgress.percent}%")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!ultraInstalled) OutlinedButton(
                            onClick = { showUltraInstallConfirm = true },
                            enabled = ultraBundled && !busy && installProgress.state != VoiceModelInstaller.InstallState.INSTALLING) {
                            Text(if (ultraBundled) "Install Ultra test model" else "Ultra model not included")
                        }
                        Button(onClick = { lab.compareModels(take.key, reference) },
                            enabled = !busy && ultraInstalled && oldModelInstalled) {
                            Text("Compare both on same audio")
                        }
                    }
                    ultraInstallMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    take.modelComparison?.let { comparison ->
                        Text("A/B results · same take · reference: ${comparison.reference}",
                            fontWeight = FontWeight.SemiBold)
                        Text("${comparison.firstDecoded} decoded first; both models were cold-loaded sequentially.",
                            style = MaterialTheme.typography.bodySmall)
                        comparison.current110m?.let {
                            ComparisonResult("Parakeet 110M", it, take.durationMs.toLong())
                        } ?: Text("Parakeet 110M result unavailable.")
                        comparison.ultra?.let {
                            ComparisonResult("Parakeet Ultra 0.6B", it, take.durationMs.toLong())
                        } ?: Text("Parakeet Ultra result unavailable.")
                        comparison.failure?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text("WER is measured against the editable reference; lower is better. Load, decode and total times are milliseconds. RTF is total time ÷ audio duration; lower is faster. Peak memory is sampled process PSS.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedButton(onClick = lab::clear, enabled = !busy) { Text("Erase this trial") }
            Text("Privacy: recordings and transcripts stay in volatile memory and are wiped when leaving. No audio files, cloud, audio logs or second microphones. Results are displayed on screen and are never copied automatically. GTCRN runs in an isolated child with a hard timeout; on failure, original audio remains available. Calls block capture. Playback follows Android's selected output.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showUltraInstallConfirm) AlertDialog(
        onDismissRequest = { showUltraInstallConfirm = false },
        title = { Text("Install Parakeet Ultra test model?") },
        text = { Text(if (ultraBundled)
            "This optional model uses about 640 MiB of private storage after installation. Its verified assets are inside this update; no download or audio upload occurs. It will not replace or remove the 110M model."
            else "This lightweight update does not include Ultra model files and will not download them. If Ultra is already verified in private app storage, it remains available after updates.") },
        confirmButton = { TextButton(onClick = {
            showUltraInstallConfirm = false
            scope.launch {
                val installed = withContext(Dispatchers.IO) {
                    VoiceModelInstaller.installBundledIfPresent(context, UltraVoiceModelManifest.pack)
                }
                ultraInstallMessage = if (installed) "Ultra model installed and verified. The default dictation model is unchanged."
                    else "Ultra installation failed or this update did not include its model. Check available storage."
            }
        }) { Text("Install") } },
        dismissButton = { TextButton(onClick = { showUltraInstallConfirm = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ComparisonResult(label: String, score: VoiceNoiseLab.ModelScore, audioDurationMs: Long) {
    val realTimeFactor = if (audioDurationMs > 0L) score.totalMs.toDouble() / audioDurationMs else 0.0
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text("Transcript: ${score.transcript}")
        Text("WER ${score.wordErrorPercent?.let { "$it%" } ?: "—"} · load ${score.modelLoadMs} ms · decode ${score.decodeMs} ms · total ${score.totalMs} ms · RTF " +
            String.format(Locale.US, "%.2f", realTimeFactor) + "× · wall ${score.wallMs} ms · peak ${score.peakPssKb / 1024} MiB",
            style = MaterialTheme.typography.bodySmall)
    }
}

private const val ENGLISH_SENTENCE = "Please meet me at the station tomorrow morning."
private const val HINGLISH_SENTENCE = "Kal office jaana hai, please call me at five."
