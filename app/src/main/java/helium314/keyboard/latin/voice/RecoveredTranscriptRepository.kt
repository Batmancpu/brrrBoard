// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Voice recovery is intentionally disabled; construction purges older saved transcripts. */
internal class RecoveredTranscriptRepository(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "recovered_transcripts.v1"))

    init { file.delete() }

    fun add(text: String): RecoveredTranscript? = null
    fun all(): List<RecoveredTranscript> = emptyList()
    fun get(id: Long): RecoveredTranscript? = null
    fun consume(id: Long): Boolean = false
    fun clear() { file.delete() }
}
