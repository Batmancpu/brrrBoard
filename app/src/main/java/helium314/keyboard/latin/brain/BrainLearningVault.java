// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Authenticated AES-256-GCM encryption for bounded, no-backup local learning.
 * The non-exportable key is generated and kept by AndroidKeyStore. Never falls
 * back to plaintext if KeyStore is unavailable. No networking or text logging.
 */
public final class BrainLearningVault {
    private static final String KEY_ALIAS = "com.mangoloads.board.brr_brain_v2";
    private static final byte[] MAGIC = new byte[]{'B', 'R', 'R', 2};
    private static final int GCM_IV_BYTES = 12;
    private static final int MAX_SEALED_BYTES = 16 * 1024;
    private BrainLearningVault() {}

    @NonNull
    private static synchronized SecretKey deviceKey() throws GeneralSecurityException, IOException {
        final KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null, null);
        final java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        final KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build());
        return generator.generateKey();
    }

    public static byte[] seal(@NonNull final byte[] plaintext)
            throws GeneralSecurityException, IOException {
        return sealWithKey(deviceKey(), plaintext);
    }

    public static byte[] open(@NonNull final byte[] ciphertext)
            throws GeneralSecurityException, IOException {
        return openWithKey(deviceKey(), ciphertext);
    }

    /** Visible to unit tests so cryptographic integrity can be tested without OEM Keystore. */
    static byte[] sealWithKey(@NonNull final SecretKey key, @NonNull final byte[] plaintext)
            throws GeneralSecurityException, IOException {
        if (plaintext.length > MAX_SEALED_BYTES - 64)
            throw new GeneralSecurityException("Learning record exceeds bound");
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        final byte[] iv = cipher.getIV();
        if (iv == null || iv.length != GCM_IV_BYTES)
            throw new GeneralSecurityException("Unexpected GCM nonce");
        final byte[] sealed = cipher.doFinal(plaintext);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(sealed.length + 17);
        out.write(MAGIC);
        out.write(iv);
        out.write(sealed);
        return out.toByteArray();
    }

    static byte[] openWithKey(@NonNull final SecretKey key, @NonNull final byte[] sealed)
            throws GeneralSecurityException {
        if (sealed.length < MAGIC.length + GCM_IV_BYTES + 16
                || sealed.length > MAX_SEALED_BYTES
                || !Arrays.equals(MAGIC, Arrays.copyOfRange(sealed, 0, MAGIC.length)))
            throw new GeneralSecurityException("Invalid sealed learning record");
        final byte[] iv = Arrays.copyOfRange(sealed, MAGIC.length, MAGIC.length + GCM_IV_BYTES);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(sealed, MAGIC.length + GCM_IV_BYTES,
                sealed.length - MAGIC.length - GCM_IV_BYTES);
    }
}
