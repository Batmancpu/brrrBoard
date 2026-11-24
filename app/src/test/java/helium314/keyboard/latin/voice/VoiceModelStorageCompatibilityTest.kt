// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceModelStorageCompatibilityTest {
    @Test
    fun verifiedModelInCredentialStorageRemainsDiscoverableAfterStorageDefaultChanges() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pack = VoiceModelPack(
            id = "test-pack-ce-fallback",
            assetDir = "unused",
            assets = listOf(VoiceModelAsset("encoder.onnx", 4L, "unused")),
        )
        val credentialDir = File(context.noBackupFilesDir, pack.modelDir)
        val deviceDir = File(context.createDeviceProtectedStorageContext().noBackupFilesDir, pack.modelDir)
        credentialDir.deleteRecursively()
        deviceDir.deleteRecursively()
        credentialDir.mkdirs()
        File(credentialDir, ".verified-${pack.id}").writeText("verified v1\n")
        File(credentialDir, "encoder.onnx").writeBytes(byteArrayOf(1, 2, 3, 4))

        try {
            assertTrue(VoiceModelInstaller.isInstalled(context, pack))
            assertEquals(credentialDir.absolutePath,
                VoiceModelInstaller.modelDir(context, pack).absolutePath)
        } finally {
            credentialDir.deleteRecursively()
            deviceDir.deleteRecursively()
        }
    }

    @Test fun retireLegacyRequiresReplacement() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacy = VoiceModelPack("test-legacy-retire","unused",listOf(VoiceModelAsset("encoder.onnx",4L,"unused")))
        val ultra = VoiceModelPack("test-ultra-retire","unused",listOf(VoiceModelAsset("encoder.onnx",4L,"unused")))
        val oldDirs = listOf(File(context.createCredentialProtectedStorageContext().noBackupFilesDir,legacy.modelDir),
            File(context.createDeviceProtectedStorageContext().noBackupFilesDir,legacy.modelDir)).distinct()
        val replacement = File(context.createCredentialProtectedStorageContext().noBackupFilesDir,ultra.modelDir)
        fun stage(dir: File,pack: VoiceModelPack) {
            dir.mkdirs()
            File(dir,".verified-${pack.id}").writeText("verified v1")
            File(dir,"encoder.onnx").writeBytes(byteArrayOf(1,2,3,4))
        }
        try {
            oldDirs.forEach { stage(it,legacy) }
            assertFalse(VoiceModelInstaller.retireLegacyAfterReplacement(context,ultra,legacy))
            assertTrue(oldDirs.all { it.exists() })
            stage(replacement,ultra)
            assertTrue(VoiceModelInstaller.retireLegacyAfterReplacement(context,ultra,legacy))
            assertTrue(oldDirs.none { it.exists() })
            assertTrue(VoiceModelInstaller.isInstalled(context,ultra))
        } finally { oldDirs.forEach { it.deleteRecursively() }; replacement.deleteRecursively() }
    }

}
