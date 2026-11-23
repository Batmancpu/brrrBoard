// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.AtomicFile
import java.io.File

/** Voice recovery is intentionally disabled; construction purges older saved transcripts. */
internal class RecoveredTranscriptRepository(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "recovered_transcripts.v1"))

    init {
        // Remove the legacy Direct-Boot copy as well as any CE remnant. This
        // repository has no write path; recognized speech is never recovered.
        AtomicFile(File(context.createDeviceProtectedStorageContext().noBackupFilesDir,
            "recovered_transcripts.v1")).delete()
        val userManager = context.getSystemService(UserManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || userManager?.isUserUnlocked == true) {
            file.delete()
        }
    }

    fun add(text: String): RecoveredTranscript? = null
    fun all(): List<RecoveredTranscript> = emptyList()
    fun get(id: Long): RecoveredTranscript? = null
    fun consume(id: Long): Boolean = false
    fun clear() { file.delete() }
}
