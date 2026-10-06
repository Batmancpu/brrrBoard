package helium314.keyboard.latin.diagnostics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import helium314.keyboard.latin.BuildConfig

object DiagnosticExporter {
    fun export(context: Context) {
        MainScope().launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                    val timestamp = dateFormat.format(Date())
                    val fileName = "brrrBoard-diagnostic-$timestamp.zip"

                    // Create log content
                    val stringBuilder = java.lang.StringBuilder()
                    stringBuilder.append("brrrBoard Diagnostic Report\n")
                    stringBuilder.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    stringBuilder.append("Android SDK: ${Build.VERSION.SDK_INT}\n")
                    stringBuilder.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                    stringBuilder.append("--------------------------------------------------\n\n")

                    DiagnosticLogger.getLogs().forEach {
                        stringBuilder.append(it).append("\n")
                    }

                    // Zip it up in cache first
                    val cacheZipFile = File(context.cacheDir, fileName)
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(cacheZipFile))).use { zos ->
                        val entry = ZipEntry("diagnostic_logs.txt")
                        zos.putNextEntry(entry)
                        zos.write(stringBuilder.toString().toByteArray())
                        zos.closeEntry()
                    }

                    // Export to Downloads
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/brrrBoard/" + timestamp.substring(0, 10) + "/" + timestamp.substring(11))
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            ?: throw Exception("Failed to create MediaStore entry")

                        resolver.openOutputStream(uri)?.use { os ->
                            FileInputStream(cacheZipFile).use { input ->
                                input.copyTo(os)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    } else {
                        @Suppress("DEPRECATION")
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val exportDir = File(downloadsDir, "brrrBoard/" + timestamp.substring(0, 10) + "/" + timestamp.substring(11))
                        exportDir.mkdirs()
                        val exportFile = File(exportDir, fileName)
                        FileInputStream(cacheZipFile).use { input ->
                            FileOutputStream(exportFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    cacheZipFile.delete()
                    "Diagnostic exported to Downloads/brrrBoard"
                }.getOrElse { "Export failed: ${it.message}" }
            }
            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
        }
    }
}
