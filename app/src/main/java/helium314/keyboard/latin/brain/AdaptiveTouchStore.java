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
    private static final int MAX_LAYOUTS = AdaptiveTouchRecord.MAX_LAYOUTS, MAX_BYTES = 16384;
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
    private final LinkedHashMap<Long, AdaptiveTouchEvidence> evidence = new LinkedHashMap<>();
    private volatile Map<Long, AdaptiveTouchModel.Snapshot> snapshots = Collections.emptyMap();
    private volatile List<Inspection> inspections = Collections.emptyList();
    public static final class Inspection {
        public final long layout;
        public final AdaptiveTouchModel.Snapshot model;
        private final AdaptiveTouchEvidence evidence;
        Inspection(long layout, AdaptiveTouchModel.Snapshot model, AdaptiveTouchEvidence evidence) {
            this.layout=layout; this.model=model; this.evidence=evidence.copy();
        }
        public int count(int key) { return evidence.count(key); }
        public int mismatch(int key) { return evidence.mismatch(key); }
        public int mismatchMask(int key) { return evidence.mismatchMask(key); }
        public int region(int key) { return evidence.region(key); }
        public int countAt(int key,int cell) { return evidence.countAt(key,cell); }
    }
    /** Copies only fixed-size aggregates; never decrypts or blocks the UI. */
    public List<Inspection> inspect() { return unlocked() ? inspections : Collections.emptyList(); }
    private boolean loaded, readOnly;
    private volatile String storageStatus="Loading";
    public String inspectionStatus() { return storageStatus; }
    private ScheduledFuture<?> saveTask;
    private AdaptiveTouchStore(Context c) {
        context = c;
        worker.execute(() -> load(generation.get()));
    }
    public AdaptiveTouchModel.Snapshot snapshot(long layout) { return snapshots.get(layout); }
    private boolean enabled() {
        android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
        return prefs.getBoolean(helium314.keyboard.latin.settings.Settings.PREF_ADAPTIVE_TOUCH, true)
                && prefs.getBoolean(helium314.keyboard.latin.settings.Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true);
    }
    private boolean unlocked() {
        UserManager user = context.getSystemService(UserManager.class);
        return user != null && user.isUserUnlocked();
    }
    private AtomicFile file() {
        // The application context is CE by default; refuse a device-protected context.
        // Android exposes createDeviceProtectedStorageContext(), not its inverse.
        if (context.isDeviceProtectedStorage()) throw new IllegalStateException("CE storage required");
        return new AtomicFile(new File(context.getNoBackupFilesDir(), "adaptive_touch_v1.enc"));
    }
    private void load(long token) {
        if (loaded || !unlocked() || token != generation.get()) return;
        loaded = true;
        byte[] plain = null;
        try {
            AtomicFile file = file();
            if (!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath()+".bak").exists()) {
                storageStatus="Empty"; return;
            }
            byte[] encrypted = file.readFully();
            if (encrypted.length > MAX_BYTES) throw new IOException("oversize");
            plain = BrainLearningVault.open(encrypted);
            AdaptiveTouchRecord record=AdaptiveTouchRecord.read(plain);
            models.putAll(record.models); evidence.putAll(record.evidence);
            storageStatus="Encrypted record loaded";
            publish(token);
        } catch (Exception e) {
            models.clear(); evidence.clear(); readOnly = true; // preserve unreadable ciphertext; never downgrade to plaintext
            storageStatus="Encrypted record unavailable; learning paused";
        } finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
    }
    private void publish(long token) {
        Map<Long, AdaptiveTouchModel.Snapshot> next = new HashMap<>();
        List<Inspection> detail = new ArrayList<>();
        for (Map.Entry<Long, AdaptiveTouchModel> entry : models.entrySet()) {
            AdaptiveTouchModel.Snapshot snap=entry.getValue().snapshot();
            next.put(entry.getKey(),snap);
            detail.add(new Inspection(entry.getKey(),snap,evidence.get(entry.getKey())));
        }
        if (token == generation.get()) {
            snapshots = Collections.unmodifiableMap(next);
            inspections = Collections.unmodifiableList(detail);
        }
    }
    /** Arrays are per-group sufficient statistics of explicitly confirmed taps, never raw samples. */
    public void learn(long layout, int[] counts, double[] xs, double[] ys, AdaptiveTouchEvidence observed) {
        if (counts.length != 6 || xs.length != 6 || ys.length != 6 || observed == null) return;
        if (pending.incrementAndGet() > 16) { pending.decrementAndGet(); return; }
        final long token = generation.get();
        int[] n = counts.clone(); double[] x = xs.clone(), y = ys.clone();
        AdaptiveTouchEvidence batch = observed.copy();
        worker.execute(() -> {
            try {
                if (token != generation.get() || !unlocked() || !enabled()) return;
                load(token);
                if (!loaded || readOnly) return;
                AdaptiveTouchModel m = models.get(layout);
                if (m == null) {
                    if (models.size() >= MAX_LAYOUTS) {
                        Long old=models.keySet().iterator().next(); models.remove(old); evidence.remove(old);
                    }
                    m = new AdaptiveTouchModel(); models.put(layout, m);
                    evidence.put(layout,new AdaptiveTouchEvidence());
                }
                if (!evidence.get(layout).compatible(batch)) return;
                for (int i = 0; i < n.length; i++) if (n[i] != 0) m.merge(i, n[i], x[i], y[i]);
                evidence.get(layout).merge(batch);
                storageStatus="Pending encrypted save";
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
            plain = AdaptiveTouchRecord.encode(models,evidence);
            byte[] encrypted = BrainLearningVault.seal(plain);
            if (token != generation.get() || !enabled()) return;
            f = file(); out = f.startWrite(); out.write(encrypted); f.finishWrite(out); out = null;
            storageStatus="Encrypted record saved";
        } catch (Exception e) {
            if (out != null && f != null) f.failWrite(out);
            storageStatus="Save failed; previous encrypted record retained";
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
        generation.incrementAndGet(); snapshots = Collections.emptyMap(); inspections = Collections.emptyList();
        worker.execute(() -> {
            if (saveTask != null) saveTask.cancel(false);
            saveTask = null; models.clear(); evidence.clear(); readOnly = false; loaded = true;
            if (unlocked() && !context.isDeviceProtectedStorage()) {
                file().delete();
                storageStatus=file().getBaseFile().exists() ? "Reset failed; encrypted file remains" : "Empty";
            } else storageStatus="Reset unavailable while storage is locked";
        });
    }
}
