// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;

/** Pure codec tests; AndroidKeystore integration is verified on actual Android. */
public class BrainLearningVaultTest {
    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");

    @Test public void sealedFileDoesNotExposePlainWordPairsAndRoundTrips() throws Exception {
        final byte[] plain = "hapy\thappy\t2\t1700000000000\n".getBytes(StandardCharsets.UTF_8);
        final byte[] sealed = BrainLearningVault.sealWithKey(key, plain);
        assertFalse(Arrays.equals(plain, sealed));
        assertFalse(new String(sealed, StandardCharsets.ISO_8859_1).contains("happy"));
        assertArrayEquals(plain, BrainLearningVault.openWithKey(key, sealed));
        assertFalse(Arrays.equals(sealed, BrainLearningVault.sealWithKey(key, plain)));
    }

    @Test public void tamperAndTruncationAreRejected() throws Exception {
        final byte[] sealed = BrainLearningVault.sealWithKey(key,
                "private correction".getBytes(StandardCharsets.UTF_8));
        sealed[sealed.length - 1] ^= 1;
        try {
            BrainLearningVault.openWithKey(key, sealed);
            fail("Modified authenticated ciphertext must not decrypt");
        } catch (GeneralSecurityException expected) {
            // GCM must refuse modified data.
        }
        try {
            BrainLearningVault.openWithKey(key, new byte[]{'B', 'R', 'R', 2});
            fail("Truncated encrypted record must be rejected");
        } catch (GeneralSecurityException expected) {
            // Correct.
        }
    }

    @Test public void wrongKeyCannotRecoverLearning() throws Exception {
        final byte[] sealed = BrainLearningVault.sealWithKey(key, "hap->happy".getBytes(StandardCharsets.UTF_8));
        final byte[] other = new byte[32];
        other[0] = 42;
        try {
            BrainLearningVault.openWithKey(new SecretKeySpec(other, "AES"), sealed);
            fail("Different Keystore key must not recover the learning store");
        } catch (GeneralSecurityException expected) {
            // Correct.
        }
    }

    @Test public void changedVersionAndNonceAreAuthenticatedAndRejected() throws Exception {
        final byte[] sealed = BrainLearningVault.sealWithKey(key,
                "synthetic\tcorrection\t2\t1700000000000\n".getBytes(StandardCharsets.UTF_8));
        final byte[] versionChanged = sealed.clone();
        versionChanged[3] = 3;
        assertThrows(GeneralSecurityException.class,
                () -> BrainLearningVault.openWithKey(key, versionChanged));
        final byte[] nonceChanged = sealed.clone();
        nonceChanged[6] ^= 1;
        assertThrows(GeneralSecurityException.class,
                () -> BrainLearningVault.openWithKey(key, nonceChanged));
    }

    @Test public void oversizedOrEmptyRecordsFailClosed() {
        assertThrows(GeneralSecurityException.class,
                () -> BrainLearningVault.openWithKey(key, new byte[16 * 1024 + 1]));
        assertThrows(GeneralSecurityException.class,
                () -> BrainLearningVault.openWithKey(key, new byte[0]));
    }
}
