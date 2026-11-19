// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** One ephemeral action at a time. Old Clipboard/Dictate callbacks cannot revoke a newer one. */
object FooterContextActionPolicy {
    @JvmStatic fun mayClear(requestingOwner: Any?, activeOwner: Any?): Boolean =
        requestingOwner == null || requestingOwner === activeOwner

    @JvmStatic fun durationMs(requested: Long): Long = requested.coerceIn(2_000L, 10_000L)
}
