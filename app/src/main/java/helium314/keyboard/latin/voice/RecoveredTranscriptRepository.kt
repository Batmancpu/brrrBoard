// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.util.AtomicFile
import helium314.keyboard.latin.utils.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Separate from the ordinary plaintext clipboard database and OS clipboard.
 * A small AES-GCM no-backup file survives same-signer in-place updates. It
 * never writes a plaintext temporary file or falls back to unencrypted I/O.
 */
internal class RecoveredTranscriptRepository(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "recovered_transcripts.v1"))
    private val queue = RecoveredTranscriptQueue()
    private var loaded = false

    fun add(text: String): RecoveredTranscript? {
        loadOnce()
        val item = queue.add(text, System.currentTimeMillis()) ?: return null
        persist()
        return item
    }

    fun all(): List<RecoveredTranscript> {
        loadOnce()
        return queue.all(System.currentTimeMillis())
    }

    fun get(id: Long): RecoveredTranscript? {
        loadOnce()
        return queue.get(id, System.currentTimeMillis())
    }

    fun consume(id: Long): Boolean {
        loadOnce()
        if (!queue.remove(id)) return false
        persist()
        return true
    }

    fun clear() {
        queue.clear()
        loaded = true
        file.delete()
    }

    private fun loadOnce() {
        if (loaded) return
        loaded = true
        val source = file.baseFile
        if (!source.exists() || source.length() > RecoveredTranscriptVault.MAX_SEALED_BYTES) return
        try {
            val plain = RecoveredTranscriptVault.open(file.readFully())
            try {
                val items = DataInputStream(ByteArrayInputStream(plain)).use { input ->
                    if (input.readInt() != VERSION) return@use emptyList()
                    val count = input.readInt()
                    if (count !in 0..MAX_ENTRIES) return@use emptyList()
                    buildList {
                        repeat(count) {
                            val id = input.readLong()
                            val time = input.readLong()
                            val bytes = input.readInt()
                            if (bytes !in 1..MAX_TEXT_BYTES) throw IllegalStateException("Invalid transcript length")
                            val content = ByteArray(bytes)
                            input.readFully(content)
                            add(RecoveredTranscript(id, time, String(content, Charsets.UTF_8)))
                            content.fill(0)
                        }
                    }.also { if (input.available() != 0) throw IllegalStateException("Trailing transcript bytes") }
                }
                queue.restore(items, System.currentTimeMillis())
            } finally {
                plain.fill(0)
            }
        } catch (e: Exception) {
            // Never log the ciphertext, recognized words or editor information.
            Log.w("RecoveredTranscriptRepository", "Could not restore sealed transcript recovery (${e.javaClass.simpleName})")
        }
    }

    private fun persist() {
        val current = queue.all(System.currentTimeMillis())
        if (current.isEmpty()) {
            file.delete()
            return
        }
        try {
            val payload = ByteArrayOutputStream()
            DataOutputStream(payload).use { output ->
                output.writeInt(VERSION)
                output.writeInt(current.size)
                current.forEach { item ->
                    val bytes = item.text.toByteArray(Charsets.UTF_8)
                    if (bytes.size > MAX_TEXT_BYTES) throw IllegalStateException("Transcript too large")
                    output.writeLong(item.id)
                    output.writeLong(item.timestamp)
                    output.writeInt(bytes.size)
                    output.write(bytes)
                }
            }
            val plain = payload.toByteArray()
            val ciphertext = try { RecoveredTranscriptVault.seal(plain) }
                finally { plain.fill(0) }
            var stream: java.io.FileOutputStream? = null
            try {
                stream = file.startWrite()
                stream.write(ciphertext)
                file.finishWrite(stream)
            } catch (e: Exception) {
                if (stream != null) file.failWrite(stream)
                throw e
            }
        } catch (e: Exception) {
            // Still recoverable in this IME process; never persist plaintext.
            Log.w("RecoveredTranscriptRepository", "Could not seal transcript recovery (${e.javaClass.simpleName})")
        }
    }

    companion object {
        private const val VERSION = 1
        private const val MAX_ENTRIES = 5
        private const val MAX_TEXT_BYTES = 48_000
    }
}
