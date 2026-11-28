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
        validateElfLibrary(staged)
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
    validateElfLibrary(staged)
    if (checksum.isBlank() || ChecksumCalculator.checksum(staged) != checksum) throw IOException("Library checksum changed")
    val prefs = context.protectedPrefs()
    val oldChecksum = prefs.getString(Settings.PREF_LIBRARY_CHECKSUM, JniUtils.expectedDefaultChecksum())
    val previous = File(appFilesDir, JniUtils.JNI_LIB_IMPORT_FILE_NAME + ".previous")
    if (previous.exists()) throw IOException("Previous library recovery pending")
    if (destination.exists()) Os.link(destination.absolutePath, previous.absolutePath)
    try {
        Os.rename(staged.absolutePath, destination.absolutePath)
        if (!prefs.edit().putString(Settings.PREF_LIBRARY_CHECKSUM, checksum)
                .putString(JniUtils.PREVIOUS_LIBRARY_CHECKSUM, oldChecksum).commit())
            throw IOException("Cannot commit library checksum")
    } catch (error: Exception) {
        if (previous.isFile) Os.rename(previous.absolutePath, destination.absolutePath)
        else destination.delete()
        prefs.edit().putString(Settings.PREF_LIBRARY_CHECKSUM, oldChecksum)
            .remove(JniUtils.PREVIOUS_LIBRARY_CHECKSUM).commit()
        throw error
    }
    // The previous bytes survive until the next successful native load.
}

private fun validateElfLibrary(file: File) {
    val header = ByteArray(20)
    file.inputStream().use { input -> java.io.DataInputStream(input).readFully(header) }
    if (header[0] != 0x7f.toByte() || header[1] != 0x45.toByte() || header[2] != 0x4c.toByte() ||
        header[3] != 0x46.toByte() || header[5] != 1.toByte() || header[6] != 1.toByte())
        throw IOException("Not a supported ELF library")
    val (elfClass, machine) = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> 2 to 183
        "armeabi-v7a" -> 1 to 40
        "x86_64" -> 2 to 62
        "x86" -> 1 to 3
        else -> throw IOException("Unsupported ABI")
    }
    fun u16(offset: Int) = (header[offset].toInt() and 255) or ((header[offset+1].toInt() and 255) shl 8)
    if (header[4].toInt() != elfClass || u16(16) != 3 || u16(18) != machine)
        throw IOException("Incompatible native library")
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
