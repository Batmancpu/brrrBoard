package helium314.keyboard.latin.database

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardPortableBackupTest {
    @Test fun roundTripPreservesPinAndUnicode() {
        val entries = listOf(ClipboardPortableBackup.Entry(123,true,"hello 🙂\nworld"))
        assertEquals(entries,ClipboardPortableBackup.decode(ClipboardPortableBackup.encode(entries).inputStream()))
    }
    @Test fun rejectsSerializedImageAndEmptyRows() {
        assertFalse(ClipboardPortableBackup.valid(listOf(ClipboardPortableBackup.Entry(1,true,"\u0001image\tcontent://other/secret\timage/png\tx"))))
        assertFalse(ClipboardPortableBackup.valid(listOf(ClipboardPortableBackup.Entry(1,false,""))))
        assertFalse(ClipboardPortableBackup.valid(listOf(ClipboardPortableBackup.Entry(-1,false,"x"))))
    }
    @Test fun rejectsUnknownVersion() {
        try { ClipboardPortableBackup.decode("""{"version":2,"entries":[]}""".byteInputStream()); fail("future version accepted") }
        catch (_: IllegalArgumentException) {}
    }
}
