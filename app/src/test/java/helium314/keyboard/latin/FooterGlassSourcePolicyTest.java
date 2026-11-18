// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;
import org.junit.Test;
import static org.junit.Assert.*;
public class FooterGlassSourcePolicyTest {
    @Test public void browseRetainsLiveGlassButSearchNeverBlursTextBehindMic() {
        assertTrue(FooterGlassSourcePolicy.shouldSampleClipboard(true, false));
        assertFalse(FooterGlassSourcePolicy.shouldSampleClipboard(true, true));
        assertFalse(FooterGlassSourcePolicy.shouldSampleClipboard(false, false));
    }
}
