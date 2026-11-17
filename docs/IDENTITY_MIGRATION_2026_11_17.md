# brrBoard identity migration — 17 November 2026

**Production name:** brrBoard  
**Attribution:** By Mangoloads  
**Android application ID:** `com.mangoloads.board`  
**Repository:** https://github.com/Batmancpu/brrrBoard  
**Support:** star the GitHub repository; issues: https://github.com/Batmancpu/brrrBoard/issues  
**Website:** https://batmancpu.github.io/mangoloads/

## User-authorized cutover

The owner confirmed the local backup exists in Downloads and explicitly accepts needing to reinstall libraries. The Android OS treats the renamed ID as an unrelated app; it cannot inherit the previous app's private preferences, learned personal dictionary, swipe data or clipboard automatically. Do not silently uninstall former brrBoard. Installation: old version backup ZIP -> new original-signed APK install -> Settings/IME enable new brrBoard -> import backed-up ZIP -> validate personal dictionary, swipe learning, themes, core tools and preferences -> retain old version until confident. Saved ZIP location and accuracy must be confirmed by the phone owner, not CI.

Release CI package-name assertion must target `com.mangoloads.board`, and the original release signing secrets must not change. Debug package remains `com.mangoloads.board.debug`. Internal Kotlin namespace `helium314.keyboard.latin` and mandatory upstream GPL/license notices remain intentionally untouched for compatibility/legal attribution. User-facing legacy brand and vendor donation links are removed wherever safely identified.

## Still not complete

- Full original icon suite, validated against official Apple HIG guidelines without distributing Apple-owned artwork.
- Real internal dual-material composition: host-glass root and independent live scrolling-content glass, no repeated blurred host backdrop or static side slabs.
- Clipboard local substring search and newest-first, without host text leakage.
- Oppo physical acceptance and backup restoration validation.
