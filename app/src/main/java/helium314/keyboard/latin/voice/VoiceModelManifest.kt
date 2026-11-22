// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/**
 * Fixed English-only Parakeet TDT 110M INT8 release. Source hashes are shared with the
 * upstream Dictate open-source model catalog and checked before any native model load.
 */
internal data class VoiceModelAsset(val name: String, val size: Long, val sha256: String)

internal data class VoiceModelPack(
    val id: String,
    val assetDir: String,
    val assets: List<VoiceModelAsset>,
) {
    val modelDir: String get() = "voice-models/$id"
    val encoderName: String get() = "encoder.onnx"
    val decoderName: String get() = "decoder.onnx"
    val joinerName: String get() = "joiner.onnx"
    val tokensName: String get() = "tokens.txt"
}

internal object VoiceModelManifest {
    const val ID = "parakeet-tdt-110m-en-v1"
    const val ASSET_DIR = "voice-model/parakeet-tdt-110m-en"
    const val MODEL_DIR = "voice-models/$ID"

    val assets = listOf(
        VoiceModelAsset("encoder.onnx", 131_113_202L, "0f35509ddeb9b39002fb077d979a9fe74f06eb0bc4dd5c34f512f82e5111d657"),
        VoiceModelAsset("decoder.onnx", 3_955_863L, "f7c331c5504c2e593c76ed22b728e3f554af6c4a383dde862e719ced08b1da19"),
        VoiceModelAsset("joiner.onnx", 1_411_403L, "bf7dff69e9f2cdbe9943d70da358f38b361c115ba0105bae7e908e0d6ec782f6"),
        VoiceModelAsset("tokens.txt", 9_953L, "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10"),
    )

    val pack = VoiceModelPack(ID, ASSET_DIR, assets)

    const val MODEL_LICENSE = "CC-BY-4.0 (NVIDIA); sherpa-onnx ONNX export Apache-2.0"
}

/** Official sherpa-onnx maintainer INT8 TDT 0.6B export, test-only side-by-side profile. */
internal object UltraVoiceModelManifest {
    const val ID = "parakeet-ultra-0.6b-int8-test-v1"
    const val ASSET_DIR = "voice-model/parakeet-ultra-0.6b-int8"
    const val MODEL_LICENSE = "CC-BY-4.0 (NVIDIA/Moondream); sherpa-onnx export Apache-2.0"
    val assets = listOf(
        VoiceModelAsset("encoder.onnx", 652_282_345L, "b43072e1b721f18a3df03fcfeb763ff7c4debdf29843598a3aba2f703e2780a3"),
        VoiceModelAsset("decoder.onnx", 11_845_274L, "1fab98fe6c12aded87d2da66272cc9e148d0a0044ce3850a12fe56302ec4a922"),
        VoiceModelAsset("joiner.onnx", 6_355_277L, "8a71aaccfdba3d451775507a889afd2257d2e754463fd81095a8c8f290f924c2"),
        VoiceModelAsset("tokens.txt", 93_939L, "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"),
    )
    val pack = VoiceModelPack(ID, ASSET_DIR, assets)
}
