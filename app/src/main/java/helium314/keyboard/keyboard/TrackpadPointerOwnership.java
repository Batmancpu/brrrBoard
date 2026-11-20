// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

/** Single-owner, content-free pointer state for the spacebar trackpad. */
final class TrackpadPointerOwnership {
    private int primary = -1;
    private int secondary = -1;

    void activate(int primaryId) {
        primary = primaryId;
        secondary = -1;
    }

    boolean isPrimary(int id) { return primary >= 0 && id == primary; }

    boolean isSecondary(int id) { return secondary >= 0 && id == secondary; }

    boolean isSelecting() { return secondary >= 0; }

    /** True only for the finger that established the selection. */
    boolean addSecondary(int id) {
        if (primary < 0 || id == primary || secondary >= 0) return false;
        secondary = id;
        return true;
    }

    boolean removeSecondary(int id) {
        if (secondary != id) return false;
        secondary = -1;
        return true;
    }

    void clear() {
        primary = -1;
        secondary = -1;
    }
}
