// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context

/**
 * A killed process cannot run its recording-finally block. Delete only exact
 * orphaned brrBoard red-mode PCM files when the next app process starts.
 * Blue offline voice never creates these files or uses this cleanup path.
 */
internal object PrivateVoiceCacheCleanup {
    private val recordedName = Regex(
        "brrboard-voice-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
            "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.pcm",
    )

    fun isOrphanName(name: String): Boolean = recordedName.matches(name)

    fun removeOrphans(context: Context, olderThanMillis: Long) {
        context.cacheDir.listFiles { file ->
            file.isFile && isOrphanName(file.name) && file.lastModified() < olderThanMillis
        }
            ?.forEach { file -> runCatching { file.delete() } }
    }
}
