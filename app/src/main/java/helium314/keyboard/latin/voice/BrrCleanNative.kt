// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** JNI is loaded only by BrrCleanVoiceService, never by LatinIME or a UI process. */
internal object BrrCleanNative {
    init { System.loadLibrary("brrclean") }

    @JvmStatic external fun process(pcm: ByteArray, weights: ByteArray): ByteArray?
}
