# My brrBoard

brrBoard is my offline-first Android keyboard, shaped around how I type and tuned for my OPPO Reno10 5G. I want it to feel as clean and familiar as an iPhone keyboard while behaving properly as an Android IME.

## What it does

- English (US/UK) and Romanized Hindi/Hinglish typing
- Local Emoji and clipboard panels
- Frosted Glass appearance
- Typing dictionaries and suggestions stay on-device; brrBoard itself has no network permission
- Mic tap uses the selected Android voice-recognition service; my configured DictateKeyboard provider handles transcription and may use its own network service

## Current build

The latest signed release is [v1.0.22](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.22).

[Download the APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.22/brrBoard.apk) · [Set up Obtainium](docs/OBTAINIUM.md) · [Privacy policy](PRIVACY_POLICY.md)

The next Reno10-focused work is in progress: consistent panel sizing, the system dictation route, iOS-inspired key behavior and icons, a compact brrPersonal Hinglish dictionary, and locally ranked Emoji suggestions. The action-key tint and neutral OG surfaces are now in the next signed build workflow; Reno10 visuals, Dictate's English language setting, and the dictionary/Emoji/symbol work still need acceptance.

## Project notes

- [Current project checkpoint](PROJECT_CONTEXT.md)
- [Post-v1.0.19 Reno10 plan](docs/POST_V1_0_19_RENO10_PLAN.md)
- [Conversation and handoff log](docs/PROJECT_CONVERSATION.md)

## Source and licenses

This project is derived from [FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), HeliBoard, OpenBoard, and AOSP LatinIME. See [LICENSE](LICENSE), [Apache notices](LICENSE-Apache-2.0), and [asset license](LICENSE-CC-BY-SA-4.0). The retained upstream icon is credited to Orion under CC BY-SA 4.0.
