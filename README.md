# My brrBoard

brrBoard is my personal Android keyboard for the OPPO Reno10 5G. I want the clean, compact feel of an iPhone keyboard with Android's own input-method behavior and useful English/Hinglish typing.

## What I use

- English (US/UK) and Romanized Hindi/Hinglish keyboards
- brrPersonal: a finite offline dictionary with personal spelling, slang and Emoji shortcuts
- Frosted Glass visuals with system light/dark appearance and optional Apple-inspired action-key tint
- Emoji and clipboard panels with local Emoji recency
- Local word learning and a confirmation-protected way to clear learned typing history
- Dictation through Android's selected RecognitionService. My Dictate app handles transcription using its own language setting and may use its configured network provider.

## Get the latest signed build

[Download the latest signed APK](https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrrBoard.apk) · [Release notes](https://github.com/Batmancpu/brrrBoard/releases/latest) · [Set up Obtainium](docs/OBTAINIUM.md)

For English-only Dictate transcription, set **Dictate → Languages → Active language → English** and turn off **Follow keyboard language**. The latest build requests English and blocks confident non-English or Devanagari results, but Dictate controls its saved language and the owner still needs to verify English output on the Reno10. Latin-script Hindi cannot be reliably identified from the transcript alone.

## Project notes

- [Current checkpoint and exact next steps](PROJECT_CONTEXT.md)
- [Post-v1.0.19 Reno10 implementation plan](docs/POST_V1_0_19_RENO10_PLAN.md)
- [Conversation and handoff record](docs/PROJECT_CONVERSATION.md)
- [Privacy policy](PRIVACY_POLICY.md)

## Source and licenses

This project is based on [FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), HeliBoard, OpenBoard and AOSP LatinIME. See [LICENSE](LICENSE), [Apache notices](LICENSE-Apache-2.0) and [asset license](LICENSE-CC-BY-SA-4.0).