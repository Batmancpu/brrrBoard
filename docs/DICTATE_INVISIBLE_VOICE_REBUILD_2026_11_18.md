# Dictate invisible voice-input rebuild — 2026-11-18

## Owner intent and evidence

The owner wants a short tap on the brrBoard footer mic to begin dictation, a second
tap to finish, and the text inserted at the same editor cursor. The brrBoard window
must stay visible; there must be no second activity, floating button, IME switch,
network permission in brrBoard, or unrequested recognizer fallback. Dictate remains
installed as the configured recognition provider and owns cloud/offline inference.
The persistent original APK signer, package `com.mangoloads.board`, three themes,
keyboard geometry, input privacy, and ordinary typing are not part of this change.

The owner previously reported that, even with brrBoard microphone permission
granted, a red mic was followed by an error originating from
`SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS`. This is *not* an Android runtime
permission denial proven at the brrBoard check. Dictate voice-search working in
YouTube exercises a different `ACTION_RECOGNIZE_SPEECH` activity and is not proof
that `RecognitionService` works on the owner's Oppo Reno 10 5G.

The existing matching maintainer discussion is
[`DevEmperor/DictateKeyboard#359`](https://github.com/DevEmperor/DictateKeyboard/issues/359).
Dictate's author confirms that only its `RecognitionService` is invisible, while
voice IME and recognition activity change the visible surface. The service was
implemented in [#67](https://github.com/DevEmperor/DictateKeyboard/issues/67).
No custom HTTP endpoint or audio-file exchange is needed.

## Why the existing two-tap path was fragile

Read directly from current upstream Dictate `main`:

- `DictateRecognitionService.onStartListening` calls `readyForSpeech` *before*
  `RecognitionSession.start` asynchronously opens the recorder.
- `RecognitionSession.stop` marks the session stopping and cancels its watchdog,
  but `DictateController.stopRecognition` only stops when the controller is
  already `UiState.Recording`. A quick stop during async startup can be dropped.
- `RecordingController` constructs `AudioRecord` with the legacy constructor
  and its application context; `DictateRecognitionService` does not propagate
  `Callback.getCallingAttributionSource()` to an attributed recorder context.
  The [Android `RecognitionService` contract](https://developer.android.com/reference/android/speech/RecognitionService#onStartListening(android.content.Intent,%20android.speech.RecognitionService.Callback))
  recommends creating a caller-attribution context and opening the mic with it.
- AOSP itself can send `ERROR_INSUFFICIENT_PERMISSIONS` after app-level
  `RECORD_AUDIO` appears granted, due to its permission/AppOps/data-delivery
  verification. See [framework source](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/RecognitionService.java).

These are verified source design risks, **not a proven diagnosis** of the Oppo
failure. Device logs for the failing session are still required. In particular,
do not claim Android attribution is the sole reason for the observed error.

An attempt to create the upstream maintainer issue through the linked GitHub
integration was rejected with HTTP 403 (`Resource not accessible by integration`).
No upstream issue was created and no other credentials were used. Issue #359
already records the invisible-recognizer integration question; the concrete
attribution/start-stop findings remain available here for an authorized upstream
report or patch.

## Focused brrBoard implementation (source candidate)

`InlineVoiceSessionState` is a new, small caller-owned phase machine:

`IDLE → STARTING → LISTENING → PROCESSING → IDLE`.

An early second tap queues a stop until `onBeginningOfSpeech`, rather than
calling `stopListening` immediately after Dictate's premature `onReadyForSpeech`.
This is not a full replacement for a Dictate recorder-ready callback: in a
silent/noisy startup where its speech detector never fires, the queued stop can
still expire under the existing eight-second no-speech timeout. The correct
Dictate-side fix must expose real capture readiness and honor stops while the
recorder start coroutine is in flight. It cannot be guaranteed client-side
from the early `onReadyForSpeech` or periodic `onRmsChanged` (which Dictate also
emits before audio capture).
`onEndOfSpeech` enters processing; a tap during processing cancels. A callback
cannot act after `cancel` or after its recognizer/generation no longer matches.
The IME editor-connection/visibility checks and English-only output guards remain.
The existing explicit, verified Dictate `RecognitionService` component remains
the only provider; no new permission or audio-sharing entry point is used.
The footer reports Starting/Listening/Waiting/Transcribing in its existing
center slot without resizing the keyboard. Errors remain reason-coded.

Unit tests in `InlineVoiceSessionStateTest` cover normal speak/stop, a stop
queued during delayed startup with exactly one command, no re-entry from late
speech callbacks, processing cancellation, and reset. Existing
`DictateServiceSelectionTest` still covers Dictate-only resolution and truthful
provider-permission error attribution.

## Pending integration / acceptance gates

1. Full Android JVM/Robolectric and debug APK CI — **pending until CI runs**.
   Local Gradle wrapper cannot fetch Gradle 8.14.4 on the restricted network.
2. Pinned original-signature Obtainium release — **pending**. A successful build
   alone does not establish that voice captures on the Oppo.
3. Physical Oppo: short tap, speak, second tap, wait for Dictate cloud or local
   provider, text inserted once, keyboard stays unchanged. Repeat slow cold
   start, rapid second tap, silence, cancel while processing, app/editor switch,
   no mic permission, and English-only active language.
4. If `ERROR_INSUFFICIENT_PERMISSIONS` survives, correlate Android's
   `RecognitionService` framework log with both apps' microphone grants/AppOps
   and the exact installed Dictate version. Do not log voice text, keys, URLs,
   editor values or raw audio. Only then assign failure to Android/Oppo vs Dictate.
5. A Dictate-side fix, if needed, should be an isolated upstream contribution:
   attributed microphone context and recorder builder, real capture-ready
   signal, pending-stop handling, one terminal outcome, cancellation, and
   integration tests. Do not fork/bundle Dictate wholesale or change its keys,
   user settings, or signing identity.

**This is a source/architecture checkpoint, not phone-verified successful dictation.**

## ColorOS 15 source audit and second candidate — 2026-11-18

The first brrBoard source rebuild was published as
[signed Obtainium v1.0.126](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.126)
from main `8e3289c`; both [debug CI](https://github.com/Batmancpu/brrrBoard/actions/runs/44339584441)
and [original-signer CI](https://github.com/Batmancpu/brrrBoard/actions/runs/44339584403)
passed including unit tests. This does not mean recording worked on the Oppo.

The latest *published* Dictate as of this audit is
[v6.4.0](https://github.com/DevEmperor/DictateKeyboard/releases/tag/v6.4.0)
(2026-10-07), while Dictate's actively developed main is later (2026-11-18).
The v6.4.0 `RecognitionSession` file was checked independently: it has the same
speech-threshold callback, early-stop race and no-speech watchdog as main.
Do not confuse a main-branch behavior with a feature guaranteed on an owner's
installed older Dictate; record its installed version during device tests.

**ColorOS 15 considerations, separated by evidence:**

1. OPPO documents `Settings → Security & privacy → Privacy controls →
   Permission manager → Microphone` for ColorOS 14–16. Check *both* brrBoard
   and Dictate; one grant does not imply the other. OPPO's official permission
   guide: https://support.oppo.com/ph/answer/?aid=neu139
2. Android exposes a global Mic access toggle on Android 12+ (including the
   target phone OS). A disabled global toggle is distinct from both runtime
   package grants. See https://developer.android.com/training/permissions/explaining-access
3. Android 15 targets restrict requesting audio focus while an app is not on
   top or in a foreground service. Dictate targetSdk 36 currently optionally
   requests focus during `RecognitionService` capture, but ignores a failed
   `requestAudioFocus` return; this is a *plausible ancillary issue*, **not**
   a demonstrated cause of error 9. See
   https://developer.android.com/about/versions/15/behavior-changes-15
4. Dictate currently acquires AudioRecord with its own application context;
   Android's RecognitionService contract recommends caller attribution through
   `callback.getCallingAttributionSource()`, `ContextParams`, and
   `AudioRecord.Builder.setContext()`. A physical log is required to determine
   whether this explains the Oppo AppOps/permission check failure.
5. Android's audio-capture priority can return silence when other apps use the
   microphone; phone calls, voice recorders and Bluetooth routing deserve
   controlled A/B tests. Do not globally disable privacy controls, force the
   keyboard into a foreground service, request unrestricted background access
   by default, or install hidden permissions as a workaround.

**ColorOS-tailored second candidate:** On the recognizer's error 9 callback,
brrBoard now asks Android only whether `net.devemperor.dictate` has a runtime
mic grant. If explicitly denied, it shows a targeted Dictate permission hint;
if granted or query unavailable, it continues to report a recognizer/provider
audio failure, because runtime grant alone cannot certify AppOps, in-use
eligibility or a successful recorder. Logs contain only the numeric error and
two boolean/null grant flags — no user utterance, text or provider credentials.
JUnit cases verify the distinction and the existing brrBoard-denied path.

**Owner phone acceptance:** Install the new signed APK; verify installed
Dictate version; enable global Mic access and grant brrBoard *and* Dictate
microphone permissions in ColorOS; confirm Dictate's own keyboard can record;
then with brrBoard selected tap mic, speak a short English sentence, tap stop,
and observe one insertion without an app/IME switch. Repeat cold-start,
rapid second tap, silent/noisy input, device lock/unlock, Bluetooth connected
vs disconnected and permission-denied cases. If error 9 remains while both
grants are allowed, capture the exact error and a *redacted* logcat showing
RecognitionService/AppOps/AudioRecord for a single attempt; no audio/transcript
or keys. Do not assume a battery-exemption workaround without an A/B result.
