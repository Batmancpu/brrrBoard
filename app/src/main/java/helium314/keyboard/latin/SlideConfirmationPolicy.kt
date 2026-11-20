// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** A completed intentional left-to-right drag is the only destructive confirmation. */
object SlideConfirmationPolicy {
    const val REQUIRED_PROGRESS = .92f

    @JvmStatic fun progress(dragX: Float, startX: Float, travel: Float): Float =
        if (travel <= 0f) 0f else ((dragX - startX) / travel).coerceIn(0f, 1f)

    @JvmStatic fun approved(startedOnThumb: Boolean, released: Boolean,
                            cancelled: Boolean, progress: Float): Boolean =
        startedOnThumb && released && !cancelled && progress >= REQUIRED_PROGRESS
}
