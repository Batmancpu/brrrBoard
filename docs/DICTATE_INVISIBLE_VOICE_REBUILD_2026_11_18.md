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
