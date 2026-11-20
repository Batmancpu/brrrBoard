// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

/** Content-free policy shared by direct Paste and history's final commit gate. */
public final class ClipboardPasteSafety {
    public enum Action { INSERT, CONFIRM, REJECT }

    private ClipboardPasteSafety() {}

    public static Action action(final boolean hasText, final boolean hasEditor,
            final boolean privateEditor, final boolean sensitiveClipboard) {
        if (!hasText || !hasEditor) return Action.REJECT;
        return privateEditor || sensitiveClipboard ? Action.CONFIRM : Action.INSERT;
    }

    /** An old receipt or a clipboard replaced by another app must never be cleared. */
    public static boolean mayClearOwned(final boolean userRequestedClear,
            final boolean sameClipboardFingerprintAndTimestamp) {
        return userRequestedClear && sameClipboardFingerprintAndTimestamp;
    }
}
