// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice;

import org.junit.Test;
import java.security.GeneralSecurityException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static org.junit.Assert.*;

public class RecoveredTranscriptVaultTest {
    @Test public void transcriptEnvelopeRoundTripsAndDetectsTampering() throws Exception {
        final KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        final SecretKey key = generator.generateKey();
        final byte[] plain = "speech \ud83c\udf99 recorded".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final byte[] sealed = RecoveredTranscriptVault.sealWithKey(key, plain);
        assertArrayEquals(plain, RecoveredTranscriptVault.openWithKey(key, sealed));
        sealed[sealed.length - 1] ^= 1;
        assertThrows(GeneralSecurityException.class,
                () -> RecoveredTranscriptVault.openWithKey(key, sealed));
    }

    @Test public void rejectsInvalidEnvelopeWithoutPlaintextFallback() throws Exception {
        final KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        assertThrows(GeneralSecurityException.class,
                () -> RecoveredTranscriptVault.openWithKey(generator.generateKey(), new byte[128]));
    }
}
