// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

/**
 * Interprets a selection reported by a host after Shift+DPAD movement.
 * A successful key-event dispatch is not proof that the editor moved its caret.
 */
final class TrackpadHostSelection {
    static final int WAITING = -2;
    static final int INVALID = -1;

    private TrackpadHostSelection() { }

    static int confirmedHead(int anchor, int previousHead, int start, int end) {
        if (anchor < 0 || previousHead < 0 || start < 0 || end < start) return INVALID;
        if (start == Math.min(anchor, previousHead)
                && end == Math.max(anchor, previousHead)) return WAITING;
        if (start == anchor) return end;
        if (end == anchor) return start;
        // The host selected a different range or editor; never guess its anchor.
        return INVALID;
    }
}
