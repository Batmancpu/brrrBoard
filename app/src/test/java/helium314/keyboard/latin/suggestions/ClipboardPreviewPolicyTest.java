package helium314.keyboard.latin.suggestions;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClipboardPreviewPolicyTest {
    @Test public void hostOnly() {
        String link = "https://www.youtube.com/watch?v=123&si=xyz&utm_source=foo";
        assertEquals("youtube.com", ClipboardPreviewPolicy.label(link));
        assertEquals("https://www.youtube.com/watch?v=123&si=xyz&utm_source=foo", link);
        assertEquals("youtube.com.example.org", ClipboardPreviewPolicy.label("https://youtube.com.example.org/?si=x"));
        assertEquals("example.org:8443", ClipboardPreviewPolicy.label("https://example.org:8443/a"));
    }
    @Test public void oneLineAndUnicode() {
        assertEquals("hello world", ClipboardPreviewPolicy.label("hello\nworld"));
        assertEquals("ab", ClipboardPreviewPolicy.label("a\u202eb"));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 121; i++) b.append("😀");
        String preview = ClipboardPreviewPolicy.label(b.toString());
        assertEquals(121, preview.codePointCount(0, preview.length()));
        assertTrue(preview.endsWith("…"));
    }
}
