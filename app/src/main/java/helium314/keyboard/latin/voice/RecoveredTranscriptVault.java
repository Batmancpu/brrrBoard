// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Independent AndroidKeystore envelope for explicit, bounded transcript recovery. */
public final class RecoveredTranscriptVault {
    private static final String KEY_ALIAS = "com.mangoloads.board.recovered_transcript_v1";
    private static final byte[] MAGIC = new byte[]{'B', 'R', 'T', 1};
    private static final int NONCE_BYTES = 12;
    public static final int MAX_SEALED_BYTES = 256 * 1024;

    private RecoveredTranscriptVault() { }

    private static synchronized SecretKey key() throws GeneralSecurityException, IOException {
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

    public static byte[] seal(byte[] plaintext) throws GeneralSecurityException, IOException {
        return sealWithKey(key(), plaintext);
    }

    public static byte[] open(byte[] sealed) throws GeneralSecurityException, IOException {
        return openWithKey(key(), sealed);
    }

    /** Package-visible tests exercise integrity without an emulator Keystore. */
    static byte[] sealWithKey(SecretKey key, byte[] plain)
            throws GeneralSecurityException, IOException {
        if (plain.length > MAX_SEALED_BYTES - 64)
            throw new GeneralSecurityException("Transcript recovery record exceeds bound");
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        final byte[] nonce = cipher.getIV();
        if (nonce == null || nonce.length != NONCE_BYTES)
            throw new GeneralSecurityException("Unexpected transcript nonce");
        final byte[] encrypted = cipher.doFinal(plain);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(encrypted.length + 16);
        out.write(MAGIC);
        out.write(nonce);
        out.write(encrypted);
        return out.toByteArray();
    }

    static byte[] openWithKey(SecretKey key, byte[] sealed) throws GeneralSecurityException {
        if (sealed.length < MAGIC.length + NONCE_BYTES + 16 ||
                sealed.length > MAX_SEALED_BYTES ||
                !Arrays.equals(MAGIC, Arrays.copyOfRange(sealed, 0, MAGIC.length)))
            throw new GeneralSecurityException("Invalid transcript recovery envelope");
        final byte[] nonce = Arrays.copyOfRange(sealed, MAGIC.length, MAGIC.length + NONCE_BYTES);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        return cipher.doFinal(sealed, MAGIC.length + NONCE_BYTES,
                sealed.length - MAGIC.length - NONCE_BYTES);
    }
}
