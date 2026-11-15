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
| Signed layout APK | **Built, not fully device-accepted** | [v1.0.13](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.13) completed signer/package validation; the owner's latest recording shows normal search-entry and layout states but not the full interaction matrix. |
| Unit test gate | **14/180 failures** | [Debug run 43679255037](https://github.com/Batmancpu/brrrBoard/actions/runs/43679255037) assembled, signed and uploaded the APK, then failed its test gate. Earlier pre-layout tests also had 14 failures; compare test names, not only the total. |
| Globe hold arc | **Owner-reported working** | The owner confirms all three release targets (Emoji, Clipboard, Settings) open correctly. Retest after other layout changes; accessible shortcuts still need TalkBack checks. |
| Emoji/Clipboard height | **Owner-reported regression; source fix pending** | With normal keyboard footer formerly hidden in panels, Emoji/Clipboard appeared shorter. Source now retains the footer and compensates for the missing 36dp strip. Confirm matching outer height in both portrait and landscape on Reno10. |
| Footer/toolbar appearance | **Owner-reported regression; source fix pending** | Excess bottom gap, undersized globe/mic, 50dp empty strip and prominent A-button circle. Source reduces strip/footer to 36dp/38dp, enlarges footer icons, removes A fill. Check dimensions against all six reference images and edge touches. |
| Toolbar swipe | **Owner-reported jerky; source fix pending** | In the recording a partial downward move resets. Source adds less restrictive axis capture, fast-flick dismissal and gentle overshoot on spring return. Check slow/fast gestures and conflicts with predictions/popup gestures before claiming fixed. |
| Offline dictation and privacy | **Owner-reported failure** | Mic still fails on the Reno10. Source checks compatible installed offline locales and for Romanized Hinglish may choose installed English if Hindi absent. Device may lack a supported on-device speech provider/model; test both languages, Stop/Cancel, airplane mode and stale-focus blocking. Android's 0 B app-usage display alone proves little. |

## Evidence standard

1. **Build evidence:** CI passes tests and produces a non-empty APK.
2. **Install evidence:** APK installs successfully.
3. **Update evidence:** second APK updates the previous install without uninstalling.
4. **Runtime evidence:** affected feature is exercised on the target device.
5. **Regression evidence:** existing related tests still pass.

Do not mark an issue as fixed solely because the source compiles.
