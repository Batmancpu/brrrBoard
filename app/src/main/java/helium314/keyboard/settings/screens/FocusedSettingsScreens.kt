// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
fun KeysLayoutScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val change by (ctx.getActivity() as SettingsActivity).prefChanged.collectAsState()
    val subtypes = remember(change) { SubtypeSettings.getEnabledSubtypes(true) }
    val languagesWithLocalNumbers = setOf("ar", "bn", "fa", "gu", "hi", "kn", "mr", "ne", "ur")
    SearchSettingsScreen(onClickBack, "Keys & layout", listOf(
        SettingsWithoutKey.SECONDARY_LAYOUTS_PAGE,
        R.string.settings_category_additional_keys,
        Settings.PREF_SHOW_NUMBER_ROW,
        if (subtypes.any { it.locale().language in languagesWithLocalNumbers }) Settings.PREF_LOCALIZED_NUMBER_ROW else null,
        if (prefs.getBoolean(Settings.PREF_SHOW_HINTS, Defaults.PREF_SHOW_HINTS) && prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) Settings.PREF_SHOW_NUMBER_ROW_HINTS else null,
        if (!prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, Defaults.PREF_SHOW_NUMBER_ROW)) Settings.PREF_SHOW_NUMBER_ROW_IN_SYMBOLS else null,
        Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY,
        Settings.PREF_LANGUAGE_SWITCH_KEY,
        Settings.PREF_SHOW_EMOJI_KEY,
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
    ))
}

@Composable
fun SoundFeedbackScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val change by (ctx.getActivity() as SettingsActivity).prefChanged.collectAsState()
    // Read change so dependent options appear/disappear as a parent toggle changes.
    val vibrationEnabled = remember(change) { prefs.getBoolean(Settings.PREF_VIBRATE_ON, Defaults.PREF_VIBRATE_ON) }
    val soundEnabled = remember(change) { prefs.getBoolean(Settings.PREF_SOUND_ON, Defaults.PREF_SOUND_ON) }
    SearchSettingsScreen(onClickBack, "Sound & haptics", listOf(
        R.string.settings_category_input,
        if (AudioAndHapticFeedbackManager.getInstance().hasVibrator()) Settings.PREF_VIBRATE_ON else null,
        if (vibrationEnabled) Settings.PREF_VIBRATION_DURATION_SETTINGS else null,
        if (vibrationEnabled) Settings.PREF_VIBRATE_IN_DND_MODE else null,
        Settings.PREF_SOUND_ON,
        if (soundEnabled) Settings.PREF_KEYPRESS_SOUND_VOLUME else null,
    ))
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
        Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD,
        if (screenshotsOn) Settings.PREF_CHANGE_SCREENSHOTS_FOLDER else null,
        if (historyOn) Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME else null,
        if (historyOn) Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST else null,
        Settings.PREF_SUGGEST_CLIPBOARD_CONTENT,
    ))
}

private fun hasScreenshotFolderAccess(context: Context): Boolean {
    val value = context.prefs().getString(ClipboardHistoryManager.PREF_SCREENSHOTS_FOLDER_URI, null) ?: return false
    return try {
        val uri = Uri.parse(value)
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    } catch (_: Exception) { false }
}

@Composable
fun PrivacySettingsScreen(onClickBack: () -> Unit) = SearchSettingsScreen(
    onClickBack, "Privacy & protected apps", listOf(
        Settings.PREF_ALWAYS_INCOGNITO_MODE,
        Settings.PREF_SECURE_APPS,
        Settings.PREF_KEY_USE_PERSONALIZED_DICTS,
        Settings.PREF_USE_CONTACTS,
        Settings.PREF_USE_APPS,
    )
)

@Composable
fun BackupSettingsScreen(onClickBack: () -> Unit) = SearchSettingsScreen(
    onClickBack, "Backup & restore", listOf(SettingsWithoutKey.BACKUP_RESTORE)
)

@Composable
fun VoiceSettingsScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    var ready by remember { mutableStateOf(VoiceModelInstaller.isInstalled(ctx)) }
    var status by remember { mutableStateOf(VoiceModelInstaller.installState) }
    val scope = rememberCoroutineScope()
    SearchSettingsScreen(onClickBack, "Offline voice", emptyList()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(if (ready) "English model ready" else "English model not installed",
                    style = MaterialTheme.typography.titleMedium)
                Text(if (ready) "Speech is processed on-device. No network connection is required."
                    else "The lightweight APK does not download or include a speech model. A verified model from a previous installation is retained.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (status == VoiceModelInstaller.InstallState.FAILED) {
            Md3ePreferenceGroup("Model recovery") {
                Md3ePreference("refresh", "Retry bundled model verification", "Only checks assets already in this APK; nothing is downloaded.", onClick = {
                    scope.launch(Dispatchers.IO) {
                        val installed = VoiceModelInstaller.installBundledIfPresent(ctx)
                        withContext(Dispatchers.Main) { ready = installed }
                        withContext(Dispatchers.Main) { status = VoiceModelInstaller.installState }
                    }
                }, isFirst = true, isLast = true)
            }
        }
        Text(
            "Microphone permission is requested when you start dictation, not while browsing settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
        )
    }
}
