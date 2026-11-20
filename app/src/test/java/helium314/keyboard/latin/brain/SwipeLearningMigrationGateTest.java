package helium314.keyboard.latin.brain;

import org.junit.Test;
import static org.junit.Assert.*;

/** Content-free fault matrix; on-device Keystore/AtomicFile acceptance is separate. */
public class SwipeLearningMigrationGateTest {
    @Test public void deletionOnlyAfterAtomicCommitAuthenticatedEqualReadback() {
        assertTrue(SwipeLearningMigrationGate.mayDeleteLegacy(true, true, true));
        assertFalse(SwipeLearningMigrationGate.mayDeleteLegacy(false, true, true));
        assertFalse(SwipeLearningMigrationGate.mayDeleteLegacy(true, false, true));
        assertFalse(SwipeLearningMigrationGate.mayDeleteLegacy(true, true, false));
        assertFalse(SwipeLearningMigrationGate.mayDeleteLegacy(false, false, false));
    }
}
