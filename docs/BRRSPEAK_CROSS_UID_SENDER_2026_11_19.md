# brrBoard → brrSpeak cross-UID PCM sender — implementation ledger (2026-11-19)

## Authorization and baseline

The owner gave an explicit **GO in the new chat** to implement the keyboard microphone sender.
The old voice WAIT notes in AGENTS.md and PROJECT_CONTEXT.md are historical. Before editing,
live brrBoard main was `bd5cc7c2f4e3592eeb845e1c3454677acf63f04c`, original-signed
Obtainium release **v1.0.147**, debug Action **44486092201** SUCCESS and original-signer
Action **44486092469** SUCCESS. Both workflows were verified terminal; the previous
v1.0.145 checkpoint is no longer the latest. Preserve the corrective UI motion/Emoji/Clipboard
work in that baseline; this voice change makes no changes to it.

brrSpeak's independent main is `49cb5d7bd70da5e9facf5cf41a802149c718a951`,
experimental v5 `142005`, with a **successful same-package, different-process** test
(153596 nonzero PCM bytes, around 4.8 seconds, and real transcription).
That is **not evidence of separate app/UID interoperability**. The original Dictate
remains separately installed, with no request to change its code or signing.

Upstream DevEmperor/DictateKeyboard issue #489 is closed. The maintainer reproduced Android
15's third-party RecognitionService capability/permission restriction on an emulator and
identified caller-supplied `EXTRA_AUDIO_SOURCE` as the viable path; source changes in
upstream `84adfa9` did not themselves provide this extra's receiver. brrSpeak v5 did.

## Scope of sender candidate

- An **explicit** `com.mangoloads.speak` / `DictateRecognitionService` component,
  filtered by service discovery **and brrSpeak versionCode >= 142005**. No automatic
  fallback to purchased `net.devemperor.dictate`, the Android default, or an OEM service.
- Foreground brrBoard permission and visibility checks before recording; Android 13+
  `ParcelFileDescriptor.createPipe()` read end in `RecognizerIntent.EXTRA_AUDIO_SOURCE`
  with channel count 1, PCM16 encoding, sample rate 16000. The keyboard opens the only
  `AudioRecord` after brrSpeak `onReadyForSpeech`, using Android VOICE_RECOGNITION source,
  then writes PCM from a background worker (no keyboard/UI thread pipe writes).
- The next mic tap stops **only** the keyboard recorder. Worker closes pipe write end
  to give the receiver EOF; no `SpeechRecognizer.stopListening()` that could truncate audio.
  Stale callbacks are gated by recognizer identity, generation, IME visibility and the
  same InputConnection. Results are committed once through the existing `ime.onTextInput`.
- Cancellation, keyboard dismissal, editor switch, teardown, permission failure and
  recognizer errors close local recorder and pipe endpoints, cancel/destroy the session,
  remove the red/processing mic status and do not store/print PCM or text.
- **Gesture resolution:** at idle, brrBoard's existing GestureDetector delays single-tap
  confirmation until the double-tap switch window has passed. An idle double-tap switches
  keyboards **without opening the microphone**. Once the mic is actively recording, the
  next tap stops immediately, without another double-tap delay; a processing tap cancels.
  This preserves switching and protects against an accidental stream on switching, at the
  cost of the existing brief initial single-tap confirmation delay. Touch-exploration
  accessibility clicks and dedicated switch action are preserved.
- brrBoard's application ID `com.mangoloads.board`, original pinned signing, offline
  permission restriction, other keyboard UI/features and original Dictate identity remain.
  No brrBoard network permission, secret, raw transcript, PCM, or private device identifiers
  were added. brrSpeak still uses the provider explicitly configured by the owner.

## Verification and release gates

Candidate source paths:

- `app/src/main/java/helium314/keyboard/latin/{InlineDictation,InlineVoiceSessionState,KeyboardPcmAudioSource,BoardFooterView,FooterMicTapPolicy}.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/helium314/keyboard/latin/{BrrSpeakAudioContractTest,InlineVoiceSessionStateTest}.kt`
- `.github/workflows/obtainium-release.yml` (wait for **exact source SHA debug Action success** before original signing/release)

Static preflight: `strings.xml` parsed as XML and the release workflow parsed as YAML.
No JDK, Android SDK or physical Android device is attached to the current local executor;
the authoritative compile/unit/debug and signed APK checks must run through GitHub Actions.
Both Actions, signed release tag, APK digest and output are **PENDING** for the voice commit.

Phone tests **still required** on Android 15 / ColorOS 15 (never mark as passed from CI):

1. Keep both original Dictate and brrSpeak installed. Configure and verify brrSpeak's provider
   first (no credentials entered in brrBoard). Verify the latest brrSpeak v5 or newer.
2. In an ordinary text field, tap mic once; after a short gesture-confirmation delay it
   should show Starting, then a **red active** mic only when actual keyboard capture began.
   Speak a short English phrase, tap again, wait for text in that **same** field. No app
   launch, keyboard switch or popup should occur. Verify words were not duplicated.
3. Idle quick double-tap still switches keyboards **and never activates the mic**. Return,
   then start-stop-start several sessions including a 1-second phrase and longer speech.
4. Cancel during service binding, recording and transcription; hide keyboard, rotate, change
   fields/app, revoke mic permission, deny microphone via privacy tile and re-open keyboard.
   No stuck red mic, leaked recorder, foreign-field insertion or late transcript.
5. Silence, no network/provider failure, phone call or another mic-using app, Bluetooth
   routing, and both apps installed must yield bounded errors/recovery. Android error 9
   is **not** claimed fixed without actual cross-UID success.
6. Confirm swipe typing, Emoji/Clipboard/GIF, Big Numpad, footer/motion and Obtainium
   update over prior original-signed v1.0.147. Debug APK uses a different app ID/signature
   and is not the installation/update acceptance artifact.

## Next checkpoint

Push one narrow source commit to `main` only with a compare-and-swap expected HEAD.
Inspect both triggered GitHub Actions, fix compilation/failed tests if needed, and only
give a direct original-signed APK URL if **both** are terminal SUCCESS and the release
artifact is verified at the exact source SHA. Device cross-UID behavior remains unproven
until the owner performs the physical checklist and reports results.
