# brrBrain local learning vault — AES-256-GCM (2026-11-19)

## User priority
Owner wants adaptive swipe learning ON for normal text fields and OFF in incognito/password/no-personalized-learning, with privacy preserved; wants local encrypted storage and optionally future temporary touch paths that get distilled and deleted. Current model learns only bounded key-skeleton -> explicitly confirmed word associations, max 64, not raw coordinates.

## Source implementation
- New BrainLearningVault Java class: AndroidKeyStore non-exportable AES-256 key, GCM with fresh Android-generated 12-byte nonce per write, 128-bit authentication tag, strict magic/version header and max sealed length. No INTERNET permission, app-private noBackup files, no plaintext fallback or telemetry.
- SwipeCorrectionLearner v2 store named brr_swipe_corrections_v2.aesgcm. Existing v1 flat plain corrections are read *only* to migrate; new data is sealed to AtomicFile first, old plaintext AtomicFile removed only after successful finishWrite. If the encrypted file exists but is unreadable/Keystore unavailable, do not overwrite ciphertext; continue RAM-only, leave source for recovery; user can explicitly choose Erase if desired.
- Real-time input hot path updates in-memory map; expensive seal/AtomicFile runs on the existing serialized daemon writer. Each queued write checks a revision so reset/outdated snapshots do not resurrect data. Stored sample is a verified correction pair, never raw swipe coordinates, editor texts, app identifiers, host messages or full traces.
- Dashboard shows true vault status, distinguishing encrypted, legacy migration pending, ready but empty, and unavailable RAM-only. No fake encryption indicator.
- BrainLearningVaultTest: round-trip, unique nonce different ciphertext for identical plaintext, ciphertext tampering rejection, truncated file and wrong-key rejection with ordinary unit test AES key; real Android Keystore/migration still require device acceptance.
- Existing Android OS file-based encryption also operates at device level. This adds app-level authenticated encryption for the *new correction-pair store only*. Other personalized native dictionary/Emoji frequency SharedPrefs are not yet encrypted with this layer. Do not falsely describe ALL typing data as end-to-end encrypted.

## QA and rollback gates
- Same existing original Android app signing and keystore, not replaced with the new **learning-data key**.
- Must pass Android debug + production unit suite; only publish signed Obtainium after both green. No AI model added and no change to original swipe native decoder.
- On real Oppo: create hapy->happy twice, see Ready 1 and encrypted status; restart IME/device, verify same pair remains; call Erase and verify pair gone. Verify old v1 pairs migrate to v2 without loss, no plaintext v1 remains when migration succeeded, no private words in logcat or backup. Test password/incognito/off; factory installation without legacy should work.
- If Keystore unavailable (direct-boot/lock/invalidated), never crash IME or write plaintext. Surface RAM-only status and preserve existing encrypted file; owner can opt to erase after unlocking or investigate.
- Raw touch paths NOT persisted in this patch. A future small encrypted, time-limited buffer can be added only after a validated spatial-update+secure TTL deletion algorithm and isolated performance benchmark; collecting raw traces without a consumer would increase privacy risk for no user benefit.

## Unfinished integration
Clipboard Search still has owner-unaccepted rainbow and glass-under-footer behavior; separate InputView shows that even with Search active it feeds clipboard_list into footer renderer, a remaining exact bug. Next targeted source patch should disable the Clipboard footer glass source during Search but retain original source in Browse, and brighten rainbow match/highlighter. Dashboard/strip source is in v1.0.135 with both workflows green; this secure vault patch has its own CI and Oppo gate.
