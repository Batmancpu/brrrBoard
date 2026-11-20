// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.AtomicFile;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import helium314.keyboard.latin.brain.BrainLearningVault;
import helium314.keyboard.latin.brain.BrainPerformanceMetrics;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.utils.KtxKt;

/**
 * The single offline Emoji recommender feeding typing, Smart/Recent and idle Search.
 * Reviewed semantic phrases + encrypted per-app/field selection patterns + Recents.
 * Never stores the typed text, app package or field identifiers; never inserts itself.
 */
public final class BrrSmartEmojiEngine {
    private static final String FILE_NAME = "br_brain_emoji_patterns_v1.bin";
    private static final Object LOCK = new Object();
    private static final BrrEmojiPatternModel MODEL = new BrrEmojiPatternModel();
    private static final AtomicBoolean WARMED = new AtomicBoolean();
    private static volatile boolean loaded;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }, "brr-smart-emoji");
        thread.setDaemon(true);
        return thread;
    });

    public static final class Pick {
        public final String emoji;
        public final String explanation;
        public final int confidence;
        Pick(final String emoji, final String explanation, final int confidence) {
            this.emoji = emoji;
            this.explanation = explanation;
            this.confidence = confidence;
        }
    }

    private BrrSmartEmojiEngine() { }

    public static void warm(@NonNull final Context context) {
        if (!WARMED.compareAndSet(false, true)) return;
        final Context app = context.getApplicationContext();
        IO.execute(() -> loadOnce(app));
    }

    private static void loadOnce(final Context app) {
        synchronized (LOCK) {
            if (loaded) return;
            try {
                final AtomicFile file = new AtomicFile(new File(app.getNoBackupFilesDir(), FILE_NAME));
                if (file.getBaseFile().isFile()) {
                    final byte[] encrypted = file.readFully();
                    final byte[] data = BrainLearningVault.open(encrypted);
                    MODEL.deserialize(new String(data, StandardCharsets.UTF_8), System.currentTimeMillis());
                }
            } catch (Exception ignored) {
                // Fail closed to a fresh empty model, not an unencrypted backup.
            }
            loaded = true;
        }
    }

    private static String scope(final EditorInfo editor) {
        if (editor == null) return "";
        // fieldId is hashed with the package: no raw app name or field ID on disk.
        final String app = editor.packageName == null ? "" : editor.packageName;
        if (app.isEmpty()) return "";
        return BrrEmojiPatternModel.appScope(app + "#" + editor.fieldId,
                editor.inputType & InputType.TYPE_MASK_VARIATION);
    }

    private static boolean personalizationAllowed(final Context context) {
        return KtxKt.prefs(context).getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true);
    }

    /** Called exactly once after the user actually selects an Emoji in an eligible host field. */
    public static void learn(@NonNull final Context context, final EditorInfo editor,
            final CharSequence previousContext, final String emoji) {
        if (!EmojiContextEligibility.isEligible(editor, Settings.getValues().mIncognitoModeEnabled)
                || !personalizationAllowed(context)
                || previousContext == null || emoji == null || emoji.isEmpty()) return;
        final String appScope = scope(editor);
        // Only the bounded ephemeral suffix reaches the worker. It is hashed before persistence.
        final String suffix = previousContext.subSequence(
                Math.max(0, previousContext.length() - 160), previousContext.length()).toString();
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            loadOnce(app);
            synchronized (LOCK) {
                MODEL.learn(appScope, suffix, emoji, System.currentTimeMillis());
                persist(app);
            }
        });
    }

    private static void persist(final Context app) {
        final AtomicFile file = new AtomicFile(new File(app.getNoBackupFilesDir(), FILE_NAME));
        FileOutputStream out = null;
        try {
            final byte[] plain = MODEL.serialize(System.currentTimeMillis())
                    .getBytes(StandardCharsets.UTF_8);
            final byte[] sealed = BrainLearningVault.seal(plain);
            out = file.startWrite();
            out.write(sealed);
            file.finishWrite(out);
        } catch (Exception ignored) {
            if (out != null) file.failWrite(out);
            // Never write a plaintext fallback or leak the context to logs.
        }
    }

    /**
     * Shared ranking. stripOnly never returns a generic/frequency-only Emoji;
     * Smart/idle Search additionally receive personal Recents and safe defaults.
     */
    public static List<Pick> suggest(@NonNull final Context context, final EditorInfo editor,
            final CharSequence previousContext, final int max, final boolean stripOnly) {
        if (max <= 0 || !EmojiContextEligibility.isEligible(editor,
                Settings.getValues().mIncognitoModeEnabled)) return List.of();
        final long started = BrainPerformanceMetrics.begin();
        try {
        final String text = previousContext == null ? "" : previousContext.toString();
        final Map<String, Pick> choices = new HashMap<>();

        for (final EmojiContextScorer.Candidate candidate : EmojiContextScorer.rank(text, false)) {
            final int confidence = candidate.confidence;
            if (!stripOnly || confidence >= 520) {
                choices.put(candidate.emoji, new Pick(candidate.emoji,
                        candidate.matchedTrigger, confidence));
            }
        }
        final boolean personal = personalizationAllowed(context);
        if (personal && loaded) {
            final List<BrrEmojiPatternModel.Prediction> learned;
            synchronized (LOCK) {
                learned = MODEL.predict(scope(editor), text, System.currentTimeMillis());
            }
            for (final BrrEmojiPatternModel.Prediction candidate : learned) {
                if (stripOnly && candidate.confidence < 500) continue;
                final Pick previous = choices.get(candidate.emoji);
                // Prioritize clear phrase cues over a merely frequent but unrelated Emoji.
                final int confidence = candidate.confidence;
                if (previous == null || confidence > previous.confidence)
                    choices.put(candidate.emoji, new Pick(candidate.emoji, "your recent phrasing", confidence));
            }
        }
        if (!stripOnly && personal) {
            final List<String> recentEmojis = EmojiPalettesView.AdaptiveEmojiEngine
                    .getFastRowEmojis(context, Math.max(max, 10));
            for (int i = 0; i < recentEmojis.size(); ++i) {
                final String emoji = recentEmojis.get(i);
                choices.putIfAbsent(emoji, new Pick(emoji, "recently used", 90 - i));
            }
        }
        final ArrayList<Pick> ordered = new ArrayList<>(choices.values());
        ordered.sort(Comparator.comparingInt((Pick p) -> p.confidence)
                .reversed().thenComparing(p -> p.emoji));
        return List.copyOf(ordered.subList(0, Math.min(max, ordered.size())));
        } finally {
            BrainPerformanceMetrics.end(BrainPerformanceMetrics.Stage.SMART_EMOJI, started);
        }
    }

    /** Local erase for a future BR Brain dashboard control; no text data ever returns. */
    public static int learnedPatternCount() {
        if (!loaded) return 0;
        synchronized (LOCK) { return MODEL.size(); }
    }

    /** A zero count is not evidence of an empty store until its async load completes. */
    public static boolean isLoaded() { return loaded; }

    /** Erases only contextual associations, preserving actual Emoji Recents. */
    public static void forgetLearnedPatterns(@NonNull final Context context) {
        forgetLearnedPatterns(context, null);
    }

    /** Completion is posted only AFTER queued writes and erase finish, on the main thread. */
    public static void forgetLearnedPatterns(@NonNull final Context context,
            @Nullable final Runnable onCompleted) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            synchronized (LOCK) {
                MODEL.deserialize("v1\n", System.currentTimeMillis());
                loaded = true;
                new AtomicFile(new File(app.getNoBackupFilesDir(), FILE_NAME)).delete();
            }
            if (onCompleted != null) new Handler(Looper.getMainLooper()).post(onCompleted);
        });
    }
}
