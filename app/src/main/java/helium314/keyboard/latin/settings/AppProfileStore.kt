// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import android.os.UserManager
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Explicit, sparse per-package settings; not an app-usage or typing history. */
object AppProfileStore {
    data class Profile(
        val autocorrect: Boolean? = null,
        val suggestions: Boolean? = null,
        val numberRow: Boolean? = null,
        val sound: Boolean? = null,
        val haptics: Boolean? = null,
        val privateMode: Boolean = false
    ) {
        fun autocorrect(global: Boolean) = autocorrect ?: global
        fun suggestions(global: Boolean) = suggestions ?: global
        fun numberRow(global: Boolean) = numberRow ?: global
        fun sound(global: Boolean) = sound ?: global
        fun haptics(global: Boolean) = haptics ?: global
        fun privateMode(global: Boolean) = global || privateMode
    }
    private const val MAX_PROFILES = 32
    private const val MAX_BYTES = 16_384
    private val generation = AtomicLong()
    private var cached: Map<String, Profile>? = null
    private var unreadable = false
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    @JvmStatic fun revision(): Long = generation.get()
    private fun available(context: Context): Boolean =
        !context.isDeviceProtectedStorage &&
            context.getSystemService(UserManager::class.java)?.isUserUnlocked == true
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "app_profiles_v1.json"))

    private fun load(context: Context): Map<String, Profile> {
        cached?.let { return it }
        if (!available(context)) return emptyMap()
        val result = LinkedHashMap<String, Profile>()
        try {
            val f = file(context)
            if (f.baseFile.exists() || File(f.baseFile.path + ".bak").exists()) {
                val bytes = f.readFully()
                require(bytes.size <= MAX_BYTES)
                val root = JSONObject(bytes.toString(Charsets.UTF_8))
                require(root.getInt("schema") == 1)
                val apps = root.getJSONObject("profiles")
                require(apps.length() <= MAX_PROFILES)
                for (pkg in apps.keys()) {
                    require(pkg.length <= 200 && packagePattern.matches(pkg))
                    val row = apps.getJSONObject(pkg)
                    fun optional(key: String): Boolean? = if (row.has(key)) row.getBoolean(key) else null
                    result[pkg] = Profile(optional("autocorrect"), optional("suggestions"),
                        optional("numberRow"), optional("sound"), optional("haptics"),
                        row.optBoolean("privateMode", false))
                }
            }
        } catch (_: Exception) {
            unreadable = true
            result.clear()
        }
        return result.toMap().also { cached = it }
    }
    @JvmStatic @Synchronized
    fun get(context: Context, packageName: String?): Profile {
        if (packageName.isNullOrEmpty()) return Profile()
        val c = context.applicationContext ?: context
        if (!available(c)) return Profile()
        return load(c)[packageName] ?: Profile()
    }
    @Synchronized fun packages(context: Context): Set<String> {
        val c = context.applicationContext ?: context
        return if (available(c)) load(c).keys.toSet() else emptySet()
    }
    @Synchronized fun save(context: Context, packageName: String, profile: Profile): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c) || packageName.length > 200 || !packagePattern.matches(packageName)) return false
        val next = LinkedHashMap(load(c))
        if (unreadable) return false
        if (profile == Profile()) next.remove(packageName) else next[packageName] = profile
        if (next.size > MAX_PROFILES) return false
        val rows = JSONObject()
        for ((pkg, p) in next) {
            val row = JSONObject()
            p.autocorrect?.let { row.put("autocorrect", it) }
            p.suggestions?.let { row.put("suggestions", it) }
            p.numberRow?.let { row.put("numberRow", it) }
            p.sound?.let { row.put("sound", it) }
            p.haptics?.let { row.put("haptics", it) }
            if (p.privateMode) row.put("privateMode", true)
            rows.put(pkg, row)
        }
        val bytes = JSONObject().put("schema", 1).put("profiles", rows).toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BYTES) return false
        val f = file(c)
        val out = try { f.startWrite() } catch (_: Exception) { return false }
        return try {
            out.write(bytes); f.finishWrite(out)
            cached = next.toMap(); generation.incrementAndGet()
            true
        } catch (_: Exception) { f.failWrite(out); false }
    }
    /** A deliberate reset can recover an unreadable record; never export package identifiers. */
    @Synchronized fun reset(context: Context): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c)) return false
        return try {
            val f = file(c); f.delete()
            if (f.baseFile.exists()) return false
            cached = emptyMap(); unreadable = false; generation.incrementAndGet()
            true
        } catch (_: Exception) { false }
    }
}
