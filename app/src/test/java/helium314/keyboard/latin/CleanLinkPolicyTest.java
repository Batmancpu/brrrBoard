package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.*;

public class CleanLinkPolicyTest {
    @Test public void trackerRemovalPreservesRawValues() {
        String input = "https://example.com/a%2Fb?q=one+two&q=%2b&UTM_SOURCE=x&fbclid=x#part";
        assertEquals("https://example.com/a%2Fb?q=one+two&q=%2b#part", CleanLinkPolicy.cleanForNavigation(input));
        assertEquals("https://example.com/?id=7", CleanLinkPolicy.cleanForNavigation("https://example.com/?%75tm_source=x&id=7"));
        assertEquals("https://example.com/", CleanLinkPolicy.cleanForNavigation("https://example.com/?gclid=x"));
        assertEquals("https://example.com/?ref=a&source=b&si=c&t=5", CleanLinkPolicy.cleanForNavigation("https://example.com/?ref=a&source=b&si=c&t=5"));
    }
    @Test public void domainRulesAreScoped() {
        assertEquals("https://www.youtube.com/watch?v=xyz&t=10", CleanLinkPolicy.cleanForNavigation("https://www.youtube.com/watch?v=xyz&si=abc&t=10"));
        assertEquals("https://youtube.com.example.com/?si=abc", CleanLinkPolicy.cleanForNavigation("https://youtube.com.example.com/?si=abc"));
        assertEquals("https://instagram.com/reel/ABC/", CleanLinkPolicy.cleanForNavigation("https://instagram.com/reel/ABC/?igsh=123"));
        assertEquals("https://open.spotify.com/track/abc", CleanLinkPolicy.cleanForNavigation("https://open.spotify.com/track/abc?si=123"));
    }
    @Test public void unsafeOrSensitiveLinksAreRefused() {
        for (String url : new String[]{"javascript:alert(1)", "https://user@example.com/",
            "https://example.com/?signature=abc&utm_source=x", "https://example.com/?%74oken=x",
            "https://example.com/oauth/authorize?a=1", "https://example.com/#access_token=abc",
            "http://127.0.0.1/", "http://localhost/", "https://example.local/", "https://example.com/?q=%ZZ",
            "https://example.com/?q=x;token=abc", "https://example.com/\\evil", "https://example.com/ hello"})
            assertNull(url, CleanLinkPolicy.cleanForNavigation(url));
    }
    @Test public void exactTenSecondWindow() {
        assertEquals(10000L, CleanLinkPolicy.remaining(100L,100L));
        assertEquals(1L, CleanLinkPolicy.remaining(100L,10099L));
        assertEquals(0L, CleanLinkPolicy.remaining(100L,10100L));
        assertEquals(0L, CleanLinkPolicy.remaining(0L,100L));
        assertEquals(0L, CleanLinkPolicy.remaining(101L,100L));
    }
    @Test public void idempotentAndPreservesUnknown() {
        String x = "https://example.com/path?tag=x&redirect=https%3A%2F%2Fother.example.com%2F%3Futm_source%3Dx&utm_campaign=x";
        String y = CleanLinkPolicy.cleanForNavigation(x);
        assertEquals("https://example.com/path?tag=x&redirect=https%3A%2F%2Fother.example.com%2F%3Futm_source%3Dx", y);
        assertEquals(y,CleanLinkPolicy.cleanForNavigation(y));
    }
}
