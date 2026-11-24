// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.os.UserManager;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Credential-protected, encrypted aggregate-only model. No words, touch traces or editor IDs. */
public final class AdaptiveTouchStore {
    private static final int MAGIC = 0x41545031, MAX_LAYOUTS = 12, MAX_BYTES = 16384;
    private static AdaptiveTouchStore instance;
    public static synchronized AdaptiveTouchStore get(Context context) {
        if (instance == null) instance = new AdaptiveTouchStore(context.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicLong generation = new AtomicLong();
    private final LinkedHashMap<Long, AdaptiveTouchModel> models = new LinkedHashMap<>();
    private volatile Map<Long, AdaptiveTouchModel.Snapshot> snapshots = Collections.emptyMap();
    private boolean loaded, readOnly;
    private ScheduledFuture<?> saveTask;
    private AdaptiveTouchStore(Context c) {
        context = c;
        worker.execute(() -> load(generation.get()));
    }
    public AdaptiveTouchModel.Snapshot snapshot(long layout) { return snapshots.get(layout); }
    private boolean enabled() {
        android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
        return prefs.getBoolean(helium314.keyboard.latin.settings.Settings.PREF_ADAPTIVE_TOUCH, false)
                && prefs.getBoolean(helium314.keyboard.latin.settings.Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true);
    }
    private boolean unlocked() {
        UserManager user = context.getSystemService(UserManager.class);
        return user != null && user.isUserUnlocked();
    }
    private AtomicFile file() {
        return new AtomicFile(new File(context.createCredentialProtectedStorageContext()
                .getNoBackupFilesDir(), "adaptive_touch_v1.enc"));
    }
    private void load(long token) {
        if (loaded || !unlocked() || token != generation.get()) return;
        loaded = true;
        byte[] plain = null;
        try {
            AtomicFile file = file();
            if (!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath()+".bak").exists()) return;
            byte[] encrypted = file.readFully();
            if (encrypted.length > MAX_BYTES) throw new IOException("oversize");
            plain = BrainLearningVault.open(encrypted);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(plain));
            if (in.readInt() != MAGIC) throw new IOException("record type");
            int size = in.readInt();
            if (size < 0 || size > MAX_LAYOUTS) throw new IOException("record count");
            for (int i = 0; i < size; i++) {
                long id = in.readLong();
                if (models.put(id, AdaptiveTouchModel.readFrom(in)) != null) throw new IOException("duplicate");
            }
            if (in.available() != 0) throw new IOException("trailing bytes");
            publish(token);
        } catch (Exception e) {
            models.clear(); readOnly = true; // preserve unreadable ciphertext; never downgrade to plaintext
        } finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
    }
    private void publish(long token) {
        Map<Long, AdaptiveTouchModel.Snapshot> next = new HashMap<>();
        for (Map.Entry<Long, AdaptiveTouchModel> entry : models.entrySet())
            next.put(entry.getKey(), entry.getValue().snapshot());
        if (token == generation.get()) snapshots = Collections.unmodifiableMap(next);
    }
    /** Arrays are per-group sufficient statistics of explicitly confirmed taps, never raw samples. */
    public void learn(long layout, int[] counts, double[] xs, double[] ys) {
        if (counts.length != 6 || xs.length != 6 || ys.length != 6) return;
        if (pending.incrementAndGet() > 16) { pending.decrementAndGet(); return; }
        final long token = generation.get();
        int[] n = counts.clone(); double[] x = xs.clone(), y = ys.clone();
        worker.execute(() -> {
            try {
                if (token != generation.get() || !unlocked() || !enabled()) return;
                load(token);
                if (!loaded || readOnly) return;
                AdaptiveTouchModel m = models.get(layout);
                if (m == null) {
                    if (models.size() >= MAX_LAYOUTS) models.remove(models.keySet().iterator().next());
                    m = new AdaptiveTouchModel(); models.put(layout, m);
                }
                for (int i = 0; i < n.length; i++) if (n[i] != 0) m.merge(i, n[i], x[i], y[i]);
                publish(token);
                if (saveTask == null || saveTask.isDone())
                    saveTask = worker.schedule(() -> save(token), 2, TimeUnit.SECONDS);
            } finally { pending.decrementAndGet(); }
        });
    }
    private void save(long token) {
        if (token != generation.get() || !unlocked() || readOnly || !enabled()) return;
        byte[] plain = null;
        FileOutputStream out = null;
        AtomicFile f = null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            data.writeInt(MAGIC); data.writeInt(models.size());
            for (Map.Entry<Long, AdaptiveTouchModel> entry : models.entrySet()) {
                data.writeLong(entry.getKey()); entry.getValue().writeTo(data);
            }
            data.flush(); plain = bytes.toByteArray();
            byte[] encrypted = BrainLearningVault.seal(plain);
            if (token != generation.get() || !enabled()) return;
            f = file(); out = f.startWrite(); out.write(encrypted); f.finishWrite(out); out = null;
        } catch (Exception e) {
            if (out != null && f != null) f.failWrite(out);
        } finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
    }
    /** Cancel pre-opt-out queued learning and writes; explicit Reset removes stored aggregates. */
    public void pause() {
        generation.incrementAndGet();
        worker.execute(() -> {
            if (saveTask != null) saveTask.cancel(false);
            saveTask = null;
        });
    }
    public void reset() {
        generation.incrementAndGet(); snapshots = Collections.emptyMap();
        worker.execute(() -> {
            if (saveTask != null) saveTask.cancel(false);
            saveTask = null; models.clear(); readOnly = false; loaded = true;
            if (unlocked()) file().delete();
        });
    }
}
