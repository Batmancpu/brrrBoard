// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/**
 * Fixed English-only Parakeet TDT 110M INT8 release. Source hashes are shared with the
 * upstream Dictate open-source model catalog and checked before any native model load.
 */
internal object VoiceModelManifest {
    const val ID = "parakeet-tdt-110m-en-v1"
    const val ASSET_DIR = "voice-model/parakeet-tdt-110m-en"
    const val MODEL_DIR = "voice-models/$ID"

    data class Asset(val name: String, val size: Long, val sha256: String)

    val assets = listOf(
        Asset("encoder.onnx", 131_113_202L, "0f35509ddeb9b39002fb077d979a9fe74f06eb0bc4dd5c34f512f82e5111d657"),
        Asset("decoder.onnx", 3_955_863L, "f7c331c5504c2e593c76ed22b728e3f554af6c4a383dde862e719ced08b1da19"),
        Asset("joiner.onnx", 1_411_403L, "bf7dff69e9f2cdbe9943d70da358f38b361c115ba0105bae7e908e0d6ec782f6"),
        Asset("tokens.txt", 9_953L, "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10"),
    )

    const val MODEL_LICENSE = "CC-BY-4.0 (NVIDIA); sherpa-onnx ONNX export Apache-2.0"
}
