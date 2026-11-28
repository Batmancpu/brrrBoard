// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.emoji.SupportedEmojis
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.FileUtils
import helium314.keyboard.latin.utils.DeviceProtectedUtils
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.InfoDialog
import java.io.File
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import android.system.Os
import java.io.IOException

private fun isEmojiFontFile(file: File): Boolean {
    return runCatching {
        java.io.FileInputStream(file).use { fis ->
            val header = ByteArray(12)
            if (fis.read(header) != 12) return false
            val numTables = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
            if (numTables <= 0 || numTables > 120) return false
            
            val recordBuffer = ByteArray(16)
            for (i in 0 until numTables) {
                if (fis.read(recordBuffer) != 16) return false
                val tag = String(recordBuffer, 0, 4, Charsets.US_ASCII)
                if (tag == "CBDT" || tag == "CBLC" || tag == "sbix" || tag == "COLR" || tag == "CPAL" || tag == "SVG ") {
                    return true
                }
            }
            false
        }
    }.getOrDefault(false)
}

@Composable
fun CustomFontPreference(setting: Setting, fontFile: File, title: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var warningPath: String? by rememberSaveable { mutableStateOf(null) }
    var busy by remember { mutableStateOf(false) }
    fun refresh() {
        KeyboardTypeface.clearCache()
        SupportedEmojis.load(ctx)
        KeyboardSwitcher.getInstance().reloadKeyboard()
    }
    fun install(staged: File) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) { Os.rename(staged.absolutePath, fontFile.absolutePath) }
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { showErrorDialog = true }
            finally { staged.delete(); busy = false }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK || busy) return@rememberLauncherForActivityResult
        val uri = it.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            var staged: File? = null
            var retained = false
            try {
                val parent = fontFile.parentFile ?: throw IOException("Font storage unavailable")
                val temp = File.createTempFile("font-import-", ".tmp", parent)
                staged = temp
                val emoji = withContext(Dispatchers.IO) {
                    val stream = ctx.contentResolver.openInputStream(uri) ?: throw IOException("Empty font")
                    stream.use { input -> temp.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > 128L * 1024 * 1024) throw IOException("Font too large")
                            output.write(buffer, 0, count)
                        }
                        if (total == 0L) throw IOException("Empty font")
                        output.fd.sync()
                    } }
                    Typeface.createFromFile(temp)
                    isEmojiFontFile(temp)
                }
                val mismatch = (fontFile.name == "custom_font" && emoji) || (fontFile.name == "custom_emoji_font" && !emoji)
                if (mismatch) {
                    warningPath = temp.absolutePath
                    retained = true
                } else {
                    withContext(Dispatchers.IO) { Os.rename(temp.absolutePath, fontFile.absolutePath) }
                    refresh()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { showErrorDialog = true }
            finally { if (!retained) staged?.delete(); busy = false }
        }
    }
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
    Preference(name = setting.title,
        description = if (busy) "Validating file…" else if (fontFile.isFile) "Installed · replace or remove" else "Not installed · import",
        onClick = {
            if (!busy && warningPath == null) {
                if (fontFile.exists()) showDialog = true else launcher.launch(intent)
            }
        })
    if (showDialog)
        ConfirmationDialog(
            onDismissRequest = { showDialog = false },
            onConfirmed = { showDialog = false; launcher.launch(intent) },
            onNeutral = {
                showDialog = false
                if (!fontFile.exists() || fontFile.delete()) refresh() else showErrorDialog = true
            },
            neutralButtonText = stringResource(R.string.delete),
            confirmButtonText = stringResource(R.string.load),
            title = { Text(stringResource(title)) }
        )
    if (showErrorDialog) InfoDialog(stringResource(R.string.file_read_error)) { showErrorDialog = false }
    warningPath?.let { path ->
        val temp = File(path)
        val warningStringRes = if (fontFile.name == "custom_emoji_font") R.string.custom_emoji_font_safeguard_warning
            else R.string.custom_font_safeguard_warning
        ConfirmationDialog(
            onDismissRequest = { warningPath = null; temp.delete() },
            onConfirmed = { warningPath = null; install(temp) },
            title = { Text(stringResource(title)) },
            content = { Text(stringResource(warningStringRes)) }
        )
    }
}
