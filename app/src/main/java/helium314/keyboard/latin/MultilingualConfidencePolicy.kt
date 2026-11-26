// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Confidence changes only when one active production main dictionary uniquely supports a word. */
internal object MultilingualConfidencePolicy {
    fun uniqueLanguageIndex(validInMainDictionaries: BooleanArray): Int {
        var match = -1
        for (index in validInMainDictionaries.indices) {
            if (!validInMainDictionaries[index]) continue
            if (match >= 0) return -1
            match = index
        }
        return match
    }
}
