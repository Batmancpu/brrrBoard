package helium314.keyboard.latin.database

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class ClipboardImageVaultTest {
    private val key=SecretKeySpec(ByteArray(32){ 5 },"AES")
    private val name="clip_123_0123456789abcdef.png"
    @Test fun authenticatesBytesAndFilename() {
        val bytes=byteArrayOf(1,2,3,4,5)
        val one=ClipboardImageVault.seal(key,name,bytes)
        val two=ClipboardImageVault.seal(key,name,bytes)
        assertFalse(one.contentEquals(two))
        assertArrayEquals(bytes,ClipboardImageVault.open(key,name,one))
        try { ClipboardImageVault.open(key,"clip_124_0123456789abcdef.png",one); fail("filename not bound") }
        catch (_: java.security.GeneralSecurityException) {}
        one[one.lastIndex]=(one.last().toInt() xor 1).toByte()
        try { ClipboardImageVault.open(key,name,one); fail("tampering accepted") }
        catch (_: java.security.GeneralSecurityException) {}
    }
    @Test fun refusesTraversalAndTemporaryNames() {
        assertTrue(ClipboardImageVault.validName(name))
        assertFalse(ClipboardImageVault.validName("../"+name))
        assertFalse(ClipboardImageVault.validName(name+".new"))
    }
}
