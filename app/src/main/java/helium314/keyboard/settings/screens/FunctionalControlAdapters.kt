// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.dialogs.ListPickerDialog
import helium314.keyboard.settings.dialogs.SliderDialog
import helium314.keyboard.settings.preferences.Preference

/** Only settings UI state. Existing per-app and DND engine checks remain authoritative. */
@Composable
internal fun FeedbackIntensityControls() {
    val context = LocalContext.current
    val prefs = context.prefs()
    val revision by (context.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val vibration = remember(revision) { prefs.getInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, Defaults.PREF_VIBRATION_DURATION_SETTINGS) }
    val volume = remember(revision) { prefs.getFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, Defaults.PREF_KEYPRESS_SOUND_VOLUME) }
    val lastVibration = remember(revision) { prefs.getInt("brr_settings_last_vibration_ms", 0) }
    val lastSound = remember(revision) { prefs.getFloat("brr_settings_last_sound_volume", 0f) }
    val hapticOn = remember(revision) { prefs.getBoolean(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON) }
    val soundOn = remember(revision) { prefs.getBoolean(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON) }
    if (AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
        IntensityControl("Vibration", hapticOn, vibration.toFloat(), 100f, { "${it.toInt()} ms" }, remembered = lastVibration.toFloat(),
            onOff = { prefs.edit().putBoolean(Settings.PREF_VIBRATE_ON, false).apply() },
            onSystem = {
                val e = prefs.edit().putBoolean(Settings.PREF_VIBRATE_ON, true)
                    .putInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, -1)
                if (vibration > 0) e.putInt("brr_settings_last_vibration_ms", vibration)
                e.apply()
            }, onCustom = { value ->
                val ms = value.toInt()
                val e = prefs.edit().putBoolean(Settings.PREF_VIBRATE_ON, ms > 0)
                if (ms > 0) e.putInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, ms).putInt("brr_settings_last_vibration_ms", ms)
                e.apply()
            })
    }
    IntensityControl("Keypress sound", soundOn, volume * 100f, 100f, { "${it.toInt()}%" }, remembered = lastSound * 100f,
        onOff = { prefs.edit().putBoolean(Settings.PREF_SOUND_ON, false).apply() },
        onSystem = {
            val e = prefs.edit().putBoolean(Settings.PREF_SOUND_ON, true)
                .putFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, -0.01f)
            if (volume > 0f) e.putFloat("brr_settings_last_sound_volume", volume)
            e.apply()
        }, onCustom = { value ->
            val e = prefs.edit().putBoolean(Settings.PREF_SOUND_ON, value > 0)
            if (value > 0) e.putFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, value / 100f).putFloat("brr_settings_last_sound_volume", value / 100f)
            e.apply()
        })
}

@Composable
private fun IntensityControl(name: String, enabled: Boolean, value: Float, max: Float, format: (Float) -> String, remembered: Float,
    onOff: () -> Unit, onSystem: () -> Unit, onCustom: (Float) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    val current = if (!enabled || value == 0f) "Off" else if (value < 0f) "System default" else "Custom"
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Preference(name = name, description = if (current == "Custom") format(value) else current, onClick = { picking = true })
    }
    if (picking) ListPickerDialog(onDismissRequest = { picking = false }, items = listOf("Off", "System default", "Custom"),
        selectedItem = current, title = { Text(name) }, onItemSelected = { mode ->
            when (mode) { "Off" -> onOff(); "System default" -> onSystem(); else -> custom = true }
        })
    if (custom) SliderDialog(onDismissRequest = { custom = false }, onDone = onCustom,
        title = { Text(name) }, initialValue = (if (value > 0f) value else remembered).let { if (it.isFinite()) it.coerceIn(0f, max) else 0f }, range = 0f..max,
        positionString = { if (it.toInt() == 0) "Off" else format(it) }, intermediateSteps = max.toInt() - 1)
}

@Composable
internal fun ClipboardRetentionPresets() {
    val context = LocalContext.current
    val prefs = context.prefs()
    val revision by (context.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val current = remember(revision) { prefs.getInt(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, Defaults.PREF_CLIPBOARD_HISTORY_RETENTION_TIME) }
    val choices = listOf(5, 10, 30, 60, 120, 121)
    var open by remember { mutableStateOf(false) }
    fun title(value: Int) = if (value > 120) "No limit" else "$value minutes"
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Preference(name = "Retention preset", description = if (current in choices) title(current) else "Custom · ${title(current)}", onClick = { open = true })
    }
    if (open) ListPickerDialog(onDismissRequest = { open = false }, items = choices, selectedItem = current.takeIf { it in choices },
        title = { Text("Clipboard retention · older unpinned clips may be removed") }, getItemName = { title(it) },
        confirmImmediately = false, onItemSelected = {
            if (it != current) {
                prefs.edit().putInt(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME, it).apply()
                ClipboardDao.getInstance(context)?.clearOldClips(true)
            }
        })
}

/** Actual IME test, no extra recording path; sample text is RAM-only and learning is disabled. */
@Composable
internal fun VoiceTestField() {
    val context = LocalContext.current
    val imm = remember(context) { context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    var text by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(UncachedInputMethodManagerUtils.isThisImeCurrent(context, imm)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) text = ""
            if (event == Lifecycle.Event.ON_RESUME) current = UncachedInputMethodManagerUtils.isThisImeCurrent(context, imm)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Text("Test dictation", style = MaterialTheme.typography.titleMedium)
    Text("Tap the microphone on brrBoard and speak a sample. This field is not saved.",
        style = MaterialTheme.typography.bodySmall)
    if (current) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Voice sample") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                platformImeOptions = PlatformImeOptions(privateImeOptions = "${context.packageName}.${Constants.ImeOption.NO_PERSONALIZED_LEARNING}")))
        TextButton(onClick = { text = "" }) { Text("Clear sample") }
    } else Text("Choose brrBoard as the active keyboard before testing.")
}
