// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AtomicFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** One bounded, content-free Emoji layout sample, copied only after an explicit tap. */
public final class EmojiGeometryReport {
    private static final String FILE_NAME = "emoji-geometry-report.v1";
    private static final String HEADER = "brrBoard Emoji geometry v1";
    private static final int MAX_REPORT_BYTES = 12 * 1024;
    private static final Pattern NUMERIC_GEOMETRY = Pattern.compile(
            "-?[0-9]+(?:\\.[0-9]+)?(?:[,x:]-?[0-9]+(?:\\.[0-9]+)?)*");
    private static final Pattern BAR_INSETS = Pattern.compile("bars:-?[0-9]+,-?[0-9]+");
    private static final AtomicReference<String> pendingReport = new AtomicReference<>();
    private static final AtomicBoolean writerScheduled = new AtomicBoolean();
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, "brr-emoji-geometry");
        thread.setDaemon(true);
        return thread;
    });

    private EmojiGeometryReport() {}

    /** Accept only the geometry-only protocol emitted by InputView. */
    public static void record(final Context context, final String report) {
        if (context == null || !isGeometryOnly(report)) return;
        pendingReport.set(report);
        if (writerScheduled.compareAndSet(false, true)) {
            final Context appContext = context.getApplicationContext();
            writer.execute(() -> drainWrites(appContext == null ? context : appContext));
        }
    }

    private static boolean isGeometryOnly(final String report) {
        // The trace is deliberately a tiny ASCII protocol of labels and numeric
        // rectangles. Reject arbitrary strings so this helper cannot become a
        // route for persisting editor text or other UI payloads.
        if (report == null || report.length() > MAX_REPORT_BYTES) return false;
        final String[] tokens = report.split(" ");
        if (tokens.length < 2 || !"geometry-only".equals(tokens[0])) return false;
        for (int i = 1; i < tokens.length; ++i) {
            final int separator = tokens[i].indexOf('=');
            if (separator <= 0 || separator == tokens[i].length() - 1) return false;
            final String key = tokens[i].substring(0, separator);
            final String value = tokens[i].substring(separator + 1);
            if (!isGeometryField(key) || !isGeometryValue(value)) return false;
        }
        return true;
    }

    private static boolean isGeometryField(final String key) {
        switch (key) {
            case "decor": case "inputArea": case "input": case "frame": case "strip":
            case "wrapper": case "emoji": case "shell": case "pager": case "categories":
            case "firstEmojiRow": case "firstKeyBand": case "footer": case "measured":
            case "localY": case "translationY": case "scrollY": case "padding":
            case "parent": case "parentPadding": case "layout": case "margins":
            case "localTop": case "viewPaddingTop": case "keyboardTopPadding":
            case "verticalGap": case "windowInsets": case "ime": case "gestures":
            case "orientation": case "density": case "panelSearch":
            case "keyboardState": case "toolbarMode":
            case "framePadding":
                return true;
            default: return false;
        }
    }

    private static boolean isGeometryValue(final String value) {
        return "absent".equals(value) || "hidden".equals(value)
                || "unavailable".equals(value) || "empty".equals(value)
                || "true".equals(value) || "false".equals(value)
                || "EMOJI".equals(value) || "HIDDEN".equals(value)
                || "SYMBOLS_SHIFTED".equals(value) || "CLIPBOARD".equals(value)
                || "AI_TOOLS".equals(value) || "ACCESS_POINT".equals(value)
                || "KLIPY".equals(value) || "OTHER".equals(value)
                || "EXPANDABLE".equals(value) || "TOOLBAR_KEYS".equals(value)
                || "SUGGESTION_STRIP".equals(value)
                || NUMERIC_GEOMETRY.matcher(value).matches()
                || BAR_INSETS.matcher(value).matches();
    }

    private static void drainWrites(final Context context) {
        while (true) {
            final String report = pendingReport.getAndSet(null);
            if (report != null) {
                try { write(context, report); }
                catch (final RuntimeException ignored) { /* diagnostics must not break the worker */ }
            }
            writerScheduled.set(false);
            if (pendingReport.get() == null || !writerScheduled.compareAndSet(false, true)) return;
        }
    }

    private static void write(final Context context, final String report) {
        final byte[] bytes = (HEADER + "\n" + report).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_REPORT_BYTES) return;
        final AtomicFile file = new AtomicFile(file(context));
        try {
            final java.io.FileOutputStream output = file.startWrite();
            try {
                output.write(bytes);
                file.finishWrite(output);
            } catch (final IOException error) {
                file.failWrite(output);
            }
        } catch (final IOException ignored) {
            // Diagnostics must never affect keyboard input or panel rendering.
        } finally {
            java.util.Arrays.fill(bytes, (byte) 0);
        }
    }

    public static boolean hasReport(final Context context) {
        final File file = file(context);
        return file.isFile() && file.length() > 0 && file.length() <= MAX_REPORT_BYTES;
    }

    /** Copies only after the user presses the visible Copy report button. */
    public static boolean copyLatest(final Context context) {
        final String report = read(context);
        if (report == null) return false;
        try {
            final ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return false;
            clipboard.setPrimaryClip(ClipData.newPlainText("brrBoard Emoji geometry", report));
            return true;
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    public static void erase(final Context context) {
        if (context == null) return;
        pendingReport.set(null);
        final Context app = context.getApplicationContext();
        try {
            writer.execute(() -> {
                try { new AtomicFile(file(app == null ? context : app)).delete(); }
                catch (final RuntimeException ignored) { /* best-effort local erasure */ }
            });
        } catch (final RuntimeException ignored) {
            // Erasure is best-effort and has no keyboard-path side effects.
        }
    }

    private static String read(final Context context) {
        final File source = file(context);
        final long length = source.length();
        if (!source.isFile() || length <= 0 || length > MAX_REPORT_BYTES) return null;
        try {
            final byte[] bytes = new byte[(int) length];
            try (java.io.FileInputStream input = new AtomicFile(source).openRead()) {
                int offset = 0;
                while (offset < bytes.length) {
                    final int count = input.read(bytes, offset, bytes.length - offset);
                    if (count <= 0) return null;
                    offset += count;
                }
            }
            final String text = new String(bytes, StandardCharsets.UTF_8);
            return text.startsWith(HEADER + "\ngeometry-only") ? text : null;
        } catch (final IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static File file(final Context context) {
        return new File(context.getNoBackupFilesDir(), FILE_NAME);
    }
}
