// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Iterator;

/** Bounded soft LRU plus a small independent strong LRU. Caller serializes construction. */
final class LayoutRetentionCache<K, V> {
    private final int capacity;
    private final int warmCapacity;
    private final LinkedHashMap<K, SoftReference<V>> soft = new LinkedHashMap<>(16, .75f, true);
    private final LinkedHashMap<K, V> warm = new LinkedHashMap<>(8, .75f, true);

    LayoutRetentionCache(int capacity, int warmCapacity) {
        if (capacity <= 0 || warmCapacity < 0 || warmCapacity > capacity)
            throw new IllegalArgumentException("Invalid layout cache capacity");
        this.capacity = capacity;
        this.warmCapacity = warmCapacity;
    }

    V get(K key, boolean retain) {
        SoftReference<V> ref = soft.get(key);
        V value = ref == null ? null : ref.get();
        if (value == null) {
            soft.remove(key);
            warm.remove(key);
        } else if (retain) {
            remember(key, value);
        }
        return value;
    }

    void put(K key, V value, boolean retain) {
        soft.entrySet().removeIf(entry -> entry.getValue().get() == null);
        soft.put(key, new SoftReference<>(value));
        if (retain) remember(key, value);
        else warm.remove(key);
        while (soft.size() > capacity) {
            // Prefer evicting a non-warm layout, even if the user has opened
            // many symbol/editor variants since their last alphabet use.
            Iterator<K> keys = soft.keySet().iterator();
            while (keys.hasNext()) {
                K victim = keys.next();
                if (!warm.containsKey(victim)) {
                    keys.remove();
                    break;
                }
            }
        }
    }

    private void remember(K key, V value) {
        if (warmCapacity == 0) return;
        warm.put(key, value);
        while (warm.size() > warmCapacity) {
            Iterator<K> keys = warm.keySet().iterator();
            keys.next(); keys.remove();
        }
    }

    void clear() { soft.clear(); warm.clear(); }
    int size() { return soft.size(); }
    int warmSize() { return warm.size(); }
}
