// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserManager
import android.util.AtomicFile
import java.io.File

/** Main-thread-only latest-session recovery. No serialization or clipboard writes. */
internal class RecoveredTranscriptRepository(context: Context) {
    private val store = VoiceRecoveryWindow()
    private val handler = Handler(Looper.getMainLooper())
    private val expire = Runnable {
        store.all(SystemClock.elapsedRealtime())
        onChanged?.invoke()
    }
    var onChanged: (() -> Unit)? = null

    init {
        // Purge only the obsolete on-disk recovery format; never restore it.
        AtomicFile(File(context.createDeviceProtectedStorageContext().noBackupFilesDir,
            "recovered_transcripts.v1")).delete()
        if (context.getSystemService(UserManager::class.java)?.isUserUnlocked == true) {
            AtomicFile(File(context.noBackupFilesDir, "recovered_transcripts.v1")).delete()
        }
    }

    fun add(text: String): RecoveredTranscript? {
        val result = store.add(text, SystemClock.elapsedRealtime()) ?: return null
        handler.removeCallbacks(expire)
        handler.postDelayed(expire, VoiceRecoveryWindow.RETENTION_MS)
        onChanged?.invoke()
        return result
    }

    fun all(): List<RecoveredTranscript> = store.all(SystemClock.elapsedRealtime())
    fun get(id: Long): RecoveredTranscript? = all().firstOrNull { it.id == id }
    fun consume(id: Long): Boolean {
        val removed = store.remove(id)
        if (removed) {
            handler.removeCallbacks(expire)
            onChanged?.invoke()
        }
        return removed
    }
    fun clear() {
        handler.removeCallbacks(expire)
        store.clear()
        onChanged?.invoke()
    }
}
