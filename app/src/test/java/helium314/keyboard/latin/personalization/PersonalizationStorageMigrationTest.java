// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin.personalization;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public final class PersonalizationStorageMigrationTest {
    @Test
    public void migratesOnlyHistoryDictionaryAndRemovesSourceAfterVerification() throws Exception {
        final File sourceDir = newDirectory("history-device-source");
        final File targetDir = newDirectory("history-credential-target");
        final byte[] dictionary = "opaque native dictionary bytes".getBytes(StandardCharsets.UTF_8);
        final File source = write(sourceDir, "UserHistoryDictionary.en.dict", dictionary);
        write(sourceDir, "unrelated.dict", "leave me".getBytes(StandardCharsets.UTF_8));

        assertTrue(PersonalizationStorageMigration.moveLegacyHistoryFiles(
                sourceDir, targetDir, "UserHistoryDictionary"));

        assertFalse(source.exists());
        assertArrayEquals(dictionary,
                java.nio.file.Files.readAllBytes(new File(targetDir, source.getName()).toPath()));
        assertTrue(new File(sourceDir, "unrelated.dict").exists());
    }

    @Test
    public void conflictingTargetPreservesBothCopiesAndFailsClosed() throws Exception {
        final File sourceDir = newDirectory("history-conflict-source");
        final File targetDir = newDirectory("history-conflict-target");
        final byte[] original = "legacy dictionary".getBytes(StandardCharsets.UTF_8);
        final byte[] current = "different credential dictionary".getBytes(StandardCharsets.UTF_8);
        final File source = write(sourceDir, "UserHistoryDictionary.en.dict", original);
        final File target = write(targetDir, source.getName(), current);

        assertFalse(PersonalizationStorageMigration.moveLegacyHistoryFiles(
                sourceDir, targetDir, "UserHistoryDictionary"));

        assertArrayEquals(original, java.nio.file.Files.readAllBytes(source.toPath()));
        assertArrayEquals(current, java.nio.file.Files.readAllBytes(target.toPath()));
    }

    @Test
    public void migratesEmptyNativeFileAndFailsClosedWhenSourceCannotBeListed() throws Exception {
        final File sourceDir = newDirectory("history-empty-source");
        final File targetDir = newDirectory("history-empty-target");
        final File empty = write(sourceDir, "UserHistoryDictionary.en.dict", new byte[0]);

        assertTrue(PersonalizationStorageMigration.moveLegacyHistoryFiles(
                sourceDir, targetDir, "UserHistoryDictionary"));
        assertFalse(empty.exists());
        assertTrue(new File(targetDir, empty.getName()).exists());

        final File blockedSource = write(sourceDir, "not-a-directory",
                "not a directory".getBytes(StandardCharsets.UTF_8));
        assertFalse(PersonalizationStorageMigration.moveLegacyHistoryFiles(
                blockedSource, targetDir, "UserHistoryDictionary"));
    }

    private static File write(final File directory, final String name, final byte[] contents)
            throws Exception {
        final File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(contents);
        }
        return file;
    }

    private static File newDirectory(final String prefix) throws Exception {
        final File base = ApplicationProvider.getApplicationContext().getCacheDir();
        final File directory = File.createTempFile(prefix, "", base);
        assertTrue(directory.delete());
        assertTrue(directory.mkdirs());
        return directory;
    }
}
