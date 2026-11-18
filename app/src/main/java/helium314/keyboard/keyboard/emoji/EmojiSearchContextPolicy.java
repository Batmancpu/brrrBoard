// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/**
 * Bounds the only editor text used by the Search-open recommendation row.
 * Caller must first check EmojiContextEligibility; this class further rejects
 * identifiers, numeric input and URI-like text instead of interpreting it.
 */
final class EmojiSearchContextPolicy {
    private static final int MAX_CHARS = 160;

    private EmojiSearchContextPolicy() { }

    static String safeSuffix(final CharSequence beforeCursor) {
        if (beforeCursor == null || beforeCursor.length() == 0
                || beforeCursor.length() > 4096) return "";
        final String content = beforeCursor.toString();
        final String suffix = content.substring(Math.max(0, content.length() - MAX_CHARS));
        for (int i = 0; i < suffix.length(); i++) {
            final char c = suffix.charAt(i);
            if (Character.isDigit(c) || c == '@' || c == '/' || c == '\\'
                    || c == '=' || c == ':') return "";
        }
        final int cps = suffix.codePointCount(0, suffix.length());
        final int keep = EmojiDescribeNormalizer.MAX_QUERY_CODE_POINTS;
        return cps <= keep ? suffix
                : suffix.substring(suffix.offsetByCodePoints(0, cps - keep));
    }
}
