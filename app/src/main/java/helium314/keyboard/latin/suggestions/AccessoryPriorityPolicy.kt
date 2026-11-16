// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Provider-owned credentials take precedence until the provider closes or focus changes. */
object AccessoryPriorityPolicy {
    enum class Incoming { WORDS, CLIPBOARD, INLINE_AUTOFILL }

    @JvmStatic
    fun canReplaceVisible(providerActive: Boolean, incoming: Incoming): Boolean =
        !providerActive || incoming == Incoming.INLINE_AUTOFILL

    @JvmStatic
    fun responseBelongsToVisibleEditor(requestEpoch: Long, editorEpoch: Long, inputVisible: Boolean): Boolean =
        inputVisible && requestEpoch >= 0 && requestEpoch == editorEpoch
}
