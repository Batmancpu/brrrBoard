# brrBoard privacy policy

**brrBoard — by Mangoloads**

brrBoard keeps its keyboard settings, learned words, personal dictionaries and optional local gesture data on your device. It does not request Android internet access. If enabled and authorized, contacts may be used for local suggestions. Clipboard history is stored locally and can be cleared from the keyboard settings.

A user-initiated local backup can export these settings, dictionary files, optional gesture library and other app data to a ZIP in your Downloads/brrBoard folder. The ZIP can contain private words and clipboard content; it is **unencrypted** and is not uploaded by brrBoard. Keep and delete it as you choose.

brrBoard records microphone audio only after an explicit microphone action. **A single tap starts private English recording; the next tap stops and transcribes it. Press-and-hold also records locally and finishes on release.** Both use a verified English recognition model stored privately on the device. The mic does not invoke brrSpeak, Android's selected speech service, any network transcription provider, or another dictation app. It glows red while listening and turquoise while decoding. brrBoard itself has no Android internet permission.

Normal signed APK updates do not include model weights. A deliberately dispatched model-bootstrap/recovery update can embed four pinned, hash-verified files; brrBoard verifies and extracts them on a background worker into private no-backup app storage. Subsequent in-place updates retain that private copy. **There is no additional model download, account, or network request from the phone.** Audio stays in memory during recording and recognition, with temporary PCM and per-section sample buffers wiped after use; no voice-history database, audio upload, WAV file, automatic retraining or transcript history is created by the offline path. User-provided spoken text is inserted into the current editor, while the existing keyboard's typing/suggestion and Emoji storage remain separate. Uninstalling or clearing app data removes the model and its private data; in that case, a model-inclusive recovery update is needed.

The earlier brrSpeak Binder integration remains in the source as dormant compatibility code, but the brrBoard microphone and VOICE_INPUT key no longer enter that integration. brrSpeak has its own privacy policy if you choose to open or use it separately.

External website and GitHub links open in other applications. brrBoard itself does not include ad trackers or a donation/payment feature.

For questions and bugs, visit the [brrBoard issue tracker](https://github.com/Batmancpu/brrrBoard/issues). For other projects visit [Mangoloads](https://batmancpu.github.io/mangoloads/).

See the repository's [LICENSE](LICENSE) and retained upstream notices for third-party code attribution.
