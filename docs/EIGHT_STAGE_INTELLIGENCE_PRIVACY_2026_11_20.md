# Eight-stage brrBoard intelligence/privacy ledger — 20 November 2026

## CI repair after first main source push

- Exact initial main SHA `01996e4df862567c5edf3af45afaaa21e4b0b162`: [debug 44672188373](https://github.com/Batmancpu/brrrBoard/actions/runs/44672188373) and [Obtainium 44672188264](https://github.com/Batmancpu/brrrBoard/actions/runs/44672188264) both compiled/built but stopped on **one failing test out of 413**. The test tried to mutate an `NgramContext` using Android `TextUtils.equals` in the plain-JVM runner (`RuntimeException`, `NextWordCacheKeyTest.kt:24`). The repair uses a mutable `StringBuilder` to exercise the immutable snapshot without Android framework methods. No production next-word ranking or cache feature is removed.
- Nearby clipboard blind spots repaired in the same main-only follow-up: no passive primary-clipboard receipt fetch in a private/no-editor session; immediate dismissal of stale private clipboard suggestion state; inaccessible clip reads become a no-op; confirmation compares the actual text and identity from **one** final `ClipData` snapshot; system-clipboard cleanup refuses any copy without a reliable OS timestamp. Older Android with no timestamp can still use explicit confirmed Paste with a text-digest comparison, never conditional system clipboard removal.
- New exact-SHA **both-workflow success** and original signer/asset verification are mandatory for this corrective source, independent of Oppo acceptance. Initial red workflows are historical diagnostics, not a release. No mic/incognito theme changes, performance claims, stored private logs or extra model.

Source baseline: `49756dcacefceaca875eb9dba4d2099fcc549fd9` on `main`. This is an authorized implementation, not the earlier research-only plan. The native dictionary, existing swipe learner, CLDR Emoji index, on-device Parakeet, application ID, original signer, previously installed private model, and encrypted on-device associations must survive. Do **not** bundle another model, record real text in tests/logs, auto-replace learned words, or treat synthetic runs as real speedups. Gold incognito and deep-red recording mic are separate uncompleted/untested design decisions and are not an intelligence requirement.

## Batch 1 — RAM-only measurements and synthetic baselines

- Fixed-bucket `IntelligenceMetrics` with enum-only names; not durable, exported, uploaded or printed. Only debug builds sample `System.nanoTime()`; no editor/user values can enter the API. Synthetic bin boundary test added. Initial live Oppo p50/p95, input corpus and benchmark numbers **not captured**; existing B16B and B17 synthetic suites are harnesses, not native measurements.

## Batch 2 — Editor/privacy generation

- `EditorSessionGeneration` opaque epoch invalidated on editor start/finish, IME hide and manual incognito change. Delayed UI suggestion/resume and worker/batch preview callbacks check it; unit test models stale refusal. This prevents obvious cross-editor completions but needs rapid-switch, rotation and input-state acceptance on the phone.

## Batch 3 — Complete clipboard entry-point audit and private Paste

| Entry/exit | Current guard or change | Residual device check |
| --- | --- | --- |
| `onPrimaryClipChanged`, `updatePrimaryClip`, `fetchPrimaryClip` → `ClipboardDao` | Ignore private/sensitive clips, fail closed without an editor; on editor start defer clipboard inspection until target-app forced-incognito settings have been loaded | Android 13+ sensitive copy, secure-app re-focus |
| MediaStore/SAF screenshot observer → cache/DAO | Permission + setting + private-editor check, now additionally generation-check before caching and DAO insertion | In-flight editor switch / OEM grant |
| Suggestion chip → text or rich `commitKlipyContent` | Recheck current editor, InputConnection, privacy, clip identity and epoch on click | Clipboard changes while chip visible |
| History panel → `pasteHistoryEntry` | Panel unavailable in private fields; final entry point now rechecks privacy | Rapid incognito toggle while panel animates |
| Direct hardware/toolbar Paste key → `requestPrimaryPaste` | New generic attached confirmation in private/sensitive context, re-read only on positive tap after checking epoch/connection/receipt | Dialog placement, TalkBack, cancellation |
| `RichInputConnection.copyText` | Only explicit brrBoard-origin copy records a process-local, SHA-256/OS-timestamp ownership receipt | Other app changes current clip |
| History Clear | Clears own nonpinned DAO rows; system clipboard cleared **only if** the explicit brrBoard copy receipt still matches; foreign/newer clipboard untouched | Pre-Android-O timestamp uncertainty fails closed |
| Emoji Search, theme copy, AI tool copy (`setPrimaryClip`) | Explicit copy exits remain untouched, not falsely considered brrBoard-owned for cleanup | Separate feature semantics |
| Android/editor's own context-menu paste | Host OS owned, not interceptable by IME; brrBoard only guards its own Paste key | OS-specific behavior |

The confirmation is generic by design: no private text, preview, fingerprint or source app name is displayed or logged. It fails closed if the IME cannot attach a dialog. No new clipboard history or automatic erase is introduced. The fingerprint stays in RAM; no content fingerprint written to preferences or network.

## Remaining batches and release gates

4. `NextWordCacheKey` captures a **copied** n-gram context, locale, layout ID digest, input style and suggestion flags. Private/incognito lookups bypass caching; delayed dictionary timeout is epoch-checked and duplicate messages coalesced. Synthetic tests check cross-scope independence. Real cache hit/miss performance and correctness are still Oppo-pending.
5. Added *test-only* two-row Levenshtein candidate (reduces theoretical row-array creations) and cached-normalized-annotation search candidate. Synthetic cross-product and multi-query fixtures assert exact agreement with `SwipeGeometryRescue.couldMatchSeed` and `EmojiContextIndex.search`, including ties/ZWJ/skin tone/Unicode. Neither candidate is wired into runtime: no live allocation, heap or latency measurements, so shipping either would be premature.
6. Added 13 authored Indian English/general public-exam terms to a separate **valid-only** TSV, bounded frequencies 24–64. Build generator incorporates them into existing brrPersonal and the native cache uses new `brrPersonal_v3` name, leaving old v2 and encrypted learning untouched; `BrrCorrectionSafety` prevents unsolicited replacement with these tokens. Sources/meaning checked against Cambridge Dictionary entries for Indian English *prepone*, *lakh*, *tiffin*, *rickshaw*; UPSC/NCERT are official institution abbreviations. This is spelling support, **not** a demonstrated prediction/accuracy gain. Offline formatter adds exact casing only (UPSC, NCERT, ISRO, ColorOS, OPPO, WhatsApp, YouTube) while protecting URL/email and untouched words; no server or model changes.
7. Existing AES-256-GCM/AndroidKeystore alias and data file unchanged. Serial writer now authenticates and byte-compares a readback of the sealed `AtomicFile` before it deletes a legacy plaintext source. Failures retain original bytes and force RAM-only learning; no write-back plaintext. Added version, nonce, truncation, length, wrong-key and migration-gate synthetic fault checks. This is NOT proof of real Keystore invalidation/reboot/migration on an Oppo; that remains device QA.
8. Added synthetic cross-boundary privacy integration tests: normal vs `NO_PERSONALIZED_LEARNING` editor/epoch, sensitive clipboard requiring confirmation. Local `git diff --check`, XML parse, and existing Python generator (7), synthetic next-word (5), and mic-routing (4) tests passed. There is no local Android SDK/adb/Gradle environment; therefore Kotlin/Java compilation, Robolectric integration, original signing, exact-SHA debug CI, published APK digest and actual Oppo acceptance **remain unverified until GitHub workflows complete**. Device checklist follows.

### Device acceptance — independent of CI

1. Upgrade original-signed APK in-place (without uninstall/data clear). Check existing swipe-correction count and user dictionaries survive; offline Parakeet previously installed in `noBackupFilesDir` still works without download. Check build package and signer unchanged.
2. In normal text, switch US↔UK, other layout, app/editor/rotation and suggestions rapidly. Verify no stale strip/corrections, typed words remain selected/unmodified, Undo remains available and swipe learner only proposes alternatives after two explicit corrections.
3. Open a normal editor and a secure-app-forced field, password field, visible password and `NO_PERSONALIZED_LEARNING` field. No clipboard history/screenshot capture or learning in restricted fields; direct Paste opens a generic confirmation, Cancel inserts nothing, switching editor before confirm inserts nothing.
4. While the private paste dialog is open, replace the OS clipboard: confirm must be a no-op. Clear brrBoard history after copying in another app: current OS clipboard must remain. After brrBoard Copy, Clear may remove only the still-current owned content.
5. Check Emoji CLDR Search variants and ranking unchanged; both native/borrowed swipe and fallback behavior unchanged; English valid-only tokens are not unsolicited corrections; voice casing only and URL/email exact preservation.
6. Reboot/restart and check encrypted swipe association recall, dashboard storage indicator and explicit Erase. For true migration, use a dedicated synthetic seeded v1 fixture on a controlled QA installation; never stage raw personal corrections or reset real data just for testing. Keystore-unavailable and corrupted v2 fixtures must remain preserved, RAM-only.
7. Visually verify *separate* premium-gold incognito and premium deep-red recording mic treatments when their own design batch is actually shipped; intelligence work has changed neither. Check the newer trackpad/footer animations inherited from base main.

### Honest status at local source checkpoint

| Gate | Status |
| --- | --- |
| Baseline Git main | `49756dcacefceaca875eb9dba4d2099fcc549fd9` before edits |
| Scope | Eight batches source/tests/documentation prepared; no new model, permission, signing file, microphone asset or incognito UI |
| Local diff/XML/Python suites | PASS (16 pure Python cases + XML parse + `git diff --check`) |
| Android full test + debug CI | PENDING — requires exact pushed SHA |
| Original-pinned-signer Obtainium release | PENDING — requires exact pushed SHA and green test gate |
| APK hash/source/cert | PENDING — must be read from actual release asset and CI |
| Physical Oppo Reno10 | PENDING — owner test, not simulated by CI |
