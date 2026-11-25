// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Provider-owned credentials take precedence until the provider closes or focus changes. */
object AccessoryPriorityPolicy {
    enum class Incoming { WORDS, CLIPBOARD, INLINE_AUTOFILL }

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
