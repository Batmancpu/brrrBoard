# Biometric privacy controls and private gesture — implementation plan

## Goal from the owner’s conversation

Keep everyday privacy settings direct, but make detailed BR Brain statistics and the small private note vault available only after a deliberate biometric unlock. The Privacy & protected apps settings row is the entry: pressing and holding it for about three seconds requests biometrics and opens the protected area. Device PIN/pattern/password is not an authentication fallback. A small, explicitly enrolled gesture that starts in the unused lower footer gap may type a chosen private note into the active editor. Dictation history remains disabled and discarded.

## Current source findings

- **Present:** always-incognito, package-exact protected apps, temporary secure-app opt-out scoped to one editor, password/no-learning filters, and private editor gates for suggestions/clipboard/learning.
- **Present in the protected-area source:** a 2.5-second long-press entry, strong-biometric-only prompt, secure-window protection, foreground lock of the note UI, a bounded AES-GCM/Android-Keystore private note, protected BR Brain counters, and an opt-in one-use footer gesture that is authorized by a fresh biometric operation and expires after 30 seconds.
- **Present:** bounded swipe and Emoji learning records use Android Keystore AES-GCM. UserHistoryDictionary remains a native dictionary format, so it is protected by Android credential-encrypted storage but is not app-layer AES-GCM sealed.
- **Voice:** transcript recovery has no write path. Existing recovery files are purged; no dictation history is retained.
- **Storage boundary:** the app's default storage is credential-encrypted. After first unlock, legacy history, clipboard database, and encrypted no-backup records are migrated from device-protected storage. Settings, voice models, and the user-installed gesture library continue using explicit device-protected paths to preserve Direct Boot and already-installed large models.

## Work sequence

### 1. Establish the sensitive-data inventory and unlock boundary

1. Trace every BR Brain statistic, learned-word view/action, personal dictionary screen, backup/export path, reset action, and private/secure editor gate.
2. Keep existing ordinary controls visible in Privacy & protected apps. Only the detailed/private dashboard and vault contents move behind the biometric gate.
3. Implement the entry-row long-press as a 2.5–3 s gesture with progress feedback. A short tap continues to open ordinary Privacy settings. Cancel when pointer leaves the row, another pointer goes down, lifecycle changes, or the screen detaches.
4. Use AndroidX `BiometricPrompt` with strong biometrics only. Require an enrolled, currently available biometric; do not enable device-credential fallback. On cancellation, lockout, no enrollment, or unavailable hardware, remain locked and show a clear on-screen explanation. Never keep an unlocked flag across process death.
5. Re-authenticate on protected-screen entry and after backgrounding/timeout; clear decrypted UI state when leaving the protected screen.

### 2. Protect on-device learning and statistics

1. Inventory files, preferences, databases, telemetry and logs. No typed strings, secret contents, transcripts, or app names may be logged or included in diagnostics.
2. Use private app storage and Android Keystore AES-GCM for user-specific learned text that needs app-level encryption. Authenticate before decrypting protected statistics or vault contents; keep encryption keys non-exportable and bind the protected key to strong biometric authentication where Android API support permits.
3. Migrate learned user-history data transactionally: decrypt old app-private data, encrypt new format, verify it, atomically replace, then securely delete old data. If migration fails, preserve the old data and disable the feature rather than silently losing words or writing plaintext.
4. Respect incognito, passwords, secure apps, `IME_FLAG_NO_PERSONALIZED_LEARNING`, user personalization-off, and sensitive field types before collection and again before persistence.
5. Ensure uninstall removes app-private data/keys by Android app lifecycle. Backups must exclude vault/key material and private learning by default.

### 3. Add a small private note vault

1. The protected dashboard can create, edit, delete and copy a small bounded number/size of notes. Show a preview only after biometric auth; do not put note text in clipboard automatically.
2. Encrypt vault records at rest, bound payloads, use atomic writes, and zero temporary byte arrays where practical. Do not sync or back up vault data.
3. Provide explicit copy/insert with a visible confirmation and clear clipboard ownership/expiry where Android supports it. No automatic paste on unlock.
4. Reset/delete actions require biometric confirmation and have clear, reversible UI where feasible. Removing the biometric enrollment invalidates the protected key and makes data inaccessible rather than falling back to the device credential.

### 4. Add the optional private-note gesture

1. Add a default-off setting inside the authenticated vault to enable and enroll one or more simple gestures. Require biometric auth to enroll, change, reveal, or delete the mapped note.
2. Recognize gestures only when a touch starts and remains inside the documented reserved gap between the space bar and footer icons. All other keyboard regions keep normal typing, swipe, cursor, Emoji and mic behavior.
3. Add a distinct hold-to-arm / trace / release state with live visual feedback, minimum duration, path bounds and a cancel affordance. Do not trigger on an ordinary key path or simple circle while typing.
4. On a match, require a second deliberate confirmation by default, then insert only into a normal, non-password, non-incognito, non-secure, learning-allowed text editor. Never insert into search, URL, payment, OTP, password, protected app, or `NO_PERSONALIZED_LEARNING` fields.
5. Put this experimental feature behind a default-off toggle and leave it off if gesture classification cannot be made reliable across keyboard sizes, insets, handedness, and accessibility modes. Keep vault copy/paste as the safe available path.

### 5. Security review, tests, build and release

