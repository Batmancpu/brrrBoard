# Biometric privacy controls and private gesture — implementation plan

## Goal from the owner’s conversation

Keep everyday privacy settings direct, but make detailed BR Brain statistics and the small private note vault available only after a deliberate biometric unlock. The Privacy & protected apps settings row is the entry: pressing and holding it for about three seconds requests biometrics and opens the protected area. Device PIN/pattern/password is not an authentication fallback. A small, explicitly enrolled gesture that starts in the unused lower footer gap may type a chosen private note into the active editor. Dictation history remains disabled and discarded.

## Current source findings

- **Present:** always-incognito, package-exact protected apps, temporary secure-app opt-out scoped to one editor, password/no-learning filters, and private editor gates for suggestions/clipboard/learning.
- **Present:** encrypted bounded swipe and Emoji learning stores in no-backup storage using Android Keystore AES-GCM.
- **Missing:** biometric prompt or lock lifecycle, biometric-only protected settings destination, protected statistics gate, private notes vault, gesture enrollment/recognition, and a write/read encrypted persistence path for the learned user-history dictionary.
- **Voice:** the latest release removes retained recovery transcripts. Keep this behavior: no voice-history feature is planned.
- **Repository status:** the checkout has local modifications from the prior voice task; preserve them and do not reset or overwrite unrelated source.

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

## Implemented in the current source checkout (2026-11-23)

- The ordinary Privacy & protected apps row still opens its current settings on a tap. A 2.5-second press opens a separate protected route.
- That route asks Android for `BIOMETRIC_STRONG` only. It does not include `DEVICE_CREDENTIAL`; on Android 10 and older, this protected feature deliberately stays unavailable instead of using a PIN/password fallback.
- A new one-note vault stores a bounded note as AES-GCM ciphertext in `noBackupFilesDir`, via `AtomicFile`. The Android Keystore AES key requires strong biometric authorization for every encryption/decryption operation and is invalidated by biometric enrollment changes. Decrypt/encrypt uses a biometric `CryptoObject`; the app never writes the note as a preference or log entry. The UI does not copy or paste the secret automatically and clears its note state when the protected route leaves the foreground.
- Ordinary BR Brain now exposes a small status view without personal usage counters. Detailed live counters are reached through the protected route.
- The app manifest already disables Android backup; vault data additionally resides in `noBackupFilesDir`. Existing voice changes continue to discard transcript recovery data.

## Not yet implemented or verified

- The proposed private keyboard gesture that types a vault value is **not active**. The footer's center area is already an owner-scoped temporary voice/cancel action; routing arbitrary secret gestures through it without interfering with mic, cancellation, trackpad, and accessibility input needs a distinct input-state implementation. No gesture was added that could silently type a secret. The vault currently offers a manual, authenticated read/edit path only.
- Existing native `UserHistoryDictionary` files are still app-private but are not converted to an encrypted-at-rest format. The native decoder opens those files directly, so a safe encrypted migration needs an explicit open/decrypt/close/encrypt lifecycle and crash-safe upgrade before it can be claimed. This code does not modify or erase learned words.
- The new Kotlin code compiled on GitHub Actions at source SHA `7e4ec7b397f868362753aa07d04b862d2a50f5fe`; the debug build and repository test gates passed in [run 45112619321](https://github.com/Batmancpu/brrrBoard/actions/runs/45112619321). It produced a 42,519,682-byte debug APK artifact, which is not an original-signer release. The workspace Gradle download itself remains blocked with `Connection refused`. Android 15/Oppo biometric behavior still needs device verification before release.

## Research used for the implementation

- Android's official [BiometricPrompt guide](https://developer.android.com/identity/sign-in/biometric-auth) and [BiometricPrompt API](https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt.Builder) describe strong-only authentication selection and unsupported authenticator combinations on older Android versions. This implementation therefore rejects older versions rather than quietly adding device credentials.
- Android's [Keystore authentication guidance](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setUserAuthenticationParameters(int,int)) documents per-operation authorization when the timeout is zero and the authentication-type binding. The vault uses that mode and passes the cipher through `CryptoObject`, binding the prompt to the actual AES-GCM operation.
- Reddit/F-Droid vault discussions were treated as usability examples only. They do not establish the security properties; those come from Android platform documentation above.

- GitHub candidate branch: `feature/biometric-private-vault-20261123`; the latest source documentation commit records the successful exact-source build above. This is not a product release.
