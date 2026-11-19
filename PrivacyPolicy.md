# brrBoard privacy policy

**brrBoard — by Mangoloads**

brrBoard keeps its keyboard settings, learned words, personal dictionaries and optional local gesture data on your device. It does not request Android internet access. If enabled and authorized, contacts may be used for local suggestions. Clipboard history is stored locally and can be cleared from the keyboard settings.

A user-initiated local backup can export these settings, dictionary files, optional gesture library and other app data to a ZIP in your Downloads/brrBoard folder. The ZIP can contain private words and clipboard content; it is **unencrypted** and is not uploaded by brrBoard. Keep and delete it as you choose.

brrBoard records microphone audio itself only when you request voice input. A normal microphone tap passes 16 kHz PCM audio to the installed **brrSpeak** speech service; brrSpeak uses your configured transcription provider, which may receive the audio over the network. brrBoard does not transmit surrounding editor text, contacts, clipboard, or API credentials to brrSpeak. Review brrSpeak's provider and privacy settings before using this red/server mode.

Holding the microphone uses the separately installed, one-time English model **inside brrBoard**. Blue/offline recognition does not contact brrSpeak or any provider and cannot send audio online because brrBoard has no Android internet permission. Microphone frames and transcripts are processed in RAM only and discarded after use; no voice-history database, WAV file, automatic retraining or automatic model download is created. If the model is installed, the normal red/server recording also produces an in-memory offline backup which you may explicitly choose to paste when the provider is slow or fails. Model weights are kept privately in Android's no-backup app storage to survive ordinary signed updates, but uninstalling the app removes them. The keyboard's existing typed-word/suggestion storage is separate; voice insertion does not intentionally learn the transcript.

External website and GitHub links open in other applications. brrBoard itself does not include ad trackers or a donation/payment feature.

For questions and bugs, visit the [brrBoard issue tracker](https://github.com/Batmancpu/brrrBoard/issues). For other projects visit [Mangoloads](https://batmancpu.github.io/mangoloads/).

See the repository's [LICENSE](LICENSE) and retained upstream notices for third-party code attribution.
