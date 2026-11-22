// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Optionally copies pinned bootstrap APK model assets into private no-backup storage.
 * Subsequent updates use the same verified copy without bundling weights again.
 * Never downloads a model, opens a socket or writes microphone audio.
 * Call from a background worker only.
 */
internal object VoiceModelInstaller {
    enum class InstallState { NOT_INSTALLED, INSTALLING, READY, FAILED }
    data class Progress(val state: InstallState, val percent: Int)

    @Volatile var installState = InstallState.NOT_INSTALLED
        private set
    @Volatile var installPercent = 0
        private set
    private val progressByModel = ConcurrentHashMap<String, Progress>()

    fun modelDir(context: Context): File = modelDir(context, VoiceModelManifest.pack)
    fun modelDir(context: Context, pack: VoiceModelPack): File = File(context.noBackupFilesDir, pack.modelDir)

    fun isInstalled(context: Context): Boolean = isInstalled(context, VoiceModelManifest.pack)

    fun isInstalled(context: Context, pack: VoiceModelPack): Boolean {
        val dir = modelDir(context, pack)
        if (!File(dir, ".verified-${pack.id}").isFile) return false
        return pack.assets.all { asset -> File(dir, asset.name).length() == asset.size }
    }

    fun hasBundled(context: Context, pack: VoiceModelPack): Boolean {
        if (pack.assets.isEmpty()) return false
        return runCatching {
            val names = context.applicationContext.assets.list(pack.assetDir)?.toSet().orEmpty()
            pack.assets.all { it.name in names }
        }.getOrDefault(false)
    }

    @Synchronized
    fun installBundledIfPresent(context: Context): Boolean = installBundledIfPresent(context, VoiceModelManifest.pack)

    /** Installs model assets only when this app update explicitly contains them. */
    @Synchronized
    fun installBundledIfPresent(context: Context, pack: VoiceModelPack): Boolean {
        if (isInstalled(context, pack)) {
            setProgress(pack, InstallState.READY, 100)
            return true
        }
        val appContext = context.applicationContext
        // Normal lightweight updates intentionally omit these assets. A verified
        // model copied by an earlier release was already returned above.
        if (!hasBundled(appContext, pack)) {
            setProgress(pack, InstallState.NOT_INSTALLED, 0)
            return false
        }

        setProgress(pack, InstallState.INSTALLING, 0)

        val destination = modelDir(context, pack)
        val parent = requireNotNull(destination.parentFile)
        if (!parent.exists() && !parent.mkdirs()) {
            setProgress(pack, InstallState.FAILED, 0)
            return false
        }
        val stage = File(parent, ".installing-${pack.id}")
        if (stage.exists() && !stage.deleteRecursively()) {
            setProgress(pack, InstallState.FAILED, 0)
            return false
        }
        // Keep room for the complete staged model plus filesystem/metadata overhead.
        // The verified prior pack remains untouched if the device is short on space.
        val totalBytes = pack.assets.sumOf { it.size }
        val requiredBytes = totalBytes + 64L * 1024L * 1024L
        val availableBytes = runCatching { StatFs(parent.absolutePath).availableBytes }.getOrDefault(0L)
        if (availableBytes < requiredBytes) {
            setProgress(pack, InstallState.FAILED, 0)
            return false
        }
        if (!stage.mkdirs()) {
            setProgress(pack, InstallState.FAILED, 0)
            return false
        }
        try {
            var completedBytes = 0L
            for (asset in pack.assets) {
                val file = File(stage, asset.name)
                appContext.assets.open("${pack.assetDir}/${asset.name}").use { input ->
                    FileOutputStream(file).use { output ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val transferred = copyAndDigest(input, output, digest) { currentBytes ->
                            setProgress(pack, InstallState.INSTALLING,
                                (((completedBytes + currentBytes) * 100L) / totalBytes).toInt().coerceIn(0, 99))
                        }
                        require(transferred == asset.size) { "Offline voice model has an unexpected size" }
                        require(hex(digest.digest()) == asset.sha256) { "Offline voice model hash mismatch" }
                        output.fd.sync()
                    }
                }
                completedBytes += asset.size
            }
            File(stage, ".verified-${pack.id}").writeText("verified v1\n")
            if (destination.exists()) destination.deleteRecursively()
            require(stage.renameTo(destination)) { "Could not activate the offline voice model" }
            val ready = isInstalled(context, pack)
            setProgress(pack, if (ready) InstallState.READY else InstallState.FAILED, if (ready) 100 else 0)
            return ready
        } catch (_: Exception) {
            stage.deleteRecursively()
            setProgress(pack, InstallState.FAILED, 0)
            return false
        }
    }

    fun progress(pack: VoiceModelPack): Progress = progressByModel[pack.id]
        ?: if (pack.id == VoiceModelManifest.ID) Progress(installState, installPercent)
        else Progress(InstallState.NOT_INSTALLED, 0)

    private fun setProgress(pack: VoiceModelPack, state: InstallState, percent: Int) {
        val safePercent = percent.coerceIn(0, 100)
        progressByModel[pack.id] = Progress(state, safePercent)
        if (pack.id == VoiceModelManifest.ID) {
            installState = state
            installPercent = safePercent
        }
    }

    private fun copyAndDigest(input: InputStream, output: FileOutputStream,
        digest: MessageDigest, progress: (Long) -> Unit): Long {
        val buffer = ByteArray(32 * 1024)
        var bytes = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
            bytes += count
            if ((bytes / (1024L * 1024L)) != ((bytes - count) / (1024L * 1024L))) {
                progress(bytes)
            }
        }
        buffer.fill(0)
        return bytes
    }

    private fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        for (b in bytes) append("%02x".format(b.toInt() and 0xff))
    }
}
