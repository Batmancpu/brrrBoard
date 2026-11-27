// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Search-only gesture ownership; a new results generation cannot inherit an old DOWN. */
final class SearchResultTouchGate {
    private boolean managed,pending,active;
    boolean managed() { return managed; }
    void setPending(boolean next) {
        managed=true;
        pending=next;
        if (next) active=false;
    }
    boolean begin() {
        if (!managed) return true;
        active=!pending;
        return active;
    }
    boolean continuationAllowed() { return !managed || (active && !pending); }
    void invalidate() { active=false; }
}
