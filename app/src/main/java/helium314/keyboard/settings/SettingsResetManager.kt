// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import helium314.keyboard.latin.utils.prefs
import org.json.JSONArray

/** Reversible reset of preferences only; user data and installed models are preserved. */
internal object SettingsResetManager {
    private const val UNDO_PREFS = "brr_settings_reset_undo_v1"
    private const val EXPIRES_AT = "expires_at"
    private const val VALUE_PREFIX = "value:"
    const val UNDO_WINDOW_MS = 60L * 60L * 1000L

    fun reset(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        val preferences = context.prefs()
        val undo = undoPreferences(context)
        val editor = undo.edit().clear().putLong(EXPIRES_AT, nowMs + UNDO_WINDOW_MS)
        preferences.all.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(VALUE_PREFIX + key, "s:$value")
                is Boolean -> editor.putString(VALUE_PREFIX + key, if (value) "b:1" else "b:0")
                is Int -> editor.putString(VALUE_PREFIX + key, "i:$value")
                is Long -> editor.putString(VALUE_PREFIX + key, "l:$value")
                is Float -> editor.putString(VALUE_PREFIX + key, "f:$value")
                is Set<*> -> editor.putString(VALUE_PREFIX + key,
                    "S:" + JSONArray(value.filterIsInstance<String>()).toString())
            }
        }
        if (!editor.commit()) return false
        if (preferences.edit().clear().commit()) return true
        restoreSnapshot(preferences, undo)
        undo.edit().clear().apply()
        return false
    }

    fun undo(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        val undo = undoPreferences(context)
        if (undo.getLong(EXPIRES_AT, 0L) <= nowMs) {
            undo.edit().clear().apply()
            return false
        }
        val success = restoreSnapshot(context.prefs(), undo)
        if (success) undo.edit().clear().apply()
        return success
    }

    fun remainingMs(context: Context, nowMs: Long = System.currentTimeMillis()): Long {
        val undo = undoPreferences(context)
        val remaining = (undo.getLong(EXPIRES_AT, 0L) - nowMs).coerceAtLeast(0L)
        if (remaining == 0L && undo.contains(EXPIRES_AT)) undo.edit().clear().apply()
        return remaining
    }

    private fun restoreSnapshot(preferences: SharedPreferences, undo: SharedPreferences): Boolean {
        val editor = preferences.edit().clear()
        undo.all.forEach { (storedKey, storedValue) ->
            if (!storedKey.startsWith(VALUE_PREFIX) || storedValue !is String) return@forEach
            val key = storedKey.removePrefix(VALUE_PREFIX)
            val split = storedValue.indexOf(':')
            if (split <= 0) return@forEach
            val value = storedValue.substring(split + 1)
            when (storedValue.substring(0, split)) {
                "s" -> editor.putString(key, value)
                "b" -> editor.putBoolean(key, value == "1")
                "i" -> value.toIntOrNull()?.let { editor.putInt(key, it) }
                "l" -> value.toLongOrNull()?.let { editor.putLong(key, it) }
                "f" -> value.toFloatOrNull()?.let { editor.putFloat(key, it) }
                "S" -> runCatching {
                    val array = JSONArray(value)
                    editor.putStringSet(key, buildSet {
                        for (index in 0 until array.length()) add(array.getString(index))
                    })
                }
            }
        }
        return editor.commit()
    }

    private fun undoPreferences(context: Context): SharedPreferences {
        val app = context.applicationContext
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !app.isDeviceProtectedStorage)
            app.createDeviceProtectedStorageContext() else app
        return storage.getSharedPreferences(UNDO_PREFS, Context.MODE_PRIVATE)
    }
}
