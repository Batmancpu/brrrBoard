// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
}
