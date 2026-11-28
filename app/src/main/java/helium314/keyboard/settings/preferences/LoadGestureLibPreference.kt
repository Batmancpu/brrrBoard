// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ChecksumCalculator
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.latin.utils.DeviceProtectedUtils
import helium314.keyboard.latin.utils.protectedPrefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.filePicker
import java.io.File
import androidx.core.content.edit

@SuppressLint("ApplySharedPref")
@Composable
fun LoadGestureLibPreference(setting: Setting) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val prefs = ctx.protectedPrefs()
    val abi = Build.SUPPORTED_ABIS[0]
    val libFile = File(DeviceProtectedUtils.getFilesDir(ctx), JniUtils.JNI_LIB_IMPORT_FILE_NAME)
    val importScope = rememberCoroutineScope()
    fun renameToLibFileAndRestart(file: File, checksum: String) {
        importScope.launch {
        try {
            withContext(Dispatchers.IO) { installGestureLibrary(ctx, file, checksum) }
            Runtime.getRuntime().exit(0)
        } catch (cancelled: CancellationException) { file.delete(); throw cancelled }
        catch (_: Exception) {
            file.delete()
            Toast.makeText(ctx, R.string.gesture_import_failed, Toast.LENGTH_LONG).show()
        }
        }
    }
    var tempFilePath: String? by rememberSaveable { mutableStateOf(null) }
    val launcher = filePicker { uri ->
        importScope.launch {
        try {
            val (staged, checksum) = withContext(Dispatchers.IO) {
                val file = stageGestureLibrary(ctx, uri)
                file to ChecksumCalculator.checksum(file).orEmpty()
            }
            if (checksum == JniUtils.expectedDefaultChecksum()) {
                renameToLibFileAndRestart(staged, checksum)
            } else {
                tempFilePath = staged.absolutePath
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            Toast.makeText(ctx, R.string.gesture_import_failed, Toast.LENGTH_LONG).show()
        }
        }
    }
    Preference(
        name = setting.title,
        description = if (libFile.isFile) (if (JniUtils.sHaveGestureLib) "Imported · decoder available" else "Imported · decoder not active") else "Import a compatible swipe typing library",
        onClick = { showDialog = true }
    )
    if (showDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDialog = false },
            onConfirmed = {
                showDialog = false
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                launcher.launch(intent)
            },
            confirmButtonText = stringResource(R.string.load_gesture_library_button_load),
            title = { Text(stringResource(R.string.load_gesture_library)) },
            content = { Text(stringResource(R.string.load_gesture_library_message, abi)) },
            neutralButtonText = if (libFile.exists()) stringResource(R.string.load_gesture_library_button_delete) else null,
            onNeutral = {
                if (libFile.exists() && !libFile.delete()) {
                    Toast.makeText(ctx, R.string.gesture_import_failed, Toast.LENGTH_LONG).show()
                    return@ConfirmationDialog
                }
                File(libFile.absolutePath + ".previous").delete()
                prefs.edit(commit = true) { remove(Settings.PREF_LIBRARY_CHECKSUM); remove(JniUtils.PREVIOUS_LIBRARY_CHECKSUM) }
                Runtime.getRuntime().exit(0)
            }
        )
    }
    if (tempFilePath != null)
        ConfirmationDialog(
            onDismissRequest = {
                File(tempFilePath!!).delete()
                tempFilePath = null
            },
            content = { Text(stringResource(R.string.checksum_mismatch_message, abi)) },
            onConfirmed = {
                val tempFile = File(tempFilePath!!)
                renameToLibFileAndRestart(tempFile, ChecksumCalculator.checksum(tempFile) ?: "")
            }
        )
}
