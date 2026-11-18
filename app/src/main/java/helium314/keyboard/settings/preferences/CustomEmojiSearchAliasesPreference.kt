// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.keyboard.emoji.EmojiDescribeCustomAliases
import helium314.keyboard.latin.R
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.InfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Optional private description→Unicode Emoji TSV import; never sees editor text. */
@Composable
fun CustomEmojiSearchAliasesPreference(setting: Setting) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showChoices by rememberSaveable { mutableStateOf(false) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var importedCount by rememberSaveable { mutableStateOf<Int?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || busy) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw java.io.IOException("Unable to open selected file")
                    stream.use { EmojiDescribeCustomAliases.importFrom(context, it) }
                }
            }
            busy = false
            result.onSuccess { importedCount = it }
                .onFailure { showError = true }
        }
    }
    Preference(
        name = setting.title,
        description = setting.description,
        onClick = {
            if (!busy) {
                if (EmojiDescribeCustomAliases.getFile(context).exists()) showChoices = true
                else launcher.launch(arrayOf("*/*"))
            }
        }
    )
    if (showChoices) {
        ConfirmationDialog(
            onDismissRequest = { showChoices = false },
            onConfirmed = {
                showChoices = false
                launcher.launch(arrayOf("*/*"))
            },
            onNeutral = {
                showChoices = false
                scope.launch {
                    busy = true
                    withContext(Dispatchers.IO) { EmojiDescribeCustomAliases.clear(context) }
                    busy = false
                }
            },
            neutralButtonText = stringResource(R.string.delete),
            confirmButtonText = stringResource(R.string.load),
            title = { Text(setting.title) },
            content = { Text(stringResource(R.string.emoji_describe_shortcuts_remove_hint)) }
        )
    }
    if (showError) InfoDialog(stringResource(R.string.emoji_describe_shortcuts_error)) {
        showError = false
    }
    if (importedCount != null) {
        InfoDialog(stringResource(R.string.emoji_describe_shortcuts_imported, importedCount!!)) {
            importedCount = null
        }
    }
}
