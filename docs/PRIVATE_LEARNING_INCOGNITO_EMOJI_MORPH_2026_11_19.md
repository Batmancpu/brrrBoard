# brrBoard: private learning + emoji incognito morph (2026-11-19)

## Scope and upgrade invariants

- Same Android application ID `com.mangoloads.board`, normal Obtainium signing key,
  same encrypted correction-vault filename `brr_swipe_corrections_v2.aesgcm`
  under `noBackupFilesDir`, same Android Keystore alias
  `com.mangoloads.board.brr_brain_v2`, same legacy v1 migration rules.
- Do **not** rename, clear, overwrite or delete app-private UserHistoryDictionary,
  emoji `pref_adaptive_emoji_metadata`, Emoji custom aliases, swipe learning or
  existing offline voice model in `noBackupFilesDir` as part of an APK update.
- Android signed in-place upgrade retains app data. Uninstall, "Clear storage",
  changing signing certificate/application ID or a device factory reset are **not**
  in-place upgrades and may lose the history/key. Never advise them to recover.
- Android auto-backup/cloud restoration and D2D extraction are disabled for app
  data. That does **not** wipe existing user data on an in-place update. It does
  mean settings/learning will not be automatically copied to a *new* install.
- The correction-vault AES-256-GCM remains authenticated, fail-closed and sealed.
  Native historical word dictionaries and adaptive Emoji prefs are still
  **app-sandbox-private, but not individually AES-encrypted**. Do not claim
  every legacy store has at-rest AES encryption. A complete encryption migration
  needs native-dictionary lifecycle support and a separately tested rollback.
- No `INTERNET` permission, no new model weights, no network learning, no
  migration/telemetry job, no changes to voice signing or startup.

## User interaction

- Same original 48dp footer button, 31dp white/dynamic-icon-tint geometric smile,
  unchanged content placement, no extra bottom bar or ring.
- Single tap opens Emoji or returns to ABC. A disjoint-pair tap recognizer waits
  for a second tap before confirming a single tap; double tap does not flash the Emoji panel.
  Standard Android `ViewConfiguration` double-tap window applies (~300ms, OEM
  dependent); long-press/slide opens the existing three shortcuts unchanged.
- Double tap in alphabet mode toggles the existing `always_incognito_mode`
  preference through `Settings.toggleAlwaysIncognitoMode`, which recomputes
  `SettingsValues.mIncognitoModeEnabled`. Protected/no-personalized-learning
  fields force privacy; toggling them off is intentionally blocked.
- Normal Android TalkBack activation remains a single Emoji click; a named
  accessibility action toggles incognito, and a state description reports it.
- Switching **into** incognito cancels an already active/processing dictation.
  No write to the learned correction vault, user-history n-grams, Emoji recents,
  adaptive Emoji counters or clipboard history should occur for subsequent
  private events; their existing records remain intact and can be reused later.
- First tap no longer navigates immediately; the Android-confirmed single-tap
  delay is the deliberate cost of a conflict-free double-tap action.

## Animation choreography

All paths are original Canvas vectors following the 24-unit artwork, centered
on the existing 31dp icon; no blur bitmap, layout animation or new icon hitbox.
Durations are *chosen brrBoard timings*, not undocumented official Apple values.

| Transition | Window | Visual channel |
|---|---|---|
| On | 0–680ms | 0–28% hat descends and settles on crown |
| On | ~130–580ms | Face glances downward and rises |
| On | ~400–580ms | Smoked glasses emerge while the face is down |
| On | ~544–680ms | White specular streak sweeps lens and rests as a glint |
| Off | 0–630ms | Face bows with shades on |
| Off | ~202–365ms | Sunglasses dissolve, original white eyes reappear |
| Off | ~360–510ms | Face rises and looks ahead |
| Off | ~440–630ms | Hat brim recedes and crown falls behind smile |

Each channel uses smoothstep easing. A new double-tap during the animation
samples the current **actual drawn pose** and begins the opposite sequence from
there; no finish-first blocking, endpoint pop or stale deferred animation.
Honor the system animator scale (including animations disabled).

## Validation and limitations

- Unit motion tests: entrance order, exit order, 24 reversal samples, bounds.
- Existing Python release/source tests and `git diff --check` should pass.
- Gradle cannot be executed in this scratch runtime without the Gradle
  distribution/Android SDK; use exact-source-SHA GitHub debug and *original
  signer* Obtainium release CI to validate Android compilation and packaging.
- On phone: verify double tap (<300ms), regular single tap (confirmation delay),
  hold/drag shortcuts, protected/password field, Android accessibility action,
  light/dark/frosted theme recoloring, mid-flight repeated double taps,
  motion-disabled preference, active voice interruption, and data learned
  before/after signed update. Device acceptance is pending until actual testing.
