# Contextual footer micro-actions (first guarded implementation)

## Owner's design specification

Use only the existing blank horizontal space **between** fixed Emoji/ABC and microphone icons. Show no permanent controls or second toolbar. One small, iOS-like, short-lived action chip may appear in a valid context: undo a clipboard swipe deletion, undo/redo the keyboard's own verified text edits, retry a recoverable Dictate failure. Keep the existing glass moving behind the controls and preserve immediate typing/touch latency. Audio provider remains offline/selected as before.

## 2026-11-19 first code candidate

- `main_keyboard_frame.xml` adds a normally GONE 36dp center action TextView within the **existing footer height**; Emoji and mic keys retain their layout and positions.
- `BoardFooterView.showContextAction(owner, message, action, duration, callback)` installs exactly one action with 2–10s timeout (6s default), accessible label, theme-aware rounded chip, haptic tap feedback and explicit owner-identity revocation; view never displays clipboard payload text. Footer reset clears stale callbacks, and a later action replaces the previous one.
- `ClipboardHistoryView.onClipDismissed` retains its existing `ClipboardHistoryManager.restoreEntry` semantics; when the footer exists and is visible, the legacy floating overlay is hidden and `Removed · Undo` appears in the footer. It expires after 6s; a new deletion replaces the old target; leaving the panel cancels ownership and clears the temporary entry. Retain old overlay fallback only when footer absent.
- Undo expiry also clears the dismissed-entry reference when the legacy overlay is absent; this avoids retaining Clipboard data solely because the modern footer path has no floating bar.
- `InlineDictation` shows `Voice failed · Retry` after transient recognizer failures, no-speech and reported provider audio errors. Retry re-enters the **same** guarded start/toggle path with the configured Dictate component and live editor checks. No retry for missing brrBoard permission, missing Dictate service, missing Dictate permission, language/model misconfiguration; existing short error hint/Toast remains. Recognition provider, Intent extras, permissions, package and signing unchanged.
- `InlineVoiceSessionStateTest` covers the recoverable/nonrecoverable decision. No raw editor text or audio is stored in footer state.

## Important limitations / next passes

- This first commit **does not** yet claim safe paste/text-delete Undo/Redo. The existing `KeyCode.UNDO/REDO` bridge sends Ctrl+Z / Ctrl+Shift+Z to the host; blindly showing it after every deletion risks undoing an unrelated host edit or losing IME composition. That feature requires verified edit ownership, cursor/selection guard, cancellation when any other action intervenes, and held-out latency tests. Keep it pending rather than wiring an unsafe generic Ctrl+Z chip.
- The clipboard restore remains a 6s in-session opportunity, not a persistent recycle bin. No undo should survive IME panel exit, app/editor change, screen-off, or an unrelated Clipboard replacement.
- Android Kotlin and Robolectric build/test plus original-signed release gate must finish before publication; none of these source changes imply Oppo UI acceptance. Device acceptance must cover footer size, dark/light themes, one-handed/layout width, touch exploration, rapid delete/retry, typing while chip expires, focus switching, and real Dictate provider outcomes. Owner's pending video will decide residual strip/glass bugs separately.
