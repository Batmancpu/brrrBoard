# Product plan implementation status — 2026-11-23

> **Current release pointer:** v1.0.257 is now the latest confirmed public APK; see the root README for link, size, and SHA-256. The v1.0.255 boundary below is historical. v1.0.257 release notes report a protected-access fix, model discovery fix, and Toolbar cleanup. The owner still reports biometric failure on-device and has put biometrics on hold; that physical issue is not accepted as solved. Current next-AI requirements: [`NEXT_AGENT_HANDOFF_2026_11_23.md`](NEXT_AGENT_HANDOFF_2026_11_23.md).

## Download and release boundary

The latest public production APK is original-signed [brrBoard v1.0.255](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.255). Install or update from the [release APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.255/brrBoard.apk); it is 63,544,172 bytes and has SHA-256 `8a64fcf8b8f8535c9209751b3c212a79758099262d63f0a1b671cbed40773f1a`. The root README carries the full direct-download, update, fresh-install, Android keyboard setup, and checksum steps. In-place update preserves app-private installed voice weights; uninstalling removes them. This release does not bundle either large Parakeet model.

The product-plan changes described below are in v1.0.255. The exact-source Android build and full unit suite passed, followed by the original-signer release workflow. Do not install a debug Actions artifact over the production APK.

## Implemented product areas

- The released app has the compact Emoji geometry fix, contextual Smart Emoji flow, progressive Clipboard glass, horizontally browsable toolbar, grouped settings with Advanced controls, default Parakeet Ultra selection when installed, optional brrClean, hold-to-talk preview/cancel, Incognito final-only voice delivery, and a biometric protected route.
- The protected route uses strong biometrics only; the private note is AES-GCM encrypted with an authentication-per-use Android Keystore key in no-backup storage. Device PIN/password is not accepted as fallback.
- v1.0.255 centralizes suggestion eligibility and learning policy for contextual Emoji, confirmed swipe learning, and manual ContextLM reranking. Native candidate generation, typing immediacy, and autocorrection remain authoritative. ContextLM remains off by default because its fixed quality gate failed.
- v1.0.255 makes Android credential-encrypted storage the app default. After first unlock it migrates legacy native UserHistoryDictionary files and the clipboard database out of device-protected storage; file migrations are bounded, atomic and SHA-256 verified, while SQLite uses Android's supported `moveDatabaseFrom` API. Existing settings, voice-model packs, and imported gesture libraries remain in their original device-protected locations so Direct Boot behavior and large-model installs are preserved.
- Existing encrypted Emoji, swipe-learning and biometric-note records migrate from the old device-protected no-backup directory only after first unlock. A conflicting or unverifiable copy fails closed and retains the source. Older transcript-recovery files are purged from both storage areas; the app has no transcript write path.
- Arming the one-use private footer-circle secret requires fresh strong-biometric confirmation. It stays only in volatile memory for up to 30 seconds and is consumed once.

## Security and privacy boundaries

Android credential-protected storage is encrypted by Android and becomes available after the user first unlocks the device. The native history dictionary and clipboard database remain in their native formats and are **not app-layer AES-GCM encrypted**; do not describe them as such. The small Emoji/swipe learning records use app-layer AES-GCM. The private note additionally requires strong-biometric authentication per Keystore operation. Backups are disabled, the Parakeet packs remain in explicit private app storage, and voice transcripts have no retained write path.

The private footer gesture is a convenience input method, not an app lock or a replacement for the Android lock screen. It is disabled by default, must be explicitly enabled, freshly fingerprint-authorized each time it is armed, is limited to a dedicated keyboard footer area, and expires after one use or 30 seconds.

## Evidence and remaining acceptance

Research is based on Android's primary guidance for [Direct Boot and credential-encrypted storage](https://developer.android.com/privacy-and-security/direct-boot), [biometric authentication with CryptoObject](https://developer.android.com/identity/sign-in/biometric-auth), and [Android Keystore](https://developer.android.com/privacy-and-security/keystore). The migration and policy tests passed on the exact release source, followed by the full Android CI and original-signer release pipeline.

Remaining owner-device acceptance includes Oppo fingerprint prompt behavior, keyboard footer-circle gesture reliability, toolbar appearance, and real typing/emoji/voice latency under load. ContextLM does not qualify as a production ranker until a fresh fixed quality evaluation passes. Exact iOS-internal animation timings are not publicly exposed, so motion matching remains an independent Android approximation.

## Plan coverage at this release boundary

| Product area | State |
| --- | --- |
| Emoji panel geometry, contextual ranking, toolbar layout, progressive Clipboard material | In v1.0.255; toolbar appearance still needs the owner's visual check |
| Settings grouping, Advanced controls, reset/Undo, history limit, DND important haptics | In v1.0.255 |
| Offline voice model choice, brrClean toggle, hold-to-talk live preview, cancel gesture, send-field auto-send | In v1.0.255; owner-device voice/cancel check remains |
| Smart Emoji, native suggestions, user-history learning, confirmed swipe learning | Existing local producers remain authoritative; v1.0.255 centralizes policy without adding a per-key model wait |
| ContextLM quality | Experimental only and off by default after the fixed gate failed |
| Private note and protected Brain counters | Strong-biometric protected route; one-use footer secret stays default-off in v1.0.255 |
| Storage security | v1.0.255 moves native history and clipboard to CE after first unlock, keeps app-encrypted records and protected note encrypted, preserves models/settings in DP |
| Final signed APK | Published: original-signed v1.0.255 at the direct APK link above; no large Parakeet weights included |
