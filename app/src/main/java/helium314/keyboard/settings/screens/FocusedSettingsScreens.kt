// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import helium314.keyboard.settings.preferences.SettingsSwitch
import android.content.Context
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import helium314.keyboard.settings.preferences.SettingsChoiceMark
import helium314.keyboard.settings.SettingsVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceModelChoice
import helium314.keyboard.latin.voice.UltraVoiceModelManifest
import helium314.keyboard.latin.voice.VoiceModelPack
import helium314.keyboard.latin.voice.VoiceNoiseChoice
import helium314.keyboard.latin.voice.VoiceGesturePreferences
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsWithoutKey
import helium314.keyboard.settings.preferences.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** These are focused views of the SAME existing Setting objects and preference keys.
 *  No migration, duplicate store, or change to how the typing engine reads a preference.
 */
@Composable
fun KeysLayoutScreen(onClickBack: () -> Unit, section: String = "legacy") {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val change by (ctx.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val subtypes = remember(change) { SubtypeSettings.getEnabledSubtypes(true) }
    val languagesWithLocalNumbers = setOf("ar", "bn", "fa", "gu", "hi", "kn", "mr", "ne", "ur")
    val items = listOf(
        SettingsWithoutKey.SECONDARY_LAYOUTS_PAGE,
        R.string.settings_category_additional_keys,
        Settings.PREF_SHOW_NUMBER_ROW,
        if (subtypes.any { it.locale().language in languagesWithLocalNumbers }) Settings.PREF_LOCALIZED_NUMBER_ROW else null,
        if (prefs.getBoolean(Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS) && prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) Settings.PREF_SHOW_NUMBER_ROW_HINTS else null,
        if (!prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS else null,
        Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY,
        Settings.PREF_LANGUAGE_SWITCH_KEY,
        Settings.PREF_SHOW_EMOJI_KEY,
        if (helium314.keyboard.latin.BuildConfig.COMPATIBILITY_EDITION) Settings.PREF_COMPAT_COMMA_PERIOD else null,
        Settings.PREF_SHOW_HINTS,
        if (prefs.getBoolean(Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS)) Settings.PREF_POPUP_KEYS_HINT_ORDER else null,
        Settings.PREF_POPUP_KEYS_ORDER,
        Settings.PREF_SHOW_POPUP_HINTS,
        Settings.PREF_SHOW_TLD_POPUP_KEYS,
        Settings.PREF_POPUP_ON,
        Settings.PREF_REMOVE_REDUNDANT_POPUPS,
        R.string.brr_category_advanced_interactions,
        Settings.PREF_SAVE_SUBTYPE_PER_APP,
        Settings.PREF_SHOW_EMOJI_DESCRIPTIONS,
        Settings.PREF_KEY_LONGPRESS_TIMEOUT,
        Settings.PREF_SPACE_HORIZONTAL_SWIPE,
        Settings.PREF_SPACE_VERTICAL_SWIPE,
        if (Settings.readHorizontalSpaceSwipe(prefs) == KeyboardActionListener.SwipeAction.SWITCH_LANGUAGE || Settings.readVerticalSpaceSwipe(prefs) == KeyboardActionListener.SwipeAction.SWITCH_LANGUAGE) Settings.PREF_LANGUAGE_SWIPE_DISTANCE else null,
        if (Settings.readVerticalSpaceSwipe(prefs) == KeyboardActionListener.SwipeAction.TOUCHPAD_MODE) Settings.PREF_TOUCHPAD_SENSITIVITY else null,
        if (Settings.readVerticalSpaceSwipe(prefs) == KeyboardActionListener.SwipeAction.TOUCHPAD_MODE) Settings.PREF_TOUCHPAD_EDGE_SCROLL else null,
        Settings.PREF_DELETE_SWIPE,
        Settings.PREF_SPACE_TO_CHANGE_LANG,
        Settings.PREF_ABC_AFTER_SYMBOL_SPACE,
        Settings.PREF_ABC_AFTER_NUMPAD_SPACE,
        Settings.PREF_ABC_AFTER_EMOJI,
        Settings.PREF_ABC_AFTER_CLIP,
        Settings.PREF_CUSTOM_CURRENCY_KEY,
        Settings.PREF_MORE_POPUP_KEYS,
    )
    SearchSettingsScreen(onClickBack, if (section == "gestures") "Gesture assignments" else "Keys & symbols",
        if (section == "legacy") items else items.filter { item -> item != null && item !is Int && helium314.keyboard.settings.WorkspaceSettings.keysSection(item.toString()) == section },
        footer = { if (section == "gestures") EditingGestureControls() })
}

@Composable
fun SoundFeedbackScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val change by (ctx.getActivity() as SettingsActivity).prefChanged.collectAsState()
    // Read change so dependent options appear/disappear as a parent toggle changes.
    val vibrationEnabled = remember(change) { prefs.getBoolean(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON) }
    SearchSettingsScreen(onClickBack, "Sound & haptics", listOf(
        if (vibrationEnabled && AudioAndHapticFeedbackManager.getInstance().hasVibrator()) Settings.PREF_IMPORTANT_HAPTICS_IN_DND else null,
    ), header = { FeedbackIntensityControls() })
}

@Composable
fun ClipboardSettingsScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val change by (ctx.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val owner = LocalLifecycleOwner.current
    var folderAccess by remember { mutableStateOf(hasScreenshotFolderAccess(ctx)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                folderAccess = hasScreenshotFolderAccess(ctx)
                if (!folderAccess &&
                    prefs.getBoolean(Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD, false)) {
                    prefs.edit().putBoolean(Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD, false).apply()
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val historyOn = remember(change) { prefs.getBoolean(Settings.PREF_ENABLE_CLIPBOARD_HISTORY, Defaults.PREF_ENABLE_CLIPBOARD_HISTORY) }
    val screenshotsOn = remember(change, folderAccess) {
        prefs.getBoolean(Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD, Defaults.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD) && folderAccess
    }
    SearchSettingsScreen(onClickBack, "Clipboard", listOf(
        R.string.settings_category_clipboard_history,
        "smart_snippets_page",
        Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD,
        if (screenshotsOn) Settings.PREF_CHANGE_SCREENSHOTS_FOLDER else null,
        if (historyOn) Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME else null,
        if (historyOn) Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST else null,
        Settings.PREF_SUGGEST_CLIPBOARD_CONTENT,
        Settings.PREF_ABC_AFTER_CLIP,
    ), header = { if (historyOn) ClipboardRetentionPresets() }, footer = {
        Column(Modifier.padding(horizontal = 16.dp)) { ClipboardTextVaultControls() }
    })
}

private fun hasScreenshotFolderAccess(context: Context): Boolean {
    val value = context.prefs().getString(ClipboardHistoryManager.PREF_SCREENSHOTS_FOLDER_URI, null) ?: return false
    return try {
        val uri = Uri.parse(value)
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    } catch (_: Exception) { false }
}

@Composable
fun PrivacySettingsScreen(onClickBack: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    val change by (LocalContext.current.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val learning = remember(change) { prefs.getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, Defaults.PREF_KEY_USE_PERSONALIZED_DICTS) }
    SearchSettingsScreen(onClickBack, "Privacy & app profiles", listOf(
        Settings.PREF_ALWAYS_INCOGNITO_MODE,
        Settings.PREF_SECURE_APPS,
        Settings.PREF_KEY_USE_PERSONALIZED_DICTS,
        Settings.PREF_USE_CONTACTS,
        Settings.PREF_USE_APPS,
        Settings.PREF_SAVE_SUBTYPE_PER_APP,
        if (learning) Settings.PREF_ADD_TO_PERSONAL_DICTIONARY else null,
        SettingsWithoutKey.CLEAR_LEARNED_TYPING_HISTORY,
    ), footer = { Text("Per-app override details remain in the protected BR Brain dashboard.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) })
}

@Composable
fun BackupSettingsScreen(onClickBack: () -> Unit) = SearchSettingsScreen(
    onClickBack, "Backup & restore", listOf(SettingsWithoutKey.BACKUP_RESTORE),
)

@Composable
internal fun ResetOnlyScreen(onClickBack: () -> Unit) = SearchSettingsScreen(
    onClickBack, "Reset settings", emptyList(), footer = { ResetSettingsControl() },
)

@Composable
internal fun TextToolsScreen(onClickBack: () -> Unit) = SearchSettingsScreen(
    onClickBack, "Text tools", listOf(Settings.PREF_TIMESTAMP_FORMAT, Settings.PREF_URL_DETECTION),
    footer = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextTemplateControls()
            TextReplacementControls()
        }
    },
)

@Composable
fun VoiceSettingsScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    val installed = remember(refresh) { VoiceModelChoice.installed(ctx) }
    val selected = remember(refresh) { VoiceModelChoice.active(ctx) }
    var cleaningEnabled by remember(refresh) { mutableStateOf(VoiceNoiseChoice.isCleaningEnabled(ctx)) }
    var adaptiveBatching by remember(refresh) { mutableStateOf(helium314.keyboard.latin.voice.VoiceEndpointPreference.enabled(ctx)) }
    val voicePrefs = ctx.prefs()
    var micDoubleTapSwitch by remember(refresh) { mutableStateOf(VoiceGesturePreferences.isMicDoubleTapSwitchEnabled(ctx)) }
    var holdToTalk by remember(refresh) { mutableStateOf(VoiceGesturePreferences.isHoldToTalkEnabled(ctx)) }
    var holdAutoSend by remember(refresh) { mutableStateOf(VoiceGesturePreferences.isHoldAutoSendEnabled(ctx)) }
    val ultraBundled = remember(refresh) { VoiceModelInstaller.hasBundled(ctx, UltraVoiceModelManifest.pack) }
    val scope = rememberCoroutineScope()
    var installingModel by remember { mutableStateOf<String?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SearchSettingsScreen(onClickBack, "Voice", emptyList()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("On-device dictation", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(if (ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) "Microphone permission granted" else "Microphone permission needed when recording", style = MaterialTheme.typography.bodySmall)
                    Text(selected?.let { "Keyboard model: ${modelTitle(it)}" }
                        ?: "No verified offline model is installed",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Ultra is preferred. A legacy installed model remains usable until Ultra is available; after successful Ultra recognition unused 110M files are removed. No model is downloaded by the keyboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Adaptive speech batching", style = MaterialTheme.typography.titleMedium)
                        Text("Silero VAD + Smart Turn: prioritize completed thoughts, with a 12-second safety limit. Shorter batches under pressure; the microphone stays active. Off restores six-second batching.", style = MaterialTheme.typography.bodySmall)
                    }
                    SettingsSwitch(checked = adaptiveBatching, onCheckedChange = {
                        adaptiveBatching = it
                        helium314.keyboard.latin.voice.VoiceEndpointPreference.setEnabled(ctx, it)
                    })
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Double-tap mic to switch keyboards", style = MaterialTheme.typography.titleMedium)
                        Text("Off by default for immediate tap-to-record. Enabling it restores the double-tap shortcut and its single-tap confirmation delay.", style = MaterialTheme.typography.bodySmall)
                    }
                    SettingsSwitch(checked = micDoubleTapSwitch, onCheckedChange = {
                        micDoubleTapSwitch = it
                        voicePrefs.edit().putBoolean(VoiceGesturePreferences.MIC_DOUBLE_TAP_SWITCH, it).apply()
                    })
                }
            }
            val startupMs = helium314.keyboard.latin.voice.VoiceDenoiseMetrics.lastMicrophoneStartupMs()
            if (startupMs >= 0) {
                Text("Last voice request to first PCM: ${startupMs} ms (RAM-only timing)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("Offline models", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            if (installed.isEmpty()) {
                Text("Voice models stay in private app storage. Lightweight updates do not bundle model weights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                installed.filter { it.id == UltraVoiceModelManifest.ID }.forEach { pack ->
                    VoiceModelOption(
                        title = modelTitle(pack),
                        summary = if (pack.id == UltraVoiceModelManifest.ID)
                            "Default · highest-capacity · more memory and slower first load"
                        else "Compact · lower memory and quicker first load",
                        selected = selected?.id == pack.id,
                        onClick = {
                            VoiceModelChoice.select(ctx, pack)
                            refresh++
                        },
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Voice cleaning · brrClean", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Reduces background noise before transcription. On by default; failed cleaning safely falls back to the original audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SettingsSwitch(checked = cleaningEnabled, onCheckedChange = {
                        cleaningEnabled = it
                        VoiceNoiseChoice.setCleaningEnabled(ctx, it)
                    })
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hold mic to talk", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Hold to record and release to finish. This gesture is disabled in Incognito.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SettingsSwitch(checked = holdToTalk, onCheckedChange = {
                        holdToTalk = it
                        voicePrefs.edit().putBoolean(VoiceGesturePreferences.HOLD_TO_TALK, it).apply()
                    })
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Send after hold-to-talk", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("On by default. Sends only in fields marked for Send; other fields receive text only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SettingsSwitch(checked = holdAutoSend, onCheckedChange = {
                        holdAutoSend = it
                        voicePrefs.edit().putBoolean(VoiceGesturePreferences.AUTO_SEND_HOLD, it).apply()
                    })
                }
            }

            SettingsActivity.settingsContainer[SettingsWithoutKey.INLINE_VOICE]?.Preference()
            SettingsActivity.settingsContainer[SettingsWithoutKey.VOICE_SERVICE]?.Preference()
            VoiceTestField()
            Text("Voice recovery", style = MaterialTheme.typography.titleMedium)
            Text("Last dictation is kept in RAM for 60 seconds; never saved to clipboard storage.", style = MaterialTheme.typography.bodySmall)
            installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (ultraBundled && UltraVoiceModelManifest.pack !in installed) {
                Button(onClick = {
                    val pack = UltraVoiceModelManifest.pack
                    if (installingModel != null) return@Button
                    installingModel = pack.id
                    installMessage = "Verifying and installing ${modelTitle(pack)}… keep brrBoard open."
                    scope.launch(Dispatchers.IO) {
                        val success = VoiceModelInstaller.installBundledIfPresent(ctx, pack)
                        withContext(Dispatchers.Main) {
                            installingModel = null
                            installMessage = if (success) "${modelTitle(pack)} is ready."
                                else "Model install failed or its assets are not in this update."
                            refresh++
                        }
                    }
                }, enabled = installingModel == null) {
                    Text(if (installingModel == null) "Install available model" else "Installing model…")
                }
            }

        }
    }
}

@Composable
private fun VoiceModelOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(SettingsVisuals.groupRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SettingsChoiceMark(selected)
        }
    }
}

private fun modelTitle(pack: VoiceModelPack): String =
    if (pack.id == UltraVoiceModelManifest.ID) "Parakeet Ultra 0.6B" else "Parakeet 110M"
