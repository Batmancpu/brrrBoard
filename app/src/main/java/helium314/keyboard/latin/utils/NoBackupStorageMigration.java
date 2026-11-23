// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin.utils;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.util.AtomicFile;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Moves small encrypted app records from legacy device-protected to credential-protected storage. */
public final class NoBackupStorageMigration {
    private static final long MAX_RECORD_BYTES = 16L * 1024L * 1024L;
    private static final int BUFFER_BYTES = 16 * 1024;

    private NoBackupStorageMigration() { }

    /** Returns true only when the record is available at its new CE location or did not exist. */
    public static boolean migrateEncryptedRecord(@NonNull final Context context,
            @NonNull final String filename) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null || !userManager.isUserUnlocked()) return false;
        final File sourceDir = context.createDeviceProtectedStorageContext().getNoBackupFilesDir();
        final File targetDir = context.getNoBackupFilesDir();
        final File source = new File(sourceDir, filename);
        if (!source.exists()) return true;
        if (!source.isFile() || source.length() > MAX_RECORD_BYTES) return false;
        if (!targetDir.isDirectory() && !targetDir.mkdirs()) return false;
        final File target = new File(targetDir, filename);
        if (target.exists()) {
            if (!sameContent(source, target)) return false;
            return !source.exists() || source.delete();
        }

        final AtomicFile atomic = new AtomicFile(target);
        FileOutputStream output = null;
        try (FileInputStream input = new FileInputStream(source)) {
            output = atomic.startWrite();
            final byte[] buffer = new byte[BUFFER_BYTES];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
            atomic.finishWrite(output);
            output = null;
            if (!sameContent(source, target)) {
                atomic.delete();
                return false;
            }
            return !source.exists() || source.delete();
        } catch (final IOException error) {
            if (output != null) atomic.failWrite(output);
            return false;
        }
    }

    private static boolean sameContent(@NonNull final File left, @NonNull final File right) {
        if (!right.isFile() || left.length() != right.length()) return false;
        try {
            return MessageDigest.isEqual(digest(left), digest(right));
        } catch (final IOException | NoSuchAlgorithmException error) {
            return false;
        }
    }

    @NonNull private static byte[] digest(@NonNull final File file)
            throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            final byte[] buffer = new byte[BUFFER_BYTES];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return digest.digest();
    }
}
