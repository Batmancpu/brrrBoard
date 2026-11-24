// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.UserManager
import androidx.core.database.sqlite.transaction
import helium314.keyboard.latin.utils.GestureDataDao
import helium314.keyboard.latin.utils.Log
import java.io.File

class Database private constructor(context: Context, name: String = NAME) : SQLiteOpenHelper(context, name, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ClipboardDao.CREATE_TABLE)
        db.execSQL(KlipyHistoryDao.CREATE_TABLE)
        onUpgrade(db, 0, VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 1) {
            db.execSQL(GestureDataDao.CREATE_TABLE)
        }
        if (oldVersion <= 2) {
            db.execSQL(KlipyHistoryDao.CREATE_TABLE)
        }
        if (oldVersion <= 3) {
            addColumnIfMissing(db, KlipyHistoryDao.TABLE_NAME, KlipyHistoryDao.COLUMN_PREVIEW_URL, "TEXT")
        }
        if (oldVersion <= 4) {
            addColumnIfMissing(db, KlipyHistoryDao.TABLE_NAME, KlipyHistoryDao.COLUMN_PINNED, "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, KlipyHistoryDao.TABLE_NAME, KlipyHistoryDao.COLUMN_PINNED_TIMESTAMP, "INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion <= 5) {
            // Delete obsolete gesture diagnostics from old installations.
            // Keep the legacy table for downgraded/database-import compatibility.
            db.execSQL("DELETE FROM GESTURE_DATA")
        }
    }

    private fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, definition: String) {
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == column) return
            }
        }
        db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }

    companion object {
        private val TAG = Database::class.java.simpleName
        private const val VERSION = 6
        const val NAME = "heliboard.db"
        private var instance: Database? = null
        fun getInstance(context: Context): Database {
            if (instance == null) {
                val storageContext = credentialStorageContext(context)
                migrateLegacyDeviceProtectedDatabase(storageContext)
                instance = Database(storageContext)
            }
            return instance!!
        }

        /** Clipboard text is sensitive and must not be read or created before first unlock. */
        private fun credentialStorageContext(context: Context): Context {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context
            val userManager = context.getSystemService(UserManager::class.java)
                ?: throw IllegalStateException("User storage state unavailable")
            check(userManager.isUserUnlocked) { "Clipboard history is unavailable before first unlock" }
            return context.applicationContext
        }

        /** Move the old Direct-Boot database only after the user unlocks the device. */
        private fun migrateLegacyDeviceProtectedDatabase(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            val sourceContext = context.createDeviceProtectedStorageContext()
            val source = sourceContext.getDatabasePath(NAME)
            if (!source.isFile) return
            val destination = context.getDatabasePath(NAME)
            if (destination.exists()) {
                // Never replace or merge two independently-mutated history stores.
                throw IllegalStateException("Both legacy and credential clipboard databases exist")
            }
            check(context.moveDatabaseFrom(sourceContext, NAME)) {
                "Could not migrate legacy clipboard history"
            }
        }

        /** Legacy portable SQLite backup. Read and validate all text before touching live history. */
        fun copyFromDb(file: File, context: Context) {
            if (!file.exists()) return
            try {
                val rows = SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY).use { source ->
                    val key = if (ClipboardTextVault.enabled(source)) ClipboardTextVault.key(context,false) else null
                    source.rawQuery("SELECT ID,TIMESTAMP,PINNED,TEXT FROM CLIPBOARD ORDER BY ID",null).use { c ->
                        buildList {
                            var chars = 0L
                            while (c.moveToNext()) {
                                val text = ClipboardTextVault.read(c,3,c.getLong(0),key)
                                // Old backup formats never carried the actual image bytes.
                                if (helium314.keyboard.latin.ClipboardImageHistoryClip.hasImageHistoryPrefix(text)) continue
                                chars += text.length
                                require(size < 10_000 && chars <= 8*1024*1024)
                                add(ClipboardPortableBackup.Entry(c.getLong(1),c.getInt(2)!=0,text))
                            }
                        }
                    }
                }
                require(ClipboardPortableBackup.valid(rows))
                checkNotNull(ClipboardDao.getInstance(context)).mergePortableText(rows)
                // Never import gesture data.
            } finally { file.delete() }
        }
    }
}
