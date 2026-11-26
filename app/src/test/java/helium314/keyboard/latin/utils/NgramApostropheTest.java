// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NgramApostropheTest {
    @Test public void internalCurlyApostrophesNormalizeOnlyInLookupCopy() {
        assertEquals("don't", NgramContextUtils.normalizeInternalApostrophes("don’t"));
        assertEquals("I'm", NgramContextUtils.normalizeInternalApostrophes("I’m"));
        assertEquals("’quote’", NgramContextUtils.normalizeInternalApostrophes("’quote’"));
        assertEquals("dogs’", NgramContextUtils.normalizeInternalApostrophes("dogs’"));
        assertEquals("a’’b", NgramContextUtils.normalizeInternalApostrophes("a’’b"));
        assertEquals("α’β", NgramContextUtils.normalizeInternalApostrophes("α’β"));
        assertEquals("word", NgramContextUtils.normalizeInternalApostrophes("word"));
    }
}
