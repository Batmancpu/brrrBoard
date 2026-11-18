// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;
/** The Clipboard Browse glass may sample cards, but Search footer must not. */
public final class FooterGlassSourcePolicy {
    private FooterGlassSourcePolicy() {}
    public static boolean shouldSampleClipboard(boolean clipboardPanel, boolean searchActive) {
        return clipboardPanel && !searchActive;
    }
}
