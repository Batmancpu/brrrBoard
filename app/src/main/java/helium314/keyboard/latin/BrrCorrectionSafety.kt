// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.latin.dictionary.BrrPersonalDictionary

/**
 * B16A: keep a deliberate correction target out of the valid-only seed.
 *
 * Such terms are valid when explicitly typed, but must never silently replace
 * a *different* token. This leaves ranking, ordinary suggestions, tap and
 * gesture decoders, locale data, and undo unchanged.
 */
object BrrCorrectionSafety {
    @JvmStatic
    fun blocksCandidate(candidate: String?): Boolean =
        candidate != null && BrrPersonalDictionary.isNoSuggestionWord(candidate)
}
