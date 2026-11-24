// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import javax.crypto.SecretKey
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.SystemClock
import helium314.keyboard.latin.ClipboardImageHistoryClip
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import java.io.File

/*
 possible extension for later: allow non-text
 setting whether to allow it at all (because it could be slow with large files)
 separate retention time setting
 add mime type column
 add file name column
 add hash column (sha 256) for quick unique check (check full content on hash conflict)
 more sophisticated content loading: some getContent that reads the file, with cache
 async file reads and writes
 caches should be dropped on low memory
 */

/** Class providing cached access to the clipboard table */
// A screenshot worker and settings export can run concurrently with the IME.
 // Serialize mutations, including vault migration, before publishing cache changes.
class ClipboardDao private constructor(private val db: Database, context: Context) {
    interface Listener {
        fun onClipInserted(position: Int)
        fun onClipsRemoved(position: Int, count: Int)
        fun onClipMoved(oldPosition: Int, newPosition: Int)
    }

    @Volatile var listener: Listener? = null
    private val context = context.applicationContext

    // we clean up old clips when a new clip is added, but not too frequently
    private var lastClearOldClips = 0L

    private var vaultKey: SecretKey? = if (ClipboardTextVault.enabled(db.readableDatabase))
        ClipboardTextVault.key(context, false) else null
    private var payloadWritesBlocked = false

    // Existing in-memory history; this feature protects persisted payloads, not RAM.
    private val cache = mutableListOf<ClipboardHistoryEntry>().apply {
        db.readableDatabase.query(
            TABLE,
            arrayOf(COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_PINNED, COLUMN_TEXT),
            null,
            null,
            null,
            null,
            "$COLUMN_PINNED, $COLUMN_TIMESTAMP DESC" // was only relevant in the initial approach of using a cursor instead of a cache
        ).use {
            while (it.moveToNext()) {
                add(ClipboardHistoryEntry(it.getLong(0), it.getLong(1), it.getInt(2) != 0,
                    ClipboardTextVault.read(it,3,it.getLong(0),vaultKey)))
            }
        }
        sort()
    }

    @Synchronized fun addClip(timestamp: Long, pinned: Boolean, text: String) {
        clearOldClips()
        val existingIndex = cache.indexOfFirst { it.text == text }
        if (existingIndex >= 0 && cache[existingIndex].timeStamp == timestamp)
            return // nothing to do
        if (existingIndex >= 0) {
            updateTimestampAt(existingIndex, timestamp)
            return
        }
        insertNewEntry(timestamp, pinned, text)
    }

    private fun insertNewEntry(timestamp: Long, pinned: Boolean, text: String) {
        val cv = ContentValues(3)
        cv.put(COLUMN_TIMESTAMP, timestamp)
        cv.put(COLUMN_PINNED, pinned)
        val rowId = insertPayload(db.writableDatabase,cv,text,null)

        val entry = ClipboardHistoryEntry(rowId, timestamp, pinned, text)
        cache.add(entry)
        cache.sort()
        listener?.onClipInserted(cache.indexOf(entry))
    }

    private fun insertPayload(database: SQLiteDatabase, values: ContentValues,
                              text: String, fixedId: Long?): Long {
        check(!payloadWritesBlocked) { "Clipboard vault state unavailable" }
        val key = vaultKey
        if (key == null) {
            values.put(COLUMN_TEXT,text)
            return database.insertOrThrow(TABLE,null,values)
        }
        if (fixedId != null) {
            values.put(COLUMN_TEXT,ClipboardTextVault.seal(key,fixedId,text))
            return database.insertOrThrow(TABLE,null,values)
        }
        // No plaintext enters a journal, even temporarily: allocate the id with
        // a null payload in the same transaction, then write authenticated bytes.
        val ownsTransaction = !database.inTransaction()
        if (ownsTransaction) database.beginTransaction()
        try {
            values.putNull(COLUMN_TEXT)
            val id = database.insertOrThrow(TABLE,null,values)
            val sealed = ContentValues().apply { put(COLUMN_TEXT,ClipboardTextVault.seal(key,id,text)) }
            check(database.update(TABLE,sealed,"$COLUMN_ID = ?",arrayOf(id.toString())) == 1)
            if (ownsTransaction) database.setTransactionSuccessful()
            return id
        } finally { if (ownsTransaction) database.endTransaction() }
    }

    /** Opt-in, irreversible per database. On any pre-commit failure the old rows remain intact. */
    @Synchronized fun enableTextVault(): ClipboardTextVault.Status {
        val database = db.writableDatabase
        if (vaultKey == null) {
            val key = ClipboardTextVault.key(context,true)
            try {
                ClipboardTextVault.migrate(database,key)
                vaultKey = key
            } catch (failure: Exception) {
                // A commit can report a late I/O error. Recheck the durable mode,
                // never resume plaintext writes if it cannot be established.
                val mode = runCatching { ClipboardTextVault.status(database) }.getOrNull()
                if (mode == null) payloadWritesBlocked = true
                else if (mode != ClipboardTextVault.Status.OFF) vaultKey = key
                throw failure
            }
        }
        payloadWritesBlocked = false
        ClipboardTextVault.finishCleanup(database)
        return ClipboardTextVault.status(database)
    }

