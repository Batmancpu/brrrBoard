// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * A final, conservative guard on the existing decoder's auto-correction decision.
 *
 * The native dictionaries remain the sole source of correction and next-word
 * candidates. This protects deliberate identifier-like input without creating
 * a second predictor, changing dictionary scores or hiding manual suggestions.
 */
internal object SmartCorrectionSafety {
    fun permitsAutomaticReplacement(typed: String): Boolean {
        if (typed.isEmpty()) return true
        // The existing InputAttributes and Suggest checks remain authoritative
        // for editor type, digits, caps and decoder confidence. These are only
        // strong signals of explicitly structured input.
        if (typed.any { it == '@' || it == '/' || it == '\\' || it == '_' ||
                    it == ':' || it == '#' || it == '=' }) return false
        if (typed.contains('.')) return false

        // A manually composed internal uppercase is often a name or symbol.
        // The first character may still be capitalized by normal sentence case.
        if (typed.drop(1).any { it.isUpperCase() }) return false
        return true
    }
}
