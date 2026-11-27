// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

/** The only animated panel route is typing <-> Emoji Browse. */
public final class PanelMotionPolicy {
    private PanelMotionPolicy() {}
    public static boolean canMorph(boolean allowed, boolean toEmoji, boolean fromEmoji,
            boolean touchesClipboard, boolean motionAllowed) {
        return allowed && !touchesClipboard && motionAllowed && (toEmoji || fromEmoji);
    }
    public static boolean supportsArtwork(boolean typing,boolean emoji,boolean clipboard) {
        return !clipboard && (typing || emoji);
    }
    public static boolean canPrewarm(boolean lowRam,boolean powerSaver) {
        return !lowRam && !powerSaver;
    }
}
