package helium314.keyboard.latin.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class ClipboardTextVaultTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() },"AES")
    @Test fun authenticatedRowBindingAndRandomNonces() {
        val a = ClipboardTextVault.seal(key,2,"hello 🙂")
        val b = ClipboardTextVault.seal(key,2,"hello 🙂")
        assertFalse(a.contentEquals(b))
        assertEquals("hello 🙂",ClipboardTextVault.open(key,2,a))
        try { ClipboardTextVault.open(key,3,a); fail("row swap accepted") }
        catch (_: java.security.GeneralSecurityException) {}
        a[a.lastIndex]=(a.last().toInt() xor 1).toByte()
        try { ClipboardTextVault.open(key,2,a); fail("tampering accepted") }
        catch (_: java.security.GeneralSecurityException) {}
    }

    @Test fun migrationPreservesIdsPinsAndTimestamps() {
        SQLiteDatabase.create(null).use { db ->
            db.execSQL(ClipboardDao.CREATE_TABLE)
            db.execSQL("INSERT INTO CLIPBOARD VALUES (4,123,1,'pinned')")
            db.execSQL("INSERT INTO CLIPBOARD VALUES (8,456,0,'ordinary')")
            ClipboardTextVault.migrate(db,key)
            assertEquals(ClipboardTextVault.Status.CLEANUP_PENDING,ClipboardTextVault.status(db))
            db.rawQuery("SELECT ID,TIMESTAMP,PINNED,TEXT FROM CLIPBOARD ORDER BY ID",null).use { c ->
                assertTrue(c.moveToNext())
                assertEquals(4L,c.getLong(0))
                assertEquals(123L,c.getLong(1))
                assertEquals(1,c.getInt(2))
                assertEquals(Cursor.FIELD_TYPE_BLOB,c.getType(3))
                assertEquals("pinned",ClipboardTextVault.read(c,3,4,key))
                assertTrue(c.moveToNext())
                assertEquals(8L,c.getLong(0))
                assertEquals(456L,c.getLong(1))
                assertEquals(0,c.getInt(2))
                assertEquals("ordinary",ClipboardTextVault.read(c,3,8,key))
                assertFalse(c.moveToNext())
            }
        }
    }

    @Test fun failedMigrationRollsBackPreviouslyConvertedRows() {
        SQLiteDatabase.create(null).use { db ->
            db.execSQL(ClipboardDao.CREATE_TABLE)
            for (id in 1..16)
                db.execSQL("INSERT INTO CLIPBOARD VALUES ($id,100,1,'keep')")
            db.execSQL("INSERT INTO CLIPBOARD VALUES (17,100,1,NULL)")
            try { ClipboardTextVault.migrate(db,key); fail("invalid record accepted") }
            catch (_: IllegalStateException) {}
            assertEquals(ClipboardTextVault.Status.OFF,ClipboardTextVault.status(db))
            db.rawQuery("SELECT TEXT FROM CLIPBOARD WHERE ID=1",null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(Cursor.FIELD_TYPE_STRING,c.getType(0))
                assertEquals("keep",c.getString(0))
            }
        }
    }
}
