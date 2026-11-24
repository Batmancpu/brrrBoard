// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import javax.crypto.SecretKey
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import helium314.keyboard.compat.isDeviceLocked
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

    // No history-wide plaintext list. HMAC is process-local, never stored on disk.
    private class IndexEntry(val id: Long,var timeStamp: Long,var isPinned: Boolean,
                             val image: Boolean,val digest: ByteArray): Comparable<IndexEntry> {
        override fun compareTo(other: IndexEntry): Int {
            val pin=other.isPinned.compareTo(isPinned)
            if(pin==0) return other.timeStamp.compareTo(timeStamp)
            return if(Settings.getValues()?.mClipboardHistoryPinnedFirst==false) -pin else pin
        }
        fun copyEntry()=IndexEntry(id,timeStamp,isPinned,image,digest)
    }
    private val digestKey=SecretKeySpec(ByteArray(32).also { SecureRandom().nextBytes(it) },"HmacSHA256")
    private fun fingerprint(text: String): ByteArray {
        val bytes=text.toByteArray(Charsets.UTF_8)
        return try { Mac.getInstance("HmacSHA256").apply { init(digestKey) }.doFinal(bytes) }
        finally { bytes.fill(0) }
    }
    private fun index(id: Long,stamp: Long,pinned: Boolean,text: String)=
        IndexEntry(id,stamp,pinned,ClipboardImageHistoryClip.hasImageHistoryPrefix(text),fingerprint(text))
    private val payloadCache=ClipboardPayloadCache(SystemClock::elapsedRealtime)
    private val expiryHandler=Handler(Looper.getMainLooper())
    private val expirePayloads: Runnable=Runnable { synchronized(this) { payloadCache.prune(); scheduleExpiry() } }
    private fun scheduleExpiry() {
        expiryHandler.removeCallbacks(expirePayloads)
        val deadline=payloadCache.expiry()
        if(deadline!=0L) expiryHandler.postDelayed(expirePayloads,(deadline-SystemClock.elapsedRealtime()).coerceAtLeast(1L))
    }
    @Synchronized fun clearDecryptedHistory() {
        expiryHandler.removeCallbacks(expirePayloads)
        payloadCache.clear()
    }
    private fun readPayload(id: Long): String {
        check(!isDeviceLocked(context))
        return db.readableDatabase.query(TABLE,arrayOf(COLUMN_TEXT),"$COLUMN_ID = ?",arrayOf(id.toString()),null,null,null).use {
            check(it.moveToFirst()) { "Clipboard row unavailable" }
            ClipboardTextVault.read(it,0,id,vaultKey)
        }
    }
    private fun materialize(e: IndexEntry): ClipboardHistoryEntry? {
        if(isDeviceLocked(context)) { clearDecryptedHistory(); return null }
        val text=payloadCache.get(e.id) ?: try { readPayload(e.id).also { payloadCache.put(e.id,it); scheduleExpiry() } }
            catch (_: Exception) { clearDecryptedHistory(); return null }
        return ClipboardHistoryEntry(e.id,e.timeStamp,e.isPinned,text)
    }
    private fun same(e: IndexEntry,digest: ByteArray,text: String): Boolean =
        e.digest.contentEquals(digest) && readPayload(e.id)==text
    private val cache = mutableListOf<IndexEntry>().apply {
        check(!isDeviceLocked(context))
        db.readableDatabase.query(TABLE,arrayOf(COLUMN_ID,COLUMN_TIMESTAMP,COLUMN_PINNED,COLUMN_TEXT),
            null,null,null,null,null).use {
            while(it.moveToNext()) {
                val id=it.getLong(0)
                add(index(id,it.getLong(1),it.getInt(2)!=0,ClipboardTextVault.read(it,3,id,vaultKey)))
            }
        }
        sort()
    }

    @Synchronized fun addClip(timestamp: Long, pinned: Boolean, text: String) {
        check(!isDeviceLocked(context))
        clearOldClips()
        val digest=fingerprint(text)
        val existingIndex = cache.indexOfFirst { same(it,digest,text) }
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

        val entry = index(rowId, timestamp, pinned, text)
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
        clearDecryptedHistory()
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
        val result=ArrayList<ClipboardPortableBackup.Entry>()
        var chars=0L
        for(e in cache) if(!e.image) {
            val text=readPayload(e.id)
            chars+=text.length
            require(result.size<10_000 && chars<=8*1024*1024) { "Clipboard export exceeds limit" }
            result.add(ClipboardPortableBackup.Entry(e.timeStamp,e.isPinned,text))
        }
        return result
    }

    /** Validate the entire import, then merge without destroying existing clips or pins. */
    @Synchronized fun mergePortableText(entries: List<ClipboardPortableBackup.Entry>) {
        require(ClipboardPortableBackup.valid(entries))
        check(!helium314.keyboard.compat.isDeviceLocked(context))
        check(listener == null) { "Close clipboard before restoring history" }
        val database = db.writableDatabase
        val original = cache.toList()
        val next = original.map { it.copyEntry() }.toMutableList()
        database.beginTransaction()
        try {
            for (item in entries) {
                val digest=fingerprint(item.text)
                val existing = next.firstOrNull { same(it,digest,item.text) }
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
                    next.add(index(id,item.timestamp,item.pinned,item.text))
                }
            }
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
        clearDecryptedHistory(); cache.clear(); cache.addAll(next); cache.sort()
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

    @Synchronized fun isPinned(index: Int) = cache.getOrNull(index)?.isPinned ?: true

    @Synchronized fun getAt(index: Int) = cache.getOrNull(index)?.let { materialize(it) }

    @Synchronized fun get(id: Long) = cache.firstOrNull { it.id == id }?.let { materialize(it) }

    @Synchronized fun count() = cache.size

    /** Scan one decrypted row at a time; return only IDs, never a plaintext results list. */
    @Synchronized fun searchIds(query: String): List<Long> {
        val q=query.trim()
        if(q.isEmpty() || isDeviceLocked(context)) return emptyList()
        return cache.asSequence().filter { !it.image && runCatching { readPayload(it.id).contains(q,ignoreCase=true) }.getOrDefault(false) }
            .sortedWith(compareByDescending<IndexEntry> { it.timeStamp }.thenByDescending { it.id })
            .map { it.id }.toList()
    }
    @Synchronized fun hasUnpinned() = cache.any { !it.isPinned }

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
    @Synchronized fun deleteClipAt(index: Int): ClipboardHistoryEntry? {
        val entry = cache.getOrNull(index) ?: return null
        val result=materialize(entry) ?: return null
        db.writableDatabase.delete(TABLE, "$COLUMN_ID = ${entry.id}", null)
        cache.remove(entry); payloadCache.remove(entry.id)
        return result
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
        val indexed=index(entry.id,entry.timeStamp,entry.isPinned,entry.text)
        cache.add(insertIndex, indexed)
        cache.sort()
        return cache.indexOf(indexed)
    }

    @Synchronized fun clearOldClips(now: Boolean = false) {
        if (listener != null || isDeviceLocked(context))
            return // never clear when clipboard is visible or locked
        if (!now && lastClearOldClips > SystemClock.elapsedRealtime() - 5 * 1000)
            return

        lastClearOldClips = SystemClock.elapsedRealtime()
        val retentionTime = Settings.getValues()?.mClipboardHistoryRetentionTime ?: 121L
        if (retentionTime > 120) return
        val minTime = System.currentTimeMillis() - retentionTime * 60 * 1000L
        val entriesToRemove = cache.filter { it.timeStamp < minTime && !it.isPinned }
        if (entriesToRemove.isEmpty())
            return // nothing was removed
        val images=imageUris(entriesToRemove)
        db.writableDatabase.delete(TABLE, "$COLUMN_TIMESTAMP < $minTime AND $COLUMN_PINNED = 0", null)
        cache.removeAll(entriesToRemove.toSet()); clearDecryptedHistory()
        deleteCachedImageFiles(images)
    }

    @Synchronized fun clearNonPinned() {
        val entriesToRemove = cache.filter { !it.isPinned }
        if (entriesToRemove.isEmpty())
            return // nothing to remove

        val images=imageUris(entriesToRemove)
        db.writableDatabase.delete(TABLE, "$COLUMN_PINNED = 0", null)
        clearDecryptedHistory()
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
        deleteCachedImageFiles(images)
    }

    @Synchronized fun clear() {
        if (count() == 0) return
        val entriesToRemove = cache.toList()
        val removedCount = entriesToRemove.size
        val images=imageUris(entriesToRemove)
        db.writableDatabase.delete(TABLE, null, null)
        clearDecryptedHistory(); cache.clear()
        listener?.onClipsRemoved(0, removedCount)
        deleteCachedImageFiles(images)
    }

    private fun imageUris(entries: Collection<IndexEntry>): List<Uri> =
        entries.filter { it.image }.mapNotNull { e ->
            runCatching { ClipboardImageHistoryClip.decode(readPayload(e.id))?.uri }.getOrNull()
        }

    private fun deleteCachedImageFiles(entries: Collection<Uri>) {
        entries.forEach { uri ->
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
