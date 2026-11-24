package helium314.keyboard.latin
import org.junit.Assert.*
import org.junit.Test

class ClipboardEntityExtractorTest {
    @Test fun explicitCodeAndOffsets() {
        val text = "OTP: 125789; email demo@example.org"
        val results = ClipboardEntityExtractor.extract(text)
        assertEquals(listOf("125789", "demo@example.org"), results.map { text.substring(it.start,it.end) })
        assertTrue(ClipboardEntityExtractor.excludeFromAutomaticHistory(text))
        assertTrue(ClipboardEntityExtractor.excludeFromAutomaticHistory("123456"))
        assertFalse(ClipboardEntityExtractor.excludeFromAutomaticHistory("Meeting at 123456 Main Street"))
    }
    @Test fun credentialBearingUrlsAndKeysNeverEnterAutomaticHistory() {
        listOf("https://demo:secret@example.org/page",
            "https://example.org/?access_token=opaque",
            "https://example.org/?%74oken=opaque",
            "Bearer abcdefghijk",
            "-----BEGIN PRIVATE KEY-----").forEach {
            assertTrue(ClipboardEntityExtractor.excludeFromAutomaticHistory(it))
            assertTrue(ClipboardEntityExtractor.extract(it).isEmpty())
        }
        assertFalse(ClipboardEntityExtractor.excludeFromAutomaticHistory("https://example.org/?page=2"))
    }
    @Test fun boundedAndNoCredentialUrls() {
        assertTrue(ClipboardEntityExtractor.extract("a".repeat(4097)).isEmpty())
        assertTrue(ClipboardEntityExtractor.extract("password: abc123").isEmpty())
        assertTrue(ClipboardEntityExtractor.extract("https://example.org/?token=secret").none { it.kind == ClipboardEntityExtractor.Kind.LINK })
        assertEquals(1, ClipboardEntityExtractor.extract("https://example.org/page").size)
        assertTrue(ClipboardEntityExtractor.excludeFromAutomaticHistory("4111 1111 1111 1111"))
        assertFalse(ClipboardEntityExtractor.excludeFromAutomaticHistory("hello world"))
    }
}
