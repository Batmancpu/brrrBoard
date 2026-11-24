// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import helium314.keyboard.latin.utils.prefs

/** Settings for microphone hold behavior; both defaults preserve the requested workflow. */
object VoiceGesturePreferences {
    const val HOLD_TO_TALK = "brr_voice_hold_to_talk_v1"
    const val AUTO_SEND_HOLD = "brr_voice_hold_auto_send_v1"
    const val MIC_DOUBLE_TAP_SWITCH = "brr_voice_mic_double_tap_switch_v1"

    fun isMicDoubleTapSwitchEnabled(context: Context): Boolean =
        context.prefs().getBoolean(MIC_DOUBLE_TAP_SWITCH, false)

    fun isHoldToTalkEnabled(context: Context): Boolean = context.prefs().getBoolean(HOLD_TO_TALK, true)
    fun isHoldAutoSendEnabled(context: Context): Boolean = context.prefs().getBoolean(AUTO_SEND_HOLD, true)
}
