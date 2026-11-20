// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** No decoder callback may silently replace text at a stale destination. */
internal object VoiceTranscriptDeliveryPolicy {
    enum class Outcome { INSERT, RECOVER, DROP }

    fun choose(detached: Boolean, sameEditor: Boolean, sameSelection: Boolean,
               validTranscript: Boolean): Outcome = when {
        !validTranscript -> Outcome.DROP
        !detached && sameEditor && sameSelection -> Outcome.INSERT
        else -> Outcome.RECOVER
    }
}
