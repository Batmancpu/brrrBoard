package helium314.keyboard.latin.settings

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class TextReplacementCipherTest {
    @Test fun purposeSeparationAndTamperRejection() {
        val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
        val plain = "example rule".toByteArray()
        val sealed = TextReplacementStore.seal(key,plain)
        assertArrayEquals(plain,TextReplacementStore.open(key,sealed))
        try { TextTemplateStore.open(key,sealed); fail("Wrong purpose accepted") }
        catch (_: java.security.GeneralSecurityException) { }
        sealed[sealed.lastIndex] = (sealed.last().toInt() xor 1).toByte()
        try { TextReplacementStore.open(key,sealed); fail("Tampered ciphertext accepted") }
        catch (_: java.security.GeneralSecurityException) { }
    }
}
