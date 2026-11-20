// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

/** Fail-closed decision for removing a legacy plaintext store after v2 migration. */
final class SwipeLearningMigrationGate {
    private SwipeLearningMigrationGate() {}

    static boolean mayDeleteLegacy(final boolean finishedAtomicWrite,
            final boolean authenticatedReadback, final boolean samePlaintext) {
        return finishedAtomicWrite && authenticatedReadback && samePlaintext;
    }
}
