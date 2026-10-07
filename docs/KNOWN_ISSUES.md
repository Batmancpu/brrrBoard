# brrrBoard Known Issues & Audit Notes

This is the current engineering watchlist. Items are not called fixed until there is evidence from tests or a real device.

| Area | Status | Finding |
|---|---|---|
| APK CI | **Partially recovered** | Canonical build workflow restored; independent audit APK workflow now builds, signs and uploads successfully. GitHub Release API publication is blocked by HTTP 403. |
| Debug APK updates | **Needs device validation** | Fallback `versionCode` now uses seconds rather than minutes; APK signing identity is persistent across debug builds. Device update without uninstall remains to be tested. |
| Runtime keyboard startup | **Needs device validation** | Static source inspection cannot prove IME startup/keyboard-popup behavior on a physical OEM build. |
| Search panels | **High regression sensitivity** | Emoji/Klipy search lifecycle and keyboard switching were recently rewritten; test enter/search/type/switch/exit repeatedly. |
| Frosted Glass | **High OEM sensitivity** | Native blur has Android/Samsung-specific fallback paths; test with blur on/off and Android 12+ plus older API behavior where supported. |
| Clipboard history | **Needs device validation** | Undo/restore and suggestion-dismissal logic was recently changed; test delete → undo, delete → paste, restart, and pinned entries. |
| Settings | **Recently fixed** | Debug settings nested-scroll crash was fixed; regression test the Debug screen and search filtering. |
| Klipy | **Recently fixed** | GIF format filter was corrected; test GIF and sticker search independently. |

## Evidence standard

1. **Build evidence:** CI passes tests and produces a non-empty APK.
2. **Install evidence:** APK installs successfully.
3. **Update evidence:** second APK updates the previous install without uninstalling.
4. **Runtime evidence:** affected feature is exercised on the target device.
5. **Regression evidence:** existing related tests still pass.

Do not mark an issue as fixed solely because the source compiles.