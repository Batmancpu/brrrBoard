package helium314.keyboard.latin.settings

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class TextTemplateCipherTest {
    @Test fun authenticatedRoundTripAndTamperRejection() {
        val key = SecretKeySpec(ByteArray(32) { it.toByte() },"AES")
        val plain = "sample snippet".toByteArray()
        val one = TextTemplateStore.seal(key,plain)
        val two = TextTemplateStore.seal(key,plain)
        assertArrayEquals(plain,TextTemplateStore.open(key,one))
        assertFalse(one.contentEquals(two))
        one[one.lastIndex] = (one.last().toInt() xor 1).toByte()
        try { TextTemplateStore.open(key,one); fail("Tampered ciphertext accepted") }
        catch (_: java.security.GeneralSecurityException) { }
    }
}
