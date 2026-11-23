// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin.personalization;

import android.util.AtomicFile;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Crash-safe copy of legacy Direct-Boot history dictionaries into credential-encrypted storage. */
final class PersonalizationStorageMigration {
    private static final long MAX_HISTORY_BYTES = 32L * 1024L * 1024L;
    private static final int COPY_BUFFER_BYTES = 16 * 1024;

    private PersonalizationStorageMigration() {}

    /**
     * Copies only the native UserHistoryDictionary files. The source is removed only after an
     * atomic target commit and byte-for-byte verification. A conflict or I/O error fails closed
     * and leaves both copies intact for a later retry.
     */
    static boolean moveLegacyHistoryFiles(@NonNull final File deviceFilesDir,
            @NonNull final File credentialFilesDir, @NonNull final String prefix) {
        if (!deviceFilesDir.exists()) return true;
        if (!deviceFilesDir.isDirectory()) return false;
        final File[] legacyFiles = deviceFilesDir.listFiles((dir, name) ->
                name.startsWith(prefix) && name.endsWith(".dict"));
        if (legacyFiles == null) return false;
        if (legacyFiles.length == 0) return true;
        if (legacyFiles.length > 32 || !credentialFilesDir.isDirectory()
                && !credentialFilesDir.mkdirs()) return false;

        for (final File source : legacyFiles) {
            if (!source.isFile() || source.length() > MAX_HISTORY_BYTES) {
                return false;
            }
            final File target = new File(credentialFilesDir, source.getName());
            if (target.exists()) {
                if (!sameContent(source, target)) return false;
                if (!source.delete() && source.exists()) return false;
                continue;
            }

            final AtomicFile atomic = new AtomicFile(target);
            FileOutputStream output = null;
            try (FileInputStream input = new FileInputStream(source)) {
                output = atomic.startWrite();
                final byte[] buffer = new byte[COPY_BUFFER_BYTES];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.getFD().sync();
                atomic.finishWrite(output);
                output = null;
                if (!sameContent(source, target)) {
                    atomic.delete();
                    return false;
                }
                if (!source.delete() && source.exists()) return false;
            } catch (final IOException error) {
                if (output != null) atomic.failWrite(output);
                return false;
            }
        }
        return true;
    }

    private static boolean sameContent(@NonNull final File left, @NonNull final File right) {
        if (left.length() != right.length()) return false;
        try {
            return MessageDigest.isEqual(digest(left), digest(right));
        } catch (final IOException | NoSuchAlgorithmException error) {
            return false;
        }
    }

    @NonNull
    private static byte[] digest(@NonNull final File file)
            throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            final byte[] buffer = new byte[COPY_BUFFER_BYTES];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return digest.digest();
    }
}
