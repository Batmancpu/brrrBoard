// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

/** Editor-scoped, content-free state for coalescing rapid backspaces. UI thread only. */
final class BackspaceSuggestionHold {
    static final long SETTLE_MS = 185L;
    private long generation = -1L;

    void noteDelete(long editorGeneration) { generation = editorGeneration; }
    boolean isActive(long editorGeneration) { return generation != -1L && generation == editorGeneration; }
    void clear() { generation = -1L; }
}
