// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Emoji page/scroll memory is scoped to one editor window session. */
final class EmojiNavigationSession {
    private boolean resetPending = true;

    void beginNewEditorSession() { resetPending = true; }

    /** Consume exactly once; ABC/Emoji toggles in the same session retain position. */
    boolean consumeResetOnOpen() {
        final boolean reset = resetPending;
        resetPending = false;
        return reset;
    }
}
