// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Copies the one-time special APK's pinned model assets into private no-backup storage.
 * Regular brrBoard APKs contain no weights, and later signed app updates keep this dir.
 * Never downloads a model, opens a socket or writes microphone audio.
 * Call from a background worker only.
 */
internal object VoiceModelInstaller {
    fun modelDir(context: Context): File = File(context.noBackupFilesDir, VoiceModelManifest.MODEL_DIR)

    fun isInstalled(context: Context): Boolean {
        val dir = modelDir(context)
        if (!File(dir, ".verified-${VoiceModelManifest.ID}").isFile) return false
        return VoiceModelManifest.assets.all { asset -> File(dir, asset.name).length() == asset.size }
    }

    @Synchronized
    fun installBundledIfPresent(context: Context): Boolean {
        if (isInstalled(context)) return true
        val appContext = context.applicationContext
        // This asset exists ONLY in the separately selected one-time model-inclusive APK.
        val first = VoiceModelManifest.assets.first()
        val present = runCatching {
            appContext.assets.open("${VoiceModelManifest.ASSET_DIR}/${first.name}").use { }
        }.isSuccess
        if (!present) return false

        val destination = modelDir(context)
        val parent = requireNotNull(destination.parentFile)
        if (!parent.exists() && !parent.mkdirs()) return false
        val stage = File(parent, ".installing-${VoiceModelManifest.ID}")
        if (stage.exists()) stage.deleteRecursively() // exact private staging path only
        if (!stage.mkdirs()) return false
        try {
            for (asset in VoiceModelManifest.assets) {
                val file = File(stage, asset.name)
                appContext.assets.open("${VoiceModelManifest.ASSET_DIR}/${asset.name}").use { input ->
                    FileOutputStream(file).use { output ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val transferred = copyAndDigest(input, output, digest)
                        require(transferred == asset.size) { "Offline voice model has an unexpected size" }
                        require(hex(digest.digest()) == asset.sha256) { "Offline voice model hash mismatch" }
                        output.fd.sync()
                    }
                }
            }
            File(stage, ".verified-${VoiceModelManifest.ID}").writeText("verified v1\n")
            if (destination.exists()) destination.deleteRecursively()
            require(stage.renameTo(destination)) { "Could not activate the offline voice model" }
            return isInstalled(context)
        } catch (_: Exception) {
            stage.deleteRecursively()
            return false
        }
    }

    private fun copyAndDigest(input: InputStream, output: FileOutputStream, digest: MessageDigest): Long {
        val buffer = ByteArray(32 * 1024)
        var bytes = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
            bytes += count
        }
        buffer.fill(0)
        return bytes
    }

    private fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        for (b in bytes) append("%02x".format(b.toInt() and 0xff))
    }
}
