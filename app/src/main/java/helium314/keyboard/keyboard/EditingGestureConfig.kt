// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.SharedPreferences
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** An explicitly enabled, dedicated origin only. Never arbitrate ordinary glide paths. */
data class EditingGestureConfig(val enabled: Boolean, val mode: Mode,
    val up: Action, val down: Action, val left: Action, val right: Action) {
    enum class Mode { LETTER, FLICK }
    enum class Action(val code: Int,val label: String) {
        NONE(KeyCode.NOT_SPECIFIED,"None"), COPY(KeyCode.CLIPBOARD_COPY,"Copy"),
        PASTE(KeyCode.CLIPBOARD_PASTE,"Paste"), CUT(KeyCode.CLIPBOARD_CUT,"Cut"),
        SELECT_ALL(KeyCode.CLIPBOARD_SELECT_ALL,"Select all"), UNDO(KeyCode.UNDO,"Undo"),
        REDO(KeyCode.REDO,"Redo"), TOOLS(KeyCode.TEXT_TOOLS,"Text tools")
    }

    fun resolve(dx: Float,dy: Float,maxTravel: Float,width: Int,height: Int,
                elapsed: Long,target: Int): Int {
        if (!enabled || width <= 0 || height <= 0 || elapsed !in 0L..1000L ||
            !dx.isFinite() || !dy.isFinite() || !maxTravel.isFinite()) return KeyCode.NOT_SPECIFIED
        val threshold = min(width,height)*.48f
        val distance = hypot(dx,dy)
        if (maxTravel < threshold && distance < threshold)
            return if (target == KeyCode.TEXT_TOOLS && elapsed <= 500L) KeyCode.TEXT_TOOLS else KeyCode.NOT_SPECIFIED
        if (distance < threshold) return KeyCode.NOT_SPECIFIED
        if (mode == Mode.LETTER) return when(target) {
            'c'.code,'C'.code -> Action.COPY.code
            'v'.code,'V'.code -> Action.PASTE.code
            'x'.code,'X'.code -> Action.CUT.code
            'a'.code,'A'.code -> Action.SELECT_ALL.code
            'z'.code,'Z'.code -> Action.UNDO.code
            else -> KeyCode.NOT_SPECIFIED
        }
        if (elapsed > 400L || distance > 2.5f*maxOf(width,height)) return KeyCode.NOT_SPECIFIED
        val horizontal = abs(dx); val vertical = abs(dy)
        return when {
            horizontal >= vertical*1.6f -> if (dx < 0) left.code else right.code
            vertical >= horizontal*1.6f -> if (dy < 0) up.code else down.code
            else -> KeyCode.NOT_SPECIFIED
        }
    }

    companion object {
        const val ENABLED = "brr_edit_gestures"
        const val MODE = "brr_edit_gesture_mode"
        const val UP = "brr_edit_gesture_up"
        const val DOWN = "brr_edit_gesture_down"
        const val LEFT = "brr_edit_gesture_left"
        const val RIGHT = "brr_edit_gesture_right"
        @JvmStatic fun read(prefs: SharedPreferences): EditingGestureConfig {
            fun action(key: String, fallback: Action) =
                runCatching { Action.valueOf(prefs.getString(key,fallback.name) ?: fallback.name) }.getOrDefault(fallback)
            val mode = runCatching { Mode.valueOf(prefs.getString(MODE,Mode.LETTER.name) ?: Mode.LETTER.name) }.getOrDefault(Mode.LETTER)
            return EditingGestureConfig(prefs.getBoolean(ENABLED,false),mode,
                action(UP,Action.COPY),action(DOWN,Action.PASTE),action(LEFT,Action.UNDO),action(RIGHT,Action.REDO))
        }
        @JvmStatic fun allowedCode(code: Int): Boolean =
            code != KeyCode.NOT_SPECIFIED && Action.entries.any { it.code == code }
    }
}
