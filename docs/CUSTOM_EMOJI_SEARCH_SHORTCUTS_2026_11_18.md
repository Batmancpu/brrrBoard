# Custom Emoji Search shortcuts — small follow-up after Describe v1.0.113

**Release verification (2026-11-18):** Source `0f62298fd8e0397457f50ea554460314fbae7eec` passed complete [debug/unit #208](https://github.com/Batmancpu/brrrBoard/actions/runs/44246920005) and [original-signer #114](https://github.com/Batmancpu/brrrBoard/actions/runs/44246919873), both terminal SUCCESS. Published [signed brrBoard.apk v1.0.114](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.114/brrBoard.apk), 38,113,740 bytes, SHA-256 `f5d609237e1f6435f0fdff434c9684473ea996f3971d7656dc542168145c1d08`. User has not yet run the Oppo acceptance test; the prior source-candidate notes below reflect pre-CI state.

**Baseline and release order:** Describe v1.0.113 source `2f279e6fe2426ea25f89a11534dc904819183b56`; debug #207 and original-signed #113 both completed SUCCESS and signed [v1.0.113](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.113/brrBoard.apk) is published. Owner cannot test immediately and explicitly requested the next small separate update close behind. New import code is a source candidate; **independent CI, signing and Oppo checks pending**.

## Exact user-facing scope and limits

The app already has an Appearance → **Set custom emoji font from file** picker, which changes a glyph font. This new feature is different: Appearance → **Import emoji search shortcuts** lets users optionally define text phrases that should retrieve a supported Unicode emoji in **Describe an Emoji**. It does *not* import images, stickers, TTF files, emoji artwork, fonts, code or arbitrary Unicode sequences. Font import, third-party font licensing and normal rendering are unchanged.

Use Android Files/SAF to choose a UTF-8 `.tsv` file containing **one description, a literal tab, and one emoji** per line. Lines beginning `# ` and blank lines are skipped. [Download/copy the sample](example_emoji_search_shortcuts.tsv):

```
sparkly heart<TAB>💖
happy tears<TAB>😂
puppy face<TAB>🐶
```

Here `<TAB>` means the actual tab key/character in the file. Up to 128 unique normalized phrases and 32 KiB total. Every Emoji must already be an exact sequence in the bundled CLDR corpus; no arbitrary ZWJ combinations, no emoji image payload, no predictive text injection. Imported aliases only apply when the full normalized Emoji Search description equals the mapping, where the user's selection ranks first and remaining built-in results follow. Ordinary words, gesture typing, context Smart picks and app editor text never consult these shortcuts.

## Implementation details

- `EmojiDescribeCustomAliases.java`: strict UTF-8 decoder (malformed input rejected), bounded stream copy before decoding, tab-column/schema count and duplicate checks, alias token/codepoint length check, real CLDR sequence membership check. Entire file is parsed/validated **before** any replacement. `AtomicFile` persists a normalized app-private TSV (device-protected files, compatible with existing backup) and can delete only that one explicitly selected file. On corrupt restored file, fail closed and use normal built-in search.
- `EmojiDescribeEngine.java`: monotonic custom-mapping revision after import or clear; refresh from local file **once on the background Describe worker at next explicit Search**. Concurrent import cannot silently clear a pending refresh. Exact custom phrase is deduplicated/promoted ahead of CLDR hits, capped to 24, while stale callbacks and all normal search latency guards remain. No per-keystroke file access.
- `EmojiDescribeIndex.java`: `containsEmoji` read-only membership query for load validation.
- `CustomEmojiSearchAliasesPreference.kt`: Compose Appearance setting, SAF `OpenDocument`, background I/O, visible success/failure feedback, replace/remove actions. No broad storage permission, network, file path logging or other app data access.
- `AppearanceScreen.kt`, `SettingsContainer.kt`, `strings.xml`: discoverable setting and clear explanation. `BackupRestorePreference.kt` adds the *exact* new private filename to the existing backup allowlist; import validation also applies when backup-restored.
- `EmojiDescribeCustomAliasesTest`: valid, dedup, UTF-8, malformed columns, unsupported emoji, non-word, 32 KiB, 128 rule cap and immutable-result tests.

## Verification and rollout

1. Run complete unskipped debug Android assemble/unit test suite and independent original signer; verify published signed APK package `com.mangoloads.board`, original certificate, monotonic version and release target SHA.
2. In Appearance, import the sample file, return to Emoji Search and search `sparkly heart` → 💖 first. Search built-in `red heart` to confirm Unicode relevance unaffected. Replace mapping with a different valid file; verify only the new aliases remain. Try invalid file, confirm warning and previous shortcuts preserved.
3. Use Remove to restore original Search. Test on Oppo dark/light, keyboard restart, backup restore and quick typing. An actual ColorOS Files provider, performance, accessibility and real keyboard interactions cannot be certified by CI. No physical acceptance claimed.

The other requested **custom Emoji artwork/pack** feature remains out of scope until a compatible format, insertion behavior and rights model are specified. Premium icon overhaul and top-glint sensor motion remain separate future batches, not buried in this small import feature.
