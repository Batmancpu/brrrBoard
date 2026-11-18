// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

/** Session boundary stored in IME memory only; never stores app IDs or text. */
public final class SwipeContextWindow {
    private SwipeContextWindow() {}
    public static final long IDLE_RESET_MS = 5L * 60L * 1000L;
    public static boolean expired(final long lastInteractionUptime, final long nowUptime) {
        return lastInteractionUptime > 0
                && (nowUptime < lastInteractionUptime
                    || nowUptime - lastInteractionUptime > IDLE_RESET_MS);
    }
}