    @Synchronized fun textVaultStatus(): ClipboardTextVault.Status =
        ClipboardTextVault.status(db.readableDatabase)

    @Synchronized fun portableText(): List<ClipboardPortableBackup.Entry> {
        check(!helium314.keyboard.compat.isDeviceLocked(context))
        return cache.filterNot { ClipboardImageHistoryClip.hasImageHistoryPrefix(it.text) }
            .map { ClipboardPortableBackup.Entry(it.timeStamp,it.isPinned,it.text) }
    }

    /** Validate the entire import, then merge without destroying existing clips or pins. */
    @Synchronized fun mergePortableText(entries: List<ClipboardPortableBackup.Entry>) {
        require(ClipboardPortableBackup.valid(entries))
        check(!helium314.keyboard.compat.isDeviceLocked(context))
        check(listener == null) { "Close clipboard before restoring history" }
        val database = db.writableDatabase
        val original = cache.toList()
        val next = original.map { ClipboardHistoryEntry(it.id,it.timeStamp,it.isPinned,it.text) }.toMutableList()
        database.beginTransaction()
        try {
            for (item in entries) {
                val existing = next.firstOrNull { it.text == item.text }
                if (existing != null) {
                    val stamp = maxOf(existing.timeStamp,item.timestamp)
                    val pin = existing.isPinned || item.pinned
                    if (stamp != existing.timeStamp || pin != existing.isPinned) {
                        val cv = ContentValues().apply { put(COLUMN_TIMESTAMP,stamp); put(COLUMN_PINNED,pin) }
                        check(database.update(TABLE,cv,"$COLUMN_ID = ?",arrayOf(existing.id.toString())) == 1)
                        existing.timeStamp = stamp; existing.isPinned = pin
                    }
                } else {
                    val cv = ContentValues().apply { put(COLUMN_TIMESTAMP,item.timestamp); put(COLUMN_PINNED,item.pinned) }
                    val id = insertPayload(database,cv,item.text,null)
                    next.add(ClipboardHistoryEntry(id,item.timestamp,item.pinned,item.text))
                }
            }
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
        cache.clear(); cache.addAll(next); cache.sort()
        // Restore normally restarts the settings/IME. Refuse to mutate an active
        // RecyclerView through background callbacks; settings callers must hide it.
    }

    private fun updateTimestampAt(index: Int, timestamp: Long) {
        val entry = cache[index]
        entry.timeStamp = timestamp
        cache.sort()
        listener?.onClipMoved(index, cache.indexOf(entry))
        val cv = ContentValues(1)
        cv.put(COLUMN_TIMESTAMP, timestamp)
        db.writableDatabase.update(TABLE, cv, "$COLUMN_ID = ${entry.id}", null)
    }

    @Synchronized fun isPinned(index: Int) = cache[index].isPinned

    @Synchronized fun getAt(index: Int) = cache[index]

    @Synchronized fun get(id: Long) = cache.first { it.id == id }

    @Synchronized fun count() = cache.size

    @Synchronized fun sort() = cache.sort()

    @Synchronized fun togglePinned(id: Long) {
        val entry = cache.first { it.id == id }
        entry.isPinned = !entry.isPinned
        entry.timeStamp = System.currentTimeMillis()
        if (listener != null) {
            val oldPos = cache.indexOf(entry)
            cache.sort()
            val newPos = cache.indexOf(entry)
            listener?.onClipMoved(oldPos, newPos)
        } else {
            cache.sort()
        }
        val cv = ContentValues(2)
        cv.put(COLUMN_PINNED, entry.isPinned)
        cv.put(COLUMN_TIMESTAMP, entry.timeStamp)
        db.writableDatabase.update(TABLE, cv, "$COLUMN_ID = ${entry.id}", null)
    }

    // RecyclerView initiates this, so we don't call listener (or we'll get an IndexOutOfRangeException from RecyclerView)
    @Synchronized fun deleteClipAt(index: Int): ClipboardHistoryEntry {
        val entry = cache[index]
        cache.remove(entry)
        db.writableDatabase.delete(TABLE, "$COLUMN_ID = ${entry.id}", null)
        return entry
    }

    @Synchronized fun restoreClip(entry: ClipboardHistoryEntry, targetIndex: Int): Int {
        val cv = ContentValues(4)
        cv.put(COLUMN_ID, entry.id)
        cv.put(COLUMN_TIMESTAMP, entry.timeStamp)
        cv.put(COLUMN_PINNED, entry.isPinned)
        // A restored swipe-delete id must not replace a different live record.
        if (cache.any { it.id == entry.id }) return -1
        insertPayload(db.writableDatabase,cv,entry.text,entry.id)

        val insertIndex = targetIndex.coerceIn(0, cache.size)
        cache.add(insertIndex, entry)
        cache.sort()
        return cache.indexOf(entry)
    }

    @Synchronized fun clearOldClips(now: Boolean = false) {
        if (listener != null)
            return // never clear when clipboard is visible
        if (!now && lastClearOldClips > SystemClock.elapsedRealtime() - 5 * 1000)
            return

        lastClearOldClips = SystemClock.elapsedRealtime()
        val retentionTime = Settings.getValues()?.mClipboardHistoryRetentionTime ?: 121L
        if (retentionTime > 120) return
        val minTime = System.currentTimeMillis() - retentionTime * 60 * 1000L
        val entriesToRemove = cache.filter { it.timeStamp < minTime && !it.isPinned }
        if (entriesToRemove.isEmpty())
            return // nothing was removed
        cache.removeAll(entriesToRemove.toSet())

        db.writableDatabase.delete(TABLE, "$COLUMN_TIMESTAMP < $minTime AND $COLUMN_PINNED = 0", null)
        deleteCachedImageFiles(entriesToRemove)
    }

    @Synchronized fun clearNonPinned() {
        val entriesToRemove = cache.filter { !it.isPinned }
        if (entriesToRemove.isEmpty())
            return // nothing to remove

        if (listener != null) {
            val indicesToRemove = mutableListOf<Int>()
            cache.forEachIndexed { idx, clip ->
                if (!clip.isPinned)
                    indicesToRemove.add(idx)
            }
            cache.removeAll(entriesToRemove.toSet())
            listener?.onClipsRemoved(indicesToRemove[0], indicesToRemove.size)
        } else {
            cache.removeAll(entriesToRemove.toSet())
        }
        db.writableDatabase.delete(TABLE, "$COLUMN_PINNED = 0", null)
        deleteCachedImageFiles(entriesToRemove)
    }

    @Synchronized fun clear() {
        if (count() == 0) return
        val entriesToRemove = cache.toList()
        val removedCount = entriesToRemove.size
        cache.clear()
        listener?.onClipsRemoved(0, removedCount)
        db.writableDatabase.delete(TABLE, null, null)
        deleteCachedImageFiles(entriesToRemove)
    }

    private fun deleteCachedImageFiles(entries: Collection<ClipboardHistoryEntry>) {
        entries.forEach { entry ->
            val uri = ClipboardImageHistoryClip.decode(entry.text)?.uri ?: return@forEach
            val file = cachedImageFile(uri) ?: return@forEach
            if (ClipboardImageVault.hasStored(file) && !ClipboardImageVault.delete(context,file)) {
                Log.e(TAG, "can't delete cached clipboard image")
            }
            runCatching { context.revokeUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    private fun cachedImageFile(uri: Uri): File? {
        val clipboardDir = File(context.cacheDir, "clipboard")
        val file = when (uri.scheme) {
            "content" -> {
                if (uri.authority != "${context.packageName}.fileprovider") return null
                val segments = uri.pathSegments
                if (segments.size < 3 || segments[0] != "cache" || segments[1] != "clipboard") return null
                File(context.cacheDir, segments.drop(1).joinToString(File.separator))
            }
            "file" -> File(uri.path ?: return null)
            else -> return null
        }
        return try {
            val canonicalDir = clipboardDir.canonicalFile
            val canonicalFile = file.canonicalFile
            val dirPath = canonicalDir.path
            val filePath = canonicalFile.path
            if (filePath.startsWith("$dirPath${File.separator}")) canonicalFile else null
        } catch (e: Exception) {
            Log.e(TAG, "can't resolve cached clipboard image (${e.javaClass.simpleName})")
            null
        }
    }

    companion object {
        private const val TAG = "ClipboardDao"

        private const val TABLE = "CLIPBOARD"
        // it's possible timestamp is not unique, so we use a separate ID
        // ID is generated and returned on insert, see https://sqlite.org/rowidtable.html
        private const val COLUMN_ID = "ID"
        private const val COLUMN_TIMESTAMP = "TIMESTAMP"
        private const val COLUMN_PINNED = "PINNED"
        private const val COLUMN_TEXT = "TEXT" // we could enforce unique text, but that's only necessary if we can drop the cache (later)
        const val CREATE_TABLE = """
            CREATE TABLE $TABLE (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_PINNED TINYINT NOT NULL,
                $COLUMN_TEXT TEXT
            )
        """

        private var instance: ClipboardDao? = null

        /** Returns the instance or creates a new one. Returns null if instance can't be created (e.g. no access to db due to device being locked) */
        @Synchronized fun getInstance(context: Context): ClipboardDao? {
            if (instance == null)
                try {
                    instance = ClipboardDao(Database.getInstance(context), context)
                } catch (e: Throwable) {
                    Log.e(TAG, "Clipboard storage unavailable (${e.javaClass.simpleName})")
                }
            return instance
        }
    }
}
