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
import androidx.compose.material3.Switch
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
import helium314.keyboard.latin.voice.VoiceCaptureNoiseReduction
import helium314.keyboard.latin.voice.VoiceDenoiseMetrics
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceNoiseLab
import helium314.keyboard.settings.SearchSettingsScreen
import java.util.Locale
import kotlinx.coroutines.delay

/** BR Brain's user-started microphone comparison lab; nothing records on page entry. */
@Composable
fun VoiceNoiseLabScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    val lab = remember(context) { VoiceNoiseLab(context) }
    DisposableEffect(lab) { onDispose { lab.close() } }
    var refresh by remember { mutableIntStateOf(0) }
    var suppressionDefault by remember(context) {
        mutableStateOf(VoiceCaptureNoiseReduction.enabled(context))
    }
    var hinglish by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf<VoiceNoiseLab.Key?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) permissionPending?.let(lab::record)
        permissionPending = null
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300L)
            refresh++
        }
    }
    val state = remember(refresh) { lab.snapshot() }
    val liveVoice = remember(refresh) { VoiceDenoiseMetrics.snapshot() }
    val reference = if (hinglish) HINGLISH_SENTENCE else ENGLISH_SENTENCE
    val busy = state.recording || state.playing || state.decoding
    val palette = MaterialTheme.colorScheme

    SearchSettingsScreen(onClickBack = onClickBack, title = "Voice & Noise Reduction",
        settings = emptyList()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = palette.primaryContainer)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live blue-mic status", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(if (liveVoice.blueMicCapturing) "● Capturing offline microphone audio now"
                        else "○ Blue microphone is not recording", fontWeight = FontWeight.Medium)
                    Text((if (liveVoice.blueMicCapturing) "Android suppression LIVE: "
                          else "Android suppression from last blue capture: ") +
                        liveVoice.platformNoiseSuppression)
                    Text("Completed blue captures this process: ${liveVoice.completedBlueCaptures}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("This is genuine capture state, not a background microphone monitor. ACTIVE means Android accepted the effect; it does not measure how much noise was removed.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default blue-mic noise suppression", fontWeight = FontWeight.SemiBold)
                            Text("ON by default. The normal voice engine keeps using Android's effect if supported.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = suppressionDefault, onCheckedChange = { value ->
                            suppressionDefault = value
                            VoiceCaptureNoiseReduction.setEnabled(context, value)
                        })
                    }
                    Text("Current recognizer: Parakeet 110M INT8, offline. " +
                        (if (VoiceModelInstaller.isInstalled(context)) "Model available."
                         else "Install the voice model before scoring speech."),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Native GTCRN noise model: disabled after the prior load failure. Earbud ENx processing is built into its firmware and cannot be independently switched or measured here.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Listen and compare", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Choose a microphone and suppression mode below. Read the same sentence for each take, tap Stop, then Play to hear exactly what that take captured. Tests last up to 12 seconds and stay only in memory until you leave or erase them.",
                style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { hinglish = false }, enabled = !busy) {
                    Text(if (!hinglish) "✓ English" else "English")
                }
                OutlinedButton(onClick = { hinglish = true }, enabled = !busy) {
                    Text(if (hinglish) "✓ Hinglish" else "Hinglish")
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = palette.secondaryContainer)) {
                Text("Read aloud: “$reference”", modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold)
            }
            Text("Session: ${state.status}", style = MaterialTheme.typography.bodyMedium)
            if (state.recording) {
                Text("Live input level: ${state.levelPercent}% of full-scale RMS (not a noise-quality score)",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = lab::stopRecording, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop & keep this take privately")
                }
            }
            for (input in VoiceNoiseLab.Input.entries) {
                Text(input.title, style = MaterialTheme.typography.titleMedium)
                if (input == VoiceNoiseLab.Input.EARBUDS) {
                    Text("Connect your Nirvana Ion first. If Android exposes no headset microphone, the request falls back to Android auto and the route is marked unverified.",
                        style = MaterialTheme.typography.bodySmall)
                }
                for (suppression in listOf(false, true)) {
                    val key = VoiceNoiseLab.Key(input, suppression)
                    val take = state.takes.firstOrNull { it.key == key }
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (suppression) "Android suppression requested ON" else "No app-added suppression",
                                fontWeight = FontWeight.SemiBold)
                            if (take != null) {
                                Text("${take.source} · ${take.suppressionStatus}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("${take.durationMs / 1000.0} s · signal ${take.signalLevelPercent}% · clipping " +
                                    String.format(Locale.US, "%.2f%%", take.clippingPercent),
                                    style = MaterialTheme.typography.bodySmall)
                                if (take.transcript != null) {
                                    Text("Parakeet heard: ${take.transcript}")
                                    Text("Scored against: “${take.scoredReference}”",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("Word error: ${take.wordErrorPercent ?: "—"}% " +
                                        "· recognition ${take.decodeMs ?: "—"} ms",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("No take yet", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                        == PackageManager.PERMISSION_GRANTED) lab.record(key)
                                    else {
                                        permissionPending = key
                                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }, enabled = !busy) { Text(if (take == null) "Record" else "Retake") }
                                OutlinedButton(onClick = { lab.play(key) }, enabled = take != null && !busy) {
                                    Text("Play")
                                }
                                OutlinedButton(onClick = { lab.transcribe(key, reference) },
                                    enabled = take != null && !busy) { Text("Score") }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = lab::clear, enabled = !busy) {
                Text("Erase all lab samples & results")
            }
            Text("Important: these are separate takes, not the same recording with a filter applied afterward. Android and earbud firmware can perform additional processing even when our extra NoiseSuppressor is OFF. Phone and Bluetooth microphones cannot be combined as separate raw channels on this standard capture path. Playback follows Android's current output selection; no automatic Bluetooth pairing/disconnection occurs.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Privacy: no audio, transcript, application name or microphone identifier is saved to storage, copied to the clipboard, uploaded, or recorded in the crash trail. Leave this screen to discard the test. No new microphone runs while you type or take a call.",
                color = palette.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val ENGLISH_SENTENCE = "Please meet me at the station tomorrow morning."
private const val HINGLISH_SENTENCE = "Kal office jaana hai, please call me at five."
