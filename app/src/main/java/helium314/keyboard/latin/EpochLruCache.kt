// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** No in-flight lookup may refill a cache after its editor/dictionary invalidation. */
internal class EpochLruCache<K, V>(private val capacity: Int) {
    private val entries = LinkedHashMap<K, V>(16, .75f, true)
    private var epoch = 0L
    init { require(capacity > 0) }
    @Synchronized fun epoch(): Long = epoch
    @Synchronized fun get(requestEpoch: Long, key: K): V? =
        if (requestEpoch == epoch) entries[key] else null
    @Synchronized fun put(requestEpoch: Long, key: K, value: V) {
        if (requestEpoch != epoch) return
        entries[key] = value
        while (entries.size > capacity) {
            val it = entries.keys.iterator()
            it.next(); it.remove()
        }
    }
    @Synchronized fun clear() { epoch++; entries.clear() }
}
