<picture>
  <source media="(prefers-color-scheme: dark)" srcset="art/brand/mangoloads/readme-hero-dark.png">
  <img src="art/brand/mangoloads/readme-hero-light.png" width="100%" alt="brrBoard by MangoLoads: a royal-blue ten-key keyboard with a mango resting on a pointed glass badge" />
</picture>

# brrBoard

**Your keyboard, thoughtfully made.**

A fast, local-first Android keyboard with a frosted-glass interface, familiar typing and useful everyday tools. Independently made by [MangoLoads](https://batmancpu.github.io/mangoloads/).

[![Android build and unit tests](https://github.com/Batmancpu/brrrBoard/actions/workflows/brrrboard-build-debug.yml/badge.svg?branch=main)](https://github.com/Batmancpu/brrrBoard/actions/workflows/brrrboard-build-debug.yml)

### [Download the latest signed APK](https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrBoard.apk)

[**Install and update with Obtainium**](docs/OBTAINIUM.md) · [All releases](https://github.com/Batmancpu/brrrBoard/releases) · [Privacy policy](PRIVACY_POLICY.md)

---

## Made for how you type

### A calmer keyboard

Frosted surfaces, considered spacing and responsive interactions make brrBoard feel coherent without asking your phone to render an animation for every keystroke. Its keyboard, Toolbar, Emoji and Clipboard surfaces use materials suited to their roles.

### Comfort in the details

English and Hinglish layouts, smart suggestions, a personal dictionary, editing shortcuts and optional imported gesture-typing libraries. Your selected keyboard layout and theme remain yours to control.

### Everyday tools, close at hand

Searchable Emoji, locally stored Clipboard history, keyboard configuration and a microphone control for on-device English transcription when the offline model is provisioned.

The design uses a single blue-keyboard-and-mango identity across the app icon, first-run setup and About. Functional keyboard controls stay functional; brand motion stays away from the typing path.

> **A note on visuals:** The marketing banner is brand artwork, not a simulated keyboard screenshot. Authentic screenshots of the signed identity build will be added after device verification. We won't present a mockup as a shipped feature.

## Install in three steps

1. **[Download `brrBoard.apk`](https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrBoard.apk)** from the latest GitHub release. Android may ask your browser or file manager for permission to install the APK.
2. Open brrBoard and follow **Enable keyboard** in the setup wizard. Android's input-method warning is a normal security reminder; please review the [privacy policy](PRIVACY_POLICY.md) before enabling any keyboard.
3. Choose **Switch to brrBoard** in the wizard or Android's keyboard picker. You're ready to type.

For straightforward signed updates, add [`https://github.com/Batmancpu/brrrBoard`](https://github.com/Batmancpu/brrrBoard) to [Obtainium](docs/OBTAINIUM.md). Keep the same app package and signing identity for in-place updates.

## Privacy, in plain language

- **No internet permission requested by brrBoard.** It does not include ad tracking or a payment/donation feature.
- **Keyboard data is local.** Settings, learned words, personal dictionaries and Clipboard history are stored on your device. Some optional features may access contacts only with appropriate device permissions.
- **Voice is deliberate and offline.** Tap the microphone to record, then tap again to transcribe; hold-and-release recording is also available. Audio is processed using a locally provisioned English recognition model, not sent to a transcription server by the keyboard.
- **Backups are private.** A manually exported backup ZIP may contain personal words and Clipboard content; it is **not encrypted**. Keep it somewhere safe.

Read the full [brrBoard privacy policy](PRIVACY_POLICY.md) for precise behavior and limitations.

### Before using voice or swipe

The standard signed update is designed to **reuse** an offline voice model already stored privately by brrBoard; it does **not** bundle about 136 MB of duplicate weights every time. A maintainer-triggered model-inclusive Obtainium build is needed for first-time provisioning or recovery. There is no automatic model download on the phone.

Gesture typing can use a separately imported, compatible offline library. It is not silently bundled into every install. See the in-app Gesture Typing setup for your device's available options.

## Moving from an older brrBoard

The current app package is **`com.mangoloads.board`**. Older installations using `com.orion.frostkeys.brrrboard` are a *different* Android package and cannot be upgraded in place to the new ID.

1. In the old app, export **Settings → Backup and restore** to a local ZIP.
2. Install the new signed brrBoard APK, but keep the older installation until you've checked your data.
3. In the new app, restore your backup and verify preferences, personal words and any imported gesture library.
4. Enable the new keyboard in Android settings. Only remove the old installation when everything works as expected.

Do not share the unencrypted ZIP publicly.

## Questions

<details>
<summary>Does brrBoard require internet access?</summary>

The keyboard itself does not request Android internet permission. Opening a GitHub or website link hands that action to your browser or another application.

</details>

<details>
<summary>Why does Android warn me when I enable the keyboard?</summary>

Android displays a standard trust prompt for input methods because a keyboard can observe what you enter into editors. Review the app permissions and [privacy policy](PRIVACY_POLICY.md) before deciding.

</details>

<details>
<summary>Why might offline voice be unavailable on a new install?</summary>

Normal incremental APKs omit the model weights. The private recognition model must first be supplied in a deliberate, signed bootstrap/recovery APK. The app does not download those weights automatically.

</details>

<details>
<summary>Can I import swipe typing support?</summary>

Yes, compatible offline gesture-typing libraries can be imported through settings. Availability and licensing are separate from the brrBoard APK. Check the in-app Gesture Typing page for the supported import workflow.

</details>

<details>
<summary>Is a local backup encrypted?</summary>

No. A backup may include learned words and Clipboard history. Treat it as sensitive and delete copies you no longer need.

</details>

## Build and contribute

brrBoard is an open-source Android project. The [contribution guide](CONTRIBUTING.md) explains the current repository and privacy-conscious testing approach. For technical details, see the [documentation index](docs/README.md) and the [current engineering checkpoint](PROJECT_CONTEXT.md).

The repository currently has public GitHub Issues disabled. We won't send people to a nonworking issue form; please check this repository for an available support channel. If a public tracker is enabled later, that section will be updated.

## Built on the work of others

brrBoard is a MangoLoads continuation of open-source [FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), HeliBoard, OpenBoard and AOSP LatinIME. We keep their required source and asset attributions intact. See [GPL-3.0](LICENSE), [Apache notices](LICENSE-Apache-2.0), [asset license](LICENSE-CC-BY-SA-4.0) and [NOTICE](NOTICE).

*Designed with care for Android. Apple and Gboard are not affiliated with or endorsing this project.*
