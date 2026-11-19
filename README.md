# brrBoard

**A beautifully considered Android keyboard. By Mangoloads.**

brrBoard combines a fast, local-first typing engine with a restrained frosted-glass interface inspired by the clarity and rhythm of modern mobile keyboards.

## What makes it brrBoard

- **Frosted Glass** — the system backdrop and in-keyboard Emoji, Toolbar and Clipboard content are different materials, with independently controlled rendering.
- **Comfortable typing** — English and Hinglish layouts, smart suggestions, custom personal words and optional offline gesture typing libraries.
- **Expressive tools** — searchable Emoji, local Clipboard history, familiar editing shortcuts and an accessible microphone control.
- **Choice and control** — local backup/restore for your keyboard settings, words and compatible gesture library. No advertising or in-app donations.

brrBoard itself does not request internet access. Its microphone uses a **private, offline English model**: tap once to record, tap again to transcribe, or hold and release. The mic glows red while listening and turquoise while decoding. Normal signed updates **reuse the verified model already installed in the app's private storage** and do not include roughly 136 MB of duplicate weights. For first-time setup or recovery, a maintainer can manually dispatch the original-signed Obtainium workflow with `bundle_voice_model` enabled; that special update embeds the hash-pinned model. No brrSpeak app, server, or on-phone model download is needed. See the [privacy policy](PRIVACY_POLICY.md).

## Install

**[Download the latest signed brrBoard APK](https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrBoard.apk)** · [Releases](https://github.com/Batmancpu/brrrBoard/releases) · [Obtainium setup](docs/OBTAINIUM.md)

### New Android package identity

Beginning with the Mangoloads identity release, the application ID is `com.mangoloads.board`. This is a **separate Android application** from the earlier `com.orion.frostkeys.brrrboard` builds; it cannot update the old installation in place. To bring existing data:

1. In your old brrBoard installation, export a local ZIP through Settings → Backup and restore. Confirm the ZIP exists in Downloads/brrBoard.
2. Install the new APK. Keep the old installation until the new one has passed your checks.
3. Open the new brrBoard → Settings → Backup and restore → Restore, and select your saved ZIP.
4. Verify personal words, settings and swipe typing library, then enable the new brrBoard under Android's keyboard list. Remove the older app only when satisfied.

Backup ZIPs may include learned words and Clipboard history. They are **not encrypted**; keep them private.

English transcription is local to brrBoard. The optionally bootstrapped NVIDIA Parakeet 110M INT8 model is a compact candidate; Indian-English accuracy and cold-start latency must be verified on real devices, including Oppo Reno10 5G.

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
