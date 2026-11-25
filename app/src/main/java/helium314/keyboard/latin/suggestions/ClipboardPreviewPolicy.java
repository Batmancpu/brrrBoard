// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;
import java.net.URI;

/** Presentation only: never pass a preview to a paste or navigation callback. */
public final class ClipboardPreviewPolicy {
    private ClipboardPreviewPolicy() {}
    public static String label(String original) {
        if (original == null || original.isEmpty()) return "";
        if (original.length() <= 4096) {
            try {
                URI uri = new URI(original);
                if (uri.getHost() != null && uri.getRawUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                    String host = uri.getHost();
                    if (host.regionMatches(true, 0, "www.", 0, 4)) host = host.substring(4);
                    return host + (uri.getPort() >= 0 ? ":" + uri.getPort() : "");
                }
            } catch (Exception ignored) {}
        }
        int count = original.codePointCount(0, original.length());
        int length = Math.min(count, 120);
        StringBuilder out = new StringBuilder();
        for (int offset = 0, i = 0; offset < original.length() && i < length; i++) {
            int cp = original.codePointAt(offset);
            offset += Character.charCount(cp);
            if (Character.getType(cp) == Character.FORMAT && cp != 0x200D && cp != 0x200C) continue;
            if (Character.isWhitespace(cp) || Character.isISOControl(cp)) out.append(' ');
            else out.appendCodePoint(cp);
        }
        return out.toString().trim() + (length < count ? "…" : "");
    }
}
