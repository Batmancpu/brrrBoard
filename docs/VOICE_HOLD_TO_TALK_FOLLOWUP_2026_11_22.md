# Private hold-to-talk follow-up — 2026-11-22

## Implemented source candidate

- Long-pressing the footer microphone starts the existing local streaming path; releasing stops capture. The preference is on by default and can be turned off in **Everyday tools → Offline voice**.
- Outside Incognito, completed streaming batches update one guarded composing span in the editor while recording continues. Incognito does not receive partial text; it receives the final phrase only after capture stops. A stale/detached editor drops the result and clears the composing span where it is still safe to do so.
- Hold-to-talk auto-send is on by default behind its own setting. It sends only when the editor advertises `IME_ACTION_SEND`, only after a valid final transcript and only for a hold gesture outside Incognito. Other editor actions insert text without triggering a send.
- Moving from the held mic onto the visible Cancel target arms discard, shows the existing mic-to-bin animation, and gives the configured important-action haptic. Releasing on Cancel ends and wipes the session; releasing elsewhere ends and transcribes. No spoken or accessibility announcement is emitted for this interaction; the transient status label is not exposed to accessibility services.
- Cancel, failure, editor loss, keyboard dismissal, and privacy transitions do not save recovery transcripts. The old transcript recovery file is deleted on repository initialization and the recovery repository now returns no saved data.
- Chunking keeps the 16 kHz PCM path bounded to six-second maximum windows with the existing 320 ms overlap. A lightweight local amplitude gate may close a window after at least three seconds of speech followed by 600 ms of low-energy audio. This is a pause heuristic, not a bundled neural VAD model, and does not change ASR language support.
- Voice content exists only in bounded session memory and the editor composing span while actively dictating. It is cleared on discard/failure and is not added to clipboard or learning history. The final dictated phrase remains in the destination app because that is the requested insertion behavior.

## Build, release, and device evidence

Exact source `2f2e7424883fab5f597e4f2c3889ec6bfb00ee29` passed debug build and all 516 Android unit tests in feature run [45057840833](https://github.com/Batmancpu/brrrBoard/actions/runs/45057840833) and main run [45058407501](https://github.com/Batmancpu/brrrBoard/actions/runs/45058407501). The original-signer release workflow [45058407505](https://github.com/Batmancpu/brrrBoard/actions/runs/45058407505) passed the same-SHA gate and published [v1.0.251](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.251), targeting that exact source. The installable `brrrBoard.apk` is 63,511,404 bytes, SHA-256 `9f0f4f609beadfede67a5e11b885d7cdc12cbb4e36e0b84f863fa55892f5bf17`. Release packaging kept both optional Parakeet weights out and retained the verified private model already installed on the device. CI checked the original signer, package identity, version, offline permissions and model packaging. The workspace could not download Gradle 8.14.4; hosted CI was used. Physical hold/release, cancel, DND haptic preference, Incognito, editor-action and TalkBack behavior still require device acceptance.

## Behavior limits

The amplitude gate is intentionally simple and has no added model or package size. It may treat sustained background noise as speech; the hard six-second batch limit remains the fallback. DND haptics honor the existing **Important haptics in Do Not Disturb** preference and device capabilities. The app does not override Android's global DND or haptics settings.
