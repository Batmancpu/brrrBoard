// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Provider-owned credentials take precedence until the provider closes or focus changes. */
object AccessoryPriorityPolicy {
    enum class Incoming { WORDS, CLIPBOARD, INLINE_AUTOFILL }
    enum class CompactOwner { NONE, CLIPBOARD, EDITOR }

    /** Full paste/provider content and explicit toolbar states never share the compact slot. */
    fun compactOwner(show: Boolean, fits: Boolean, external: Boolean,
                     clipboard: Boolean, editor: Boolean): CompactOwner = when {
        !show || !fits || external -> CompactOwner.NONE
        clipboard -> CompactOwner.CLIPBOARD
        editor -> CompactOwner.EDITOR
        else -> CompactOwner.NONE
    }

    /** Elapsed realtime is independent of wall-clock changes and delayed UI callbacks. */
    fun actionIsFresh(now: Long, deadline: Long): Boolean = deadline > 0 && now >= 0 && now < deadline

    /** Explicit toolbar-only mode is a deliberate visible row, not a prediction. */
    @JvmStatic
    fun collapseUnusedHeader(wordsEnabled: Boolean, appCompletions: Boolean,
                             externalContent: Boolean, explicitToolbar: Boolean): Boolean =
        !wordsEnabled && !appCompletions && !externalContent && !explicitToolbar

    @JvmStatic
    fun canReplaceVisible(providerActive: Boolean, incoming: Incoming): Boolean =
        !providerActive || incoming == Incoming.INLINE_AUTOFILL

    @JvmStatic
    fun renderBelongsToVisibleEditor(providerActive: Boolean, requestEpoch: Long, editorEpoch: Long,
                                     requestGeneration: Long, activeGeneration: Long, inputVisible: Boolean): Boolean =
        providerActive && requestGeneration == activeGeneration &&
            responseBelongsToVisibleEditor(requestEpoch,editorEpoch,inputVisible)

    @JvmStatic
    fun responseBelongsToVisibleEditor(requestEpoch: Long, editorEpoch: Long, inputVisible: Boolean): Boolean =
        inputVisible && requestEpoch >= 0 && requestEpoch == editorEpoch
}
