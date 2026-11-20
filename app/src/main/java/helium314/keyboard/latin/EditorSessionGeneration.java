// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import java.util.concurrent.atomic.AtomicLong;

/** Content-free editor/privacy epoch for discarding work queued in a previous field. */
public final class EditorSessionGeneration {
    private final AtomicLong epoch = new AtomicLong(1L);

    public long current() { return epoch.get(); }

    public long invalidate() { return epoch.incrementAndGet(); }

    public boolean accepts(final long expected) { return epoch.get() == expected; }
}
