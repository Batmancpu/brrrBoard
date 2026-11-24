# Continuous offline dictation repair — 2026-11-24

Owner report: tapping the microphone transcribes only the first roughly 5–7 seconds and stops. The owner needs one- to two-minute dictation with successive batches inserted while the mic remains active.

Source diagnosis on main at bbd14045754e759c521bb5dff1dfe3253bce12f5: PrivateVoiceChunker emits bounded 5–6 s chunks; KeyboardPcmAudioSource has a 170 s capture limit, not a 6 s limit. OfflineBoardDictation bypassed RichInputConnection for live drafts. The expected selection/composing cache was stale, so InputLogic could interpret the resulting callback as a manual cursor move and finish the span. Subsequent partials then failed the captured-destination guard and cancelled capture. Source repair routes interim and discard edits through the rich connection, clears unrelated typed composition before first batch, guards session generation and handles decode failure even during capture. No model, signing, package, hold-to-talk or incognito changes.

Added regression coverage for consecutive composing updates and 120 s synthetic PCM. Device acceptance: tap once, speak for 120 s across pauses; mic must remain active while text accumulates, tap Stop to drain final batch without duplicate text; repeat hold/release, cancel, cursor move and Incognito. Test in ChatGPT and a native text field on Reno10. Source candidate only until exact-commit debug CI and original-signed release succeed. Do not mark device-verified without owner confirmation.

## Release gate recovery

The initial candidate at `25b803b` failed both hosted workflows before Kotlin compilation because the notes ledger exceeded its 30-change limit (31 entries). Replace the cumulative entries with just this update's two concrete changes; prior signed release history remains intact. Also defer live delivery when a destination editor rejects composing text so capture can continue until Stop. Keep the full debug/unit and original-signer gates; no bypass. A fresh main push is required to build the corrected candidate.