1. Add policy tests for biometric cancellation/lockout/no-enrollment, timeout/backgrounding, migration failure, tampered ciphertext, uninstall/backup boundaries, incognito/password/no-learning exclusions, and every gesture near-boundary/non-match case.
2. Review exports, Android backup rules, logs, crash diagnostics, clipboard behavior, and key invalidation. Confirm no network permission or telemetry is introduced.
3. Build and run exact-source Android CI. Fix failures rather than weakening gates. Publish only through the existing original-signer release workflow, without bundling large models.
4. Physical acceptance after install: short tap still opens normal privacy settings; 3-second hold requests fingerprint; failed/cancelled fingerprint never opens protected content; app background relocks; notes decrypt only after fingerprint; gesture works only from the reserved gap and is inert by default; private modes reject insertion; no dictation history appears.

## Security decisions and limits

- “Biometric only” means Android strong biometric authentication and no device-credential fallback. If the user has no enrolled strong biometric or the sensor is locked out, protected content stays unavailable. Do not fake a fallback with an in-app PIN unless the owner separately specifies a secure enrollment/recovery scheme.
- Biometrics gate access; they do not make the phone or other apps biometric-locked.
- Raw gesture traces must not be persisted. Store only the minimum encrypted gesture template required, and do not let the recognizer record while normal key/swipe interaction is active.
- Android cannot guarantee physical flash erasure of old file blocks; migration deletes old files/keys after verified replacement and relies on app-private storage and Keystore guarantees.
- Exact biometric strength and authentication-bound encryption behavior varies by Android version/OEM and needs device verification on the owner's Oppo Android 15 device.

## Implemented in the current source checkout (2026-11-23; candidate, release pending)

- The ordinary Privacy & protected apps row still opens its current settings on a tap. A 2.5-second press opens a separate protected route.
- That route asks Android for `BIOMETRIC_STRONG` only. It does not include `DEVICE_CREDENTIAL`; on Android 10 and older, this protected feature deliberately stays unavailable instead of using a PIN/password fallback.
- A new one-note vault stores a bounded note as AES-GCM ciphertext in `noBackupFilesDir`, via `AtomicFile`. The Android Keystore AES key requires strong biometric authorization for every encryption/decryption operation and is invalidated by biometric enrollment changes. Decrypt/encrypt uses a biometric `CryptoObject`; the app never writes the note as a preference or log entry. The UI does not copy or paste the secret automatically and clears its note state when the protected route leaves the foreground.
- The protected route adds `FLAG_SECURE` while it is open to prevent screenshots and task-switcher previews, clears the decrypted note UI when the route leaves the foreground, and zeros the temporary decrypted byte buffer after creating the UI string. A freshly biometric-authorized one-use gesture may remain armed for the explicit Settings-to-keyboard transition only, and expires after 30 seconds; app process death still destroys it. Deleting the note also deletes its Keystore key.
- Ordinary BR Brain now exposes a small status view without personal usage counters. Detailed live counters are reached through the protected route.
- The app manifest already disables Android backup; vault data additionally resides in `noBackupFilesDir`. Existing voice changes continue to discard transcript recovery data.
- A default-off one-use circle gesture is now available inside the decrypted note screen. The user explicitly arms it for 30 seconds, after which a bounded recognition zone appears in the center footer. It consumes the secret once and only in normal general prose fields; incognito, password/no-learning, secure-app, Toolbar, dictation, and accessibility touch-exploration states reject it. The secret itself remains only in short-lived process memory; no gesture trace is stored. The input path uses the existing private voice-origin transaction so it does not update personal learning.

## Remaining implementation and verification

- The circle gesture is still an experimental source candidate. It remains default-off, is explicitly armed for one use, and is limited to a reserved footer hit region. Exact boundary/false-trigger/reduced-motion/TalkBack and Oppo behavior still need device verification; the feature must stay off if that acceptance fails.
- History and clipboard data move into Android credential-encrypted storage at rest, but are not app-layer AES-GCM encrypted. The note vault and the compact Emoji/swipe learning records use app-layer AES-GCM. This distinction is deliberate and must stay explicit in UI/docs.
- Migration runs only after first unlock. It verifies copied native-history bytes before deleting the legacy source; the clipboard database uses Android's supported `moveDatabaseFrom` operation. Conflicts fail closed and preserve the old source. Exact-source Android CI must validate these changes before release.
- The current source candidate is not an original-signer release. Android 15/Oppo biometric and gesture behavior still needs physical device verification. Do not mark the overall feature complete based only on a clean build.

## Research used for the implementation

- Android's official [BiometricPrompt guide](https://developer.android.com/identity/sign-in/biometric-auth) and [BiometricPrompt API](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt.Builder) describe strong-only authentication selection and unsupported authenticator combinations on older Android versions. This implementation therefore rejects older versions rather than quietly adding device credentials.
- Android's [Keystore authentication guidance](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setUserAuthenticationParameters(int,int)) documents per-operation authorization when the timeout is zero and the authentication-type binding. The vault uses that mode and passes the cipher through `CryptoObject`, binding the prompt to the actual AES-GCM operation.
- Android's [Auto Backup guidance](https://developer.android.com/identity/data/autobackup) confirms `noBackupFilesDir` is excluded from backup. The protected screen also uses Android's secure-window flag to suppress screenshots and task-switcher thumbnails while open.
- Reddit/F-Droid vault discussions were treated as usability examples only. They do not establish the security properties; those come from Android platform documentation above.
