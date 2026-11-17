# brrBoard

**A beautifully considered Android keyboard. By Mangoloads.**

brrBoard combines a fast, local-first typing engine with a restrained frosted-glass interface inspired by the clarity and rhythm of modern mobile keyboards.

## What makes it brrBoard

- **Frosted Glass** — the system backdrop and in-keyboard Emoji, Toolbar and Clipboard content are different materials, with independently controlled rendering.
- **Comfortable typing** — English and Hinglish layouts, smart suggestions, custom personal words and optional offline gesture typing libraries.
- **Expressive tools** — searchable Emoji, local Clipboard history, familiar editing shortcuts and an accessible microphone control.
- **Choice and control** — local backup/restore for your keyboard settings, words and compatible gesture library. No advertising or in-app donations.

brrBoard itself does not request internet access. If you use dictation, Android's selected speech recognition service manages audio processing and may use a network provider; see the [privacy policy](PRIVACY_POLICY.md).

## Install

**[Download the latest signed brrBoard APK](https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrBoard.apk)** · [Releases](https://github.com/Batmancpu/brrrBoard/releases) · [Obtainium setup](docs/OBTAINIUM.md)

### New Android package identity

Beginning with the Mangoloads identity release, the application ID is `com.mangoloads.board`. This is a **separate Android application** from the earlier `com.orion.frostkeys.brrrboard` builds; it cannot update the old installation in place. To bring existing data:

1. In your old brrBoard installation, export a local ZIP through Settings → Backup and restore. Confirm the ZIP exists in Downloads/brrBoard.
2. Install the new APK. Keep the old installation until the new one has passed your checks.
3. Open the new brrBoard → Settings → Backup and restore → Restore, and select your saved ZIP.
4. Verify personal words, settings and swipe typing library, then enable the new brrBoard under Android's keyboard list. Remove the older app only when satisfied.

Backup ZIPs may include learned words and Clipboard history. They are **not encrypted**; keep them private.

For English-only transcription in an external Dictate service, set **Dictate → Languages → Active language → English**, disable Follow keyboard language, and verify its own privacy/settings. brrBoard cannot override upstream recognition behavior.

## Support and feedback

No payment required. If brrBoard helps you, **[star the project](https://github.com/Batmancpu/brrrBoard)**. Have an idea or found a bug? **[Open an issue](https://github.com/Batmancpu/brrrBoard/issues)**.

Explore more independently made projects at **[Mangoloads](https://batmancpu.github.io/mangoloads/)** · [GitHub profile](https://github.com/Batmancpu).

## Engineering notes

- [Current implementation checkpoint](PROJECT_CONTEXT.md)
- [Open work and regressions](docs/KNOWN_ISSUES.md)
- [Engineering handoff](docs/ENGINEERING_HANDOFF.md)
- [Privacy policy](PRIVACY_POLICY.md)

## Open-source attribution

brrBoard is derived from the open-source [FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), HeliBoard, OpenBoard and AOSP LatinIME projects. Their required license notices and credits remain intact. See [GPL](LICENSE), [Apache notices](LICENSE-Apache-2.0) and [asset licenses](LICENSE-CC-BY-SA-4.0). Design inspiration does not imply endorsement by Apple.
