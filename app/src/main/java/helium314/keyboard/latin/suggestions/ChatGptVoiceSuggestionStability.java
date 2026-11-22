// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import android.view.inputmethod.EditorInfo;

/** Volatile counters and a short debounce for ChatGPT's host-driven voice transcript stream. */
public final class ChatGptVoiceSuggestionStability {
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private static final long LOCAL_INPUT_GRACE_MS = 450L;
    public static final long SETTLE_MS = 260L;
    private static long lastKeyboardInputMs = -1L;
    private static long holdUntilMs;
    private static long hostEditCount;
    private static long deferredSuggestionCount;

    private ChatGptVoiceSuggestionStability() { }

    public static synchronized void noteKeyboardInput(long nowMs) {
        lastKeyboardInputMs = nowMs;
    }

    /** Detect only cursor-at-end text growth in ChatGPT without a recent keyboard event. */
    public static synchronized boolean noteSelectionUpdate(EditorInfo editor,
            int oldSelectionEnd, int newSelectionStart, int newSelectionEnd, long nowMs) {
        if (editor == null || !CHATGPT_PACKAGE.equals(editor.packageName)
                || newSelectionStart < 0 || newSelectionStart != newSelectionEnd
                || newSelectionEnd <= oldSelectionEnd
                || (lastKeyboardInputMs >= 0 && nowMs - lastKeyboardInputMs < LOCAL_INPUT_GRACE_MS)) {
            return false;
        }
        hostEditCount++;
        holdUntilMs = Math.max(holdUntilMs, nowMs + SETTLE_MS);
        return true;
    }

    public static synchronized boolean shouldHold(EditorInfo editor, long nowMs) {
        return editor != null && CHATGPT_PACKAGE.equals(editor.packageName) && nowMs < holdUntilMs;
    }

    public static synchronized long remainingHoldMs(long nowMs) {
        return Math.max(0L, holdUntilMs - nowMs);
    }

    public static synchronized void recordDeferredSuggestion() {
        deferredSuggestionCount++;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(hostEditCount, deferredSuggestionCount);
    }

    public static synchronized void reset() {
        lastKeyboardInputMs = -1L;
        holdUntilMs = 0L;
        hostEditCount = 0L;
        deferredSuggestionCount = 0L;
    }

    public static final class Snapshot {
        public final long hostEditCount;
        public final long deferredSuggestionCount;

        private Snapshot(long hostEditCount, long deferredSuggestionCount) {
            this.hostEditCount = hostEditCount;
            this.deferredSuggestionCount = deferredSuggestionCount;
        }
    }
}
