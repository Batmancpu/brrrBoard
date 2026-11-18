// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * One process-local, low-priority worker for Describe an Emoji. No model,
 * network, writable database, typed-query history or normal-typing hooks.
 */
final class EmojiDescribeEngine {
    interface Callback {
        /** Always invoked on the UI thread; false allows legacy dictionary fallback. */
        void onResult(List<String> results, boolean indexAvailable);
    }

    private static final EmojiDescribeEngine INSTANCE = new EmojiDescribeEngine();
    private static final String TAG = "EmojiDescribe";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(
            1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), runnable -> {
                final Thread thread = new Thread(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    runnable.run();
                }, "brr-emoji-describe");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());

    private volatile EmojiDescribeIndex index;
    private volatile boolean loadFailed;

    static EmojiDescribeEngine get() { return INSTANCE; }

    private EmojiDescribeEngine() {
        // After idle Search, release even the worker thread; the immutable
        // index stays resident and the next explicit Search is cheap.
        worker.allowCoreThreadTimeOut(true);
    }

    void warmUp(final Context context) {
        if (index != null || loadFailed) return;
        final Context app = context.getApplicationContext();
        worker.execute(() -> ensureIndex(app));
    }

    void search(final Context context, final String query, final int limit,
            final Callback callback) {
        // The only queued request is always the latest keystroke. The UI also
        // checks a generation ID before applying a result from an older job.
        worker.getQueue().clear();
        final Context app = context.getApplicationContext();
        final String bounded = EmojiDescribeNormalizer.clampQuery(query);
        worker.execute(() -> {
            final EmojiDescribeIndex ready = ensureIndex(app);
            List<String> result = List.of();
            boolean available = ready != null;
            if (ready != null) {
                try {
                    result = ready.search(bounded, limit);
                } catch (final RuntimeException error) {
                    // A malformed description must never terminate Search or
                    // permanently stall the single worker thread.
                    available = false;
                    Log.e(TAG, "Unable to search bundled Emoji descriptions", error);
                }
            }
            final List<String> safeResult = result;
            final boolean indexAvailable = available;
            ui.post(() -> callback.onResult(safeResult, indexAvailable));
        });
    }

    void cancelPending() {
        worker.getQueue().clear();
    }

    private EmojiDescribeIndex ensureIndex(final Context context) {
        if (index != null || loadFailed) return index;
        try (final InputStream input = context.getAssets().open(EmojiContextIndex.ASSET_NAME)) {
            final EmojiContextIndex annotations = EmojiContextIndex.read(input);
            index = EmojiDescribeIndex.from(annotations);
        } catch (final IOException | RuntimeException error) {
            loadFailed = true;
            Log.e(TAG, "Unable to build bundled offline Emoji description index", error);
        }
        return index;
    }
}
