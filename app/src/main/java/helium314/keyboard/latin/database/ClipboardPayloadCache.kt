// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

/** Session-only cache: bounded by count, characters and a non-sliding time window. */
internal class ClipboardPayloadCache(
    private val clock: () -> Long,
    private val maxEntries: Int = 16,
    private val maxChars: Int = 262144,
    private val ttl: Long = 30000L
) {
    private val values = LinkedHashMap<Long,String>(16,.75f,true)
    private var chars=0
    private var deadline=0L
    fun get(id: Long): String? { prune(); return values[id] }
    fun put(id: Long,text: String) {
        prune()
        if(text.length>maxChars) return
        remove(id)
        if(values.isEmpty() && deadline==0L) deadline=clock()+ttl
        values[id]=text; chars+=text.length
        while(values.size>maxEntries || chars>maxChars) remove(values.entries.iterator().next().key)
    }
    fun remove(id: Long) { values.remove(id)?.let { chars-=it.length } }
    fun clear() { values.clear(); chars=0; deadline=0 }
    fun prune() { if(deadline!=0L && clock()>=deadline) clear() }
    fun expiry(): Long { prune(); return deadline }
}
