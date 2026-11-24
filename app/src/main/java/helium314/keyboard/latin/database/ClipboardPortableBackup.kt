// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import helium314.keyboard.latin.ClipboardImageHistoryClip
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.ByteArrayOutputStream

/** User-selected portable text export. Plaintext ZIP entry; never automatic. */
object ClipboardPortableBackup {
    const val ENTRY_NAME = "clipboard_text_v1.json"
    private const val MAX_ROWS = 10_000
    private const val MAX_BYTES = 8*1024*1024
    private const val MAX_CHARS = 1_048_576
    data class Entry(val timestamp: Long,val pinned: Boolean,val text: String)
    fun valid(items: List<Entry>): Boolean =
        items.size <= MAX_ROWS && items.sumOf { it.text.length.toLong() } <= MAX_BYTES &&
            items.all { it.timestamp >= 0 && it.text.length in 1..MAX_CHARS &&
                !ClipboardImageHistoryClip.hasImageHistoryPrefix(it.text) &&
                it.text.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8) == it.text }
    fun encode(items: List<Entry>): ByteArray {
        require(valid(items))
        val rows = JSONArray()
        items.forEach { rows.put(JSONObject().put("timestamp",it.timestamp).put("pinned",it.pinned).put("text",it.text)) }
        val bytes = JSONObject().put("version",1).put("entries",rows).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES)
        return bytes
    }
    fun decode(input: InputStream): List<Entry> {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            require(output.size()+n <= MAX_BYTES)
            output.write(buffer,0,n)
        }
        val root = JSONObject(output.toString("UTF-8"))
        require(root.getInt("version") == 1)
        val rows = root.getJSONArray("entries")
        require(rows.length() <= MAX_ROWS)
        val result = List(rows.length()) { i ->
            val row = rows.getJSONObject(i)
            require(row.get("pinned") is Boolean && row.get("text") is String)
            Entry(row.getLong("timestamp"),row.getBoolean("pinned"),row.getString("text"))
        }
        require(valid(result))
        return result
    }
}
