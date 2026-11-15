# My brrBoard

brrBoard is my offline-first Android keyboard, shaped around how I type and tuned for my OPPO Reno10 5G. I want it to feel as clean and familiar as an iPhone keyboard while behaving properly as an Android IME.

## What it does

- English (US/UK) and Romanized Hindi/Hinglish typing
- Local Emoji and clipboard panels
- Frosted Glass appearance
- Typing dictionaries and suggestions stay on-device; brrBoard itself has no network permission
- Mic tap uses the selected Android voice-recognition service; my configured DictateKeyboard provider handles transcription and may use its own network service

## Current build

The latest signed release is [v1.0.31](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.31).

[Download the APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.31/brrBoard.apk) · [Set up Obtainium](docs/OBTAINIUM.md) · [Privacy policy](PRIVACY_POLICY.md)

The system-following OG dark/light appearance and optional Apple-palette action-key tint shipped in v1.0.23. Shift states and safe saved-icon migration shipped in v1.0.25. v1.0.31 includes the brrPersonal offline dictionary foundation. brrBoard uses the selected Dictate RecognitionService, rejects Hindi-script-only final output, and explains how to set Dictate's Active language to English and turn off Follow keyboard language. Dictate controls its own language; brrBoard cannot force English or reliably detect Romanized Hindi. Reno10 checks, Smart Emoji acceptance, contextual Emoji suggestions, proofreading scope, and swipe-library review remain open.

## Project notes

- [Current project checkpoint](PROJECT_CONTEXT.md)
- [Post-v1.0.19 Reno10 plan](docs/POST_V1_0_19_RENO10_PLAN.md)
- [Conversation and handoff log](docs/PROJECT_CONVERSATION.md)

## Source and licenses

This project is derived from [FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), HeliBoard, OpenBoard, and AOSP LatinIME. See [LICENSE](LICENSE), [Apache notices](LICENSE-Apache-2.0), and [asset license](LICENSE-CC-BY-SA-4.0). The retained upstream icon is credited to Orion under CC BY-SA 4.0.
