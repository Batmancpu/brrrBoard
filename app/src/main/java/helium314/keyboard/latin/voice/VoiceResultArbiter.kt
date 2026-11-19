// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** Only one source may insert text for a dictation, regardless of callback order. */
internal class VoiceResultArbiter {
    enum class Winner { SERVER, OFFLINE }
    var winner: Winner? = null
        private set

    @Synchronized fun acceptServer(): Boolean = select(Winner.SERVER)
    @Synchronized fun acceptOffline(): Boolean = select(Winner.OFFLINE)

    private fun select(source: Winner): Boolean {
        if (winner != null) return false
        winner = source
        return true
    }
}
