# Private hold-to-talk follow-up — 2026-11-22

## Implemented source candidate

- Long-pressing the footer microphone starts the existing local streaming path; releasing stops capture. The preference is on by default and can be turned off in **Everyday tools → Offline voice**.
- Outside Incognito, completed streaming batches update one guarded composing span in the editor while recording continues. Incognito does not receive partial text; it receives the final phrase only after capture stops. A stale/detached editor drops the result and clears the composing span where it is still safe to do so.
- Hold-to-talk auto-send is on by default behind its own setting. It sends only when the editor advertises `IME_ACTION_SEND`, only after a valid final transcript and only for a hold gesture outside Incognito. Other editor actions insert text without triggering a send.
- Moving from the held mic onto the visible Cancel target arms discard, shows the existing mic-to-bin animation, and gives the configured important-action haptic. Releasing on Cancel ends and wipes the session; releasing elsewhere ends and transcribes. No spoken or accessibility announcement is emitted for this interaction; the transient status label is not exposed to accessibility services.
- Cancel, failure, editor loss, keyboard dismissal, and privacy transitions do not save recovery transcripts. The old transcript recovery file is deleted on repository initialization and the recovery repository now returns no saved data.
- Chunking keeps the 16 kHz PCM path bounded to six-second maximum windows with the existing 320 ms overlap. A lightweight local amplitude gate may close a window after at least three seconds of speech followed by 600 ms of low-energy audio. This is a pause heuristic, not a bundled neural VAD model, and does not change ASR language support.
- Voice content exists only in bounded session memory and the editor composing span while actively dictating. It is cleared on discard/failure and is not added to clipboard or learning history. The final dictated phrase remains in the destination app because that is the requested insertion behavior.

## Checks and release gate

`git diff --check` is clean. The workspace has no installed Gradle executable or cached Gradle 8.14.4 distribution; the wrapper's previous attempt failed because outbound network/proxy access is refused. Run `brrrBoard — Build Debug APK` against the exact feature-branch commit to compile and run the unit suite. Do not call the candidate built or released until the exact SHA passes CI and the original-signed Obtainium workflow publishes it. Physical hold/release, cancel, DND haptic preference, Incognito, editor-action and TalkBack behavior still require device acceptance.

## Behavior limits

The amplitude gate is intentionally simple and has no added model or package size. It may treat sustained background noise as speech; the hard six-second batch limit remains the fallback. DND haptics honor the existing **Important haptics in Do Not Disturb** preference and device capabilities. The app does not override Android's global DND or haptics settings.
