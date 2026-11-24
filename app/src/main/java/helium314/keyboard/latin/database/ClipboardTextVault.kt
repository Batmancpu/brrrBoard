// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import helium314.keyboard.compat.isDeviceLocked
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Purpose-separated, per-row authenticated text encryption. No plaintext fallback. */
object ClipboardTextVault {
    enum class Status { OFF, CLEANUP_PENDING, ENCRYPTED }
    private const val ALIAS = "com.mangoloads.board.clipboard_text_v1"
    private const val STATE = "CLIPBOARD_TEXT_VAULT"
    private const val MAX_BYTES = 1_048_576
    private val purpose = "brrBoard.clipboard.text.v1".toByteArray(Charsets.UTF_8)
    private fun aad(id: Long) = purpose + ByteBuffer.allocate(8).putLong(id).array()

    internal fun key(context: Context, create: Boolean): SecretKey {
        check(!isDeviceLocked(context))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val current = store.getKey(ALIAS,null)
        if (current is SecretKey) return current
        check(create && current == null)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
        return generator.generateKey()
    }

    internal fun seal(key: SecretKey,id: Long,text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        try {
            require(bytes.size <= MAX_BYTES && bytes.toString(Charsets.UTF_8) == text)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE,key)
            require(cipher.iv.size == 12)
            cipher.updateAAD(aad(id))
            return cipher.iv + cipher.doFinal(bytes)
        } finally { bytes.fill(0) }
    }

    internal fun open(key: SecretKey,id: Long,bytes: ByteArray): String {
        require(bytes.size in 28..(MAX_BYTES+28))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        cipher.updateAAD(aad(id))
        val plain = cipher.doFinal(bytes,12,bytes.size-12)
        return try { plain.toString(Charsets.UTF_8) } finally { plain.fill(0) }
    }

    internal fun status(db: SQLiteDatabase): Status {
        val exists = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",arrayOf(STATE))
            .use { it.moveToFirst() }
        if (!exists) return Status.OFF
        return db.rawQuery("SELECT CLEANUP_PENDING FROM $STATE WHERE ID=1",null).use {
            check(it.moveToFirst())
            val pending = it.getInt(0)
            check(pending == 0 || pending == 1)
            if (pending == 1) Status.CLEANUP_PENDING else Status.ENCRYPTED
        }
    }
    internal fun enabled(db: SQLiteDatabase) = status(db) != Status.OFF

    internal fun read(cursor: Cursor,column: Int,id: Long,key: SecretKey?): String {
        return if (key != null) {
            check(cursor.getType(column) == Cursor.FIELD_TYPE_BLOB)
            open(key,id,cursor.getBlob(column))
        } else {
            check(cursor.getType(column) == Cursor.FIELD_TYPE_STRING)
            cursor.getString(column)
        }
    }

    // secure_delete is a result-returning PRAGMA even when assigning a value.
    // Android execSQL is only for statements with no returned rows. Consume and
    // verify the result on the transaction's connection before modifying data.
    private fun requireSecureDelete(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA secure_delete=ON",null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                "SQLite secure deletion unavailable"
            }
        }
    }

    /** State and every existing payload switch together in the same SQLite transaction. */
    internal fun migrate(db: SQLiteDatabase,key: SecretKey) {
        check(!enabled(db))
        db.beginTransaction()
        try {
            requireSecureDelete(db)
            // Page through ids, not all ciphertext. Existing rows and pins keep exact ids/times.
            var last: Long? = null
            while (true) {
                val rows = db.rawQuery(
                    if (last == null) "SELECT ID,TEXT FROM CLIPBOARD ORDER BY ID LIMIT 16"
                    else "SELECT ID,TEXT FROM CLIPBOARD WHERE ID>? ORDER BY ID LIMIT 16",
                    last?.let { arrayOf(it.toString()) }).use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            val id = c.getLong(0)
                            val text = read(c,1,id,null)
                            val sealed = seal(key,id,text)
                            check(open(key,id,sealed) == text)
                            add(id to sealed)
                        }
                    }
                }
                if (rows.isEmpty()) break
                for ((id,sealed) in rows) {
                    val values = ContentValues().apply { put("TEXT",sealed) }
                    check(db.update("CLIPBOARD",values,"ID=?",arrayOf(id.toString())) == 1)
                }
                last = rows.last().first
            }
            db.execSQL("CREATE TABLE $STATE (ID INTEGER PRIMARY KEY CHECK(ID=1), CLEANUP_PENDING INTEGER NOT NULL)")
            db.execSQL("INSERT INTO $STATE VALUES (1,1)")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    /** Best-effort SQLite cleanup; flash wear-leveling/older backups are outside our control. */
    internal fun finishCleanup(db: SQLiteDatabase) {
        if (status(db) != Status.CLEANUP_PENDING) return
        try {
            requireSecureDelete(db)
            fun checkpoint() = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null).use {
                it.moveToFirst() && it.getInt(0) == 0
            }
            if (!checkpoint()) return
            db.execSQL("VACUUM")
            if (!checkpoint()) return
            db.execSQL("UPDATE $STATE SET CLEANUP_PENDING=0 WHERE ID=1")
        } catch (_: Exception) { /* explicit status allows retry without plaintext fallback */ }
    }
}
