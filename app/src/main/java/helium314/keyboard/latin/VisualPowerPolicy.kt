// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings

/** Edition-local quality policy; cannot change global power or accessibility settings. */
object VisualPowerPolicy {
    data class Battery(val percent: Int, val charging: Boolean)
    @Volatile private var cached: Battery? = null
    internal fun lowBattery(percent: Int, charging: Boolean): Boolean = percent in 0..19 && !charging
    private fun parse(intent: Intent?): Battery? {
        if (intent == null) return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (scale <= 0 || level !in 0..scale) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return Battery((level.toLong() * 100 / scale).toInt(), status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL)
    }
    fun battery(context: Context): Battery? = runCatching {
        parse(context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    }.getOrNull()
    @JvmStatic fun updateBattery(intent: Intent): Boolean {
        val before = cached
        val next = parse(intent) ?: return false
        cached = next
        return before == null || lowBattery(before.percent, before.charging) != lowBattery(next.percent, next.charging)
    }
    @JvmStatic fun suppressOptional(context: Context): Boolean {
        if (!BuildConfig.COMPATIBILITY_EDITION) return systemSaver(context)
        val value = cached ?: battery(context)?.also { cached = it }
        return value?.let { lowBattery(it.percent, it.charging) } ?: systemSaver(context)
    }
    fun systemSaver(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    fun isBatteryRestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == false
    fun openBatterySettings(context: Context) {
        val intents = listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
        for (intent in intents) {
            if (runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }
}
