// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlinx.coroutines.CancellationException

/** Contains recoverable dictionary failures and always releases the owning request slot. */
internal inline fun <T> runDictionarySuggestionQuery(
    query: () -> T,
    onFailure: (Exception) -> Unit,
    onComplete: () -> Unit
): T? {
    try {
        return query()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFailure(e)
        return null
    } finally {
        onComplete()
    }
}
