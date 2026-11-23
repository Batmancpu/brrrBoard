// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import android.content.Context
import helium314.keyboard.latin.utils.DeviceProtectedUtils
import android.net.Uri
import android.os.Build
import android.system.Os
import androidx.core.content.edit
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ChecksumCalculator
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.latin.utils.protectedPrefs
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

private const val MAX_LIBRARY_BYTES = 8 * 1024 * 1024
private const val ARM64_MODULE_ENTRY = "system/lib64/libjni_latinimegoogle.so"

internal fun stageGestureLibrary(context: Context, uri: Uri): File {
    val selected = File.createTempFile("gesture-source-", ".tmp", context.cacheDir)
    val appFilesDir = DeviceProtectedUtils.getFilesDir(context)
        ?: throw IOException("App data directory unavailable")
    val staged = File.createTempFile("gesture-lib-", ".tmp", appFilesDir)
    var stagedSuccessfully = false
    try {
        val source = context.contentResolver.openInputStream(uri) ?: throw IOException("Empty selection")
        source.use { input -> selected.outputStream().use { input.copyBounded(it) } }
        val isZip = selected.inputStream().use { it.read() == 0x50 && it.read() == 0x4b }
        if (isZip) {
            if (Build.SUPPORTED_ABIS.firstOrNull() != "arm64-v8a") throw IOException("ARM64 module required")
            ZipFile(selected).use { zip ->
                val matches = zip.entries().asSequence().filter { it.name == ARM64_MODULE_ENTRY }.toList()
                val entry = matches.singleOrNull() ?: throw IOException("Missing or repeated library entry")
                if (entry.isDirectory || entry.size !in 1L..MAX_LIBRARY_BYTES.toLong())
                    throw IOException("Invalid library size")
                zip.getInputStream(entry).use { input ->
                    staged.outputStream().use { output ->
                        if (!staged.setReadOnly()) throw IOException("Cannot protect staged library")
                        input.copyBounded(output)
                    }
                }
            }
            if (ChecksumCalculator.checksum(staged) != JniUtils.expectedDefaultChecksum())
                throw IOException("Unknown module library")
        } else {
            selected.inputStream().use { input ->
                staged.outputStream().use { output ->
                    if (!staged.setReadOnly()) throw IOException("Cannot protect staged library")
                    input.copyBounded(output)
                }
            }
        }
        stagedSuccessfully = true
        return staged
    } finally {
        selected.delete()
        if (!stagedSuccessfully) staged.delete()
    }
}

internal fun installGestureLibrary(context: Context, staged: File, checksum: String) {
    val appFilesDir = DeviceProtectedUtils.getFilesDir(context)
        ?: throw IOException("App data directory unavailable")
    val destination = File(appFilesDir, JniUtils.JNI_LIB_IMPORT_FILE_NAME)
    Os.rename(staged.absolutePath, destination.absolutePath)
    context.protectedPrefs().edit(commit = true) { putString(Settings.PREF_LIBRARY_CHECKSUM, checksum) }
}

private fun InputStream.copyBounded(output: OutputStream) {
    var total = 0
    val buffer = ByteArray(8192)
    while (true) {
        val size = read(buffer)
        if (size < 0) break
        total += size
        if (total > MAX_LIBRARY_BYTES) throw IOException("Import is too large")
        output.write(buffer, 0, size)
    }
    if (total == 0) throw IOException("Empty library")
}
