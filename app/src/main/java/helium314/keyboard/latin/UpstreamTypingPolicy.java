// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

/** Local adaptation of upstream caps and learning fixes; no new history storage. */
public final class UpstreamTypingPolicy {
    private UpstreamTypingPolicy() {}

    public static boolean preserveSwipeShift(int keyboardShift, int autoCaps) {
        return keyboardShift == WordComposer.CAPS_MODE_MANUAL_SHIFTED
                || keyboardShift == WordComposer.CAPS_MODE_MANUAL_SHIFT_LOCKED
                || ((keyboardShift == WordComposer.CAPS_MODE_OFF) != (autoCaps == 0));
    }

    public static boolean mayLearnCapitalization(boolean automaticallyCapitalized,
            boolean beginningOfSentence) {
        return !automaticallyCapitalized || beginningOfSentence;
    }
}
