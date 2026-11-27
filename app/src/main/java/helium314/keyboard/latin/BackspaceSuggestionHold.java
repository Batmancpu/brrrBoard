// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

/** Editor-scoped, content-free state for coalescing rapid backspaces. UI thread only. */
final class BackspaceSuggestionHold {
    static final long SETTLE_MS = 185L;
    static final long RECOVERY_GRACE_MS = 120L;
    private long generation = -1L;
    private long recoveryGeneration = -1L;

    void noteDelete(long editorGeneration) {
        generation = editorGeneration;
        recoveryGeneration = editorGeneration;
    }
    boolean isActive(long editorGeneration) { return generation != -1L && generation == editorGeneration; }
    // New input cancels coalescing, but an inert row still needs a bounded recovery.
    void clear() { generation = -1L; }
    boolean canRecover(long editorGeneration, boolean heldFrame) {
        return heldFrame && !isActive(editorGeneration)
                && recoveryGeneration != -1L && recoveryGeneration == editorGeneration;
    }
    void clearRecovery() { recoveryGeneration = -1L; }
}
