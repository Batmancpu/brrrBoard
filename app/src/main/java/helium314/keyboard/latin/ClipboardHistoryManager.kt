// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.app.AlertDialog
import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.database.ContentObserver
import helium314.keyboard.latin.suggestions.ClipboardAccessoryView
import helium314.keyboard.latin.suggestions.ContextualAccessoryPolicy
import helium314.keyboard.latin.brain.SmartPastePolicy
import helium314.keyboard.latin.brain.SmartPasteLearning
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import helium314.keyboard.latin.database.ClipboardImageVault
import androidx.core.content.edit
import androidx.core.view.isGone
import coil.load
import coil.request.CachePolicy
import helium314.keyboard.compat.ClipboardManagerCompat
import helium314.keyboard.compat.isDeviceLocked
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.voice.RecoveredTranscript
import helium314.keyboard.latin.voice.RecoveredTranscriptRepository
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.databinding.ClipboardSuggestionBinding
import helium314.keyboard.latin.permissions.PermissionsUtil
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.io.File
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClipboardHistoryManager(
        private val latinIME: LatinIME
) : ClipboardManager.OnPrimaryClipChangedListener {

    private lateinit var clipboardManager: ClipboardManager
    private var clipboardSuggestionView: ClipboardAccessoryView? = null
    private data class AccessoryKey(val clip: RecentClip.Text?, val rich: RichPaste?,
        val epoch: Long, val connection: android.view.inputmethod.InputConnection,
        val candidate: SmartPastePolicy.Candidate?)
    private data class ClassifiedCopy(val clip: RecentClip.Text, val candidates: List<SmartPastePolicy.Candidate>)
    private var classifiedCopy: ClassifiedCopy? = null
    private var activeDecision: SmartPastePolicy.Decision? = null
    private var pasteDeadline = 0L
    private var otpBlockedThrough = 0L
    private fun learningAllowed() = !chromePrivate() &&
        latinIME.mSettings.current.mUsePersonalizedDicts
    private fun recordPasteDecision(accepted: Boolean) {
        val d = activeDecision ?: return
        if (learningAllowed()) {
            SmartPasteLearning.initialize(latinIME)
            SmartPasteLearning.record(d.candidate.kind,d.destination,accepted)
        }
    }
    /** Only reads the already-cached editor suffix supplied by InputLogic, never the OS clipboard. */
    fun hasActiveSmartPaste(): Boolean = activeDecision?.candidate?.kind?.let {
        it != SmartPastePolicy.Kind.OTP && it != SmartPastePolicy.Kind.RICH
    } == true && clipboardSuggestionView?.retired == false

    fun updateSmartPasteIntent(suffix: String, typing: Boolean) {
        val current = accessoryKey ?: return
        val clip = current.clip ?: return
        val root = clipboardSuggestionView ?: return
        val decision = activeDecision ?: return
        if (root.retired || decision.destination == SmartPastePolicy.Destination.OTP) return
        if (chromePrivate() || latinIME.editorGeneration() != current.epoch ||
            latinIME.currentInputConnection !== current.connection || SystemClock.elapsedRealtime() >= pasteDeadline) {
            releaseClipboardAccessory()
            return
        }
        val snapshot = classifiedCopy ?: return
        val age = System.currentTimeMillis() - clip.timeStamp
        val bias = if (learningAllowed()) SmartPasteLearning.bias(decision.candidate.kind, decision.destination) else 0
        val updated = SmartPastePolicy.decide(snapshot.candidates, decision.destination, age, suffix, typing, bias,
            age in 0L until CleanLinkPolicy.WINDOW_MS && root.hasLiveChrome)
        if (updated == null) {
            releaseClipboardAccessory()
            return
        }
        activeDecision = updated
        root.prefersProminent = updated.prominent
        val intent = SmartPastePolicy.intent(decision.candidate.kind, suffix) ||
            (decision.destination == SmartPastePolicy.Destination.GENERAL &&
                snapshot.candidates.any { SmartPastePolicy.intent(it.kind, suffix) })
        root.updateIntentEmphasis(intent)
    }
    private var accessoryKey: AccessoryKey? = null
    private var pasteExpiry: Runnable? = null

    fun releaseClipboardAccessory() {
        cancelChromeExpiry()
        pasteExpiry?.let { latinIME.mHandler.removeCallbacks(it) }
        pasteExpiry = null
        val old = clipboardSuggestionView
        if (activeDecision?.candidate?.kind == SmartPastePolicy.Kind.OTP)
            accessoryKey?.clip?.let { otpBlockedThrough = maxOf(otpBlockedThrough,it.timeStamp) }
        clipboardSuggestionView = null
        accessoryKey = null
        classifiedCopy = null
        activeDecision = null
        pasteDeadline = 0L
        old?.retire()
    }
    private var chromeCopyTimestamp = 0L
    private var chromeDeadline = 0L
    private var chromeBlockedThrough = 0L
    private var chromeExpiry: Runnable? = null
    private fun cancelChromeExpiry() {
        chromeExpiry?.let { latinIME.mHandler.removeCallbacks(it) }
        chromeExpiry = null
    }
    private fun invalidateChrome() {
        chromeBlockedThrough = maxOf(chromeBlockedThrough, System.currentTimeMillis(), chromeCopyTimestamp)
        otpBlockedThrough = maxOf(otpBlockedThrough, System.currentTimeMillis())
        chromeDeadline = 0L
        cancelChromeExpiry()
    }
    private fun chromeRemaining(copyTime: Long): Long {
        if (copyTime <= chromeBlockedThrough) return 0L
        val remaining = CleanLinkPolicy.remaining(copyTime, System.currentTimeMillis())
        if (remaining <= 0L) return 0L
        val now = SystemClock.elapsedRealtime()
        if (copyTime != chromeCopyTimestamp) {
            chromeCopyTimestamp = copyTime
            chromeDeadline = now + remaining
        }
        return minOf(remaining, chromeDeadline - now).coerceAtLeast(0L)
    }
    // Do not use the compatibility timestamp fallback for a navigation action.
    private fun strictTextReceipt(): OwnedClip? = try {
        val live = clipboardManager.primaryClip
        if (live == null || ClipboardPrivacyPolicy.boundedPlainText(live) == null) null
        else clipReceipt(live, true)
    } catch (_: Exception) { null }
    private fun chromePrivate(): Boolean {
        val values = latinIME.mSettings.current
        return isHistoryPrivate() || values.mInputAttributes.mNoLearning ||
            values.mInputAttributes.mVaultPrivateEditor
    }
    private var extractionDialog: AlertDialog? = null
    private val expireExtraction = Runnable { dismissExtraction() }
    fun dismissExtraction() {
        latinIME.mHandler.removeCallbacks(expireExtraction)
        val dialog = extractionDialog
        extractionDialog = null
        dialog?.dismiss()
    }
    private var cachedClipboardDao: ClipboardDao? = null
    // The IME can start before first unlock. Retry lazily after unlock instead of
    // permanently caching the expected locked-storage failure from onCreate().
    private val clipboardDao: ClipboardDao?
        get() {
            cachedClipboardDao?.let { return it }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val userManager = latinIME.getSystemService(UserManager::class.java)
                if (userManager?.isUserUnlocked != true) return null
            }
            return ClipboardDao.getInstance(latinIME)?.also {
                cachedClipboardDao = it
            }
        }
    private var screenshotObserver: ContentObserver? = null
    private var lastProcessedImageUri: String? = null
    private val processedScreenshotUris = ArrayDeque<String>()
    private val screenshotInFlightUris = mutableSetOf<String>()
    private val inaccessibleImageClipUris = mutableSetOf<String>()
    @Volatile
    private var latestImageSuggestion: RecentClip.Image? = null
    @Volatile
    private var latestScreenshotSuggestion: RecentClip.Image? = null
    // Provenance is deliberately process-local and content-free. Unknown origin
    // means the system clipboard is never cleared when history is cleared.
    private data class OwnedClip(val fingerprint: String, val timestamp: Long)
    private var ownedPrimaryClip: OwnedClip? = null
    // Finalized voice text is a separate RAM-only recovery item, never an OS clip.
    private val recoveryRepository by lazy { RecoveredTranscriptRepository(latinIME) }
    private val recoveryScreenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                clearRecoveredTranscripts()
                invalidateChrome()
                releaseClipboardAccessory()
            }
        }
    }
    private var recoveryReceiverRegistered = false
    fun setVoiceRecoveryChangeListener(listener: (() -> Unit)?) {
        recoveryRepository.onChanged = listener
    }

    fun onCreate() {
        // Purge any legacy encrypted voice-recovery file; new recovery is RAM-only.
        clearRecoveredTranscripts()
        ContextCompat.registerReceiver(latinIME, recoveryScreenOff,
            IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        recoveryReceiverRegistered = true
        clipboardManager = latinIME.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(this)
        clipboardDao // attempt once now; getter retries on later access if still locked
        GlobalScope.launch(Dispatchers.IO) { ClipboardImageVault.migrateExisting(latinIME.applicationContext) }
        restoreProcessedScreenshotUris()
        // Migrate away from old preferences containing raw copied text/URI.
        ClipboardDismissalPreferences.removeLegacyRawContent(latinIME.prefs())
        if (latinIME.mSettings.current.mClipboardHistoryEnabled)
            fetchPrimaryClip()
        syncScreenshotObserver()
    }

    fun onDestroy() {
        releaseClipboardAccessory()
        setVoiceRecoveryChangeListener(null)
        clearRecoveredTranscripts()
        if (recoveryReceiverRegistered) {
            latinIME.unregisterReceiver(recoveryScreenOff)
            recoveryReceiverRegistered = false
        }
        clearDecryptedHistory()
        dismissExtraction()
        clipboardManager.removePrimaryClipChangedListener(this)
        unregisterScreenshotObserver()
    }

    private fun syncScreenshotObserver() {
        if (shouldObserveScreenshots()) registerScreenshotObserver()
        else unregisterScreenshotObserver()
    }

    private fun shouldObserveScreenshots(): Boolean {
        val settings = latinIME.mSettings.current
        return settings.mClipboardHistoryEnabled
                && settings.mShowScreenshotsInClipboard
                && !isHistoryPrivate()
                && (hasScreenshotFolderAccess() || hasScreenshotReadPermission())
    }

    private fun hasScreenshotFolderAccess(): Boolean {
        val uriStr = latinIME.prefs().getString(PREF_SCREENSHOTS_FOLDER_URI, null) ?: return false
        val uri = Uri.parse(uriStr)
        return try {
            latinIME.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun hasScreenshotReadPermission(): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Manifest.permission.READ_EXTERNAL_STORAGE
            else -> return true
        }
        return PermissionsUtil.checkAllPermissionsGranted(latinIME, permission)
    }

    private fun registerScreenshotObserver() {
        if (screenshotObserver != null) return
        
        screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onScreenshotMediaChanged()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                onScreenshotMediaChanged()
            }

            @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
            override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int) {
                super.onChange(selfChange, uris, flags)
                onScreenshotMediaChanged()
            }
        }
        
        try {
            latinIME.contentResolver.registerContentObserver(
                imageCollectionUri(),
                true,
                screenshotObserver!!
            )
        } catch (e: Exception) {
            Log.e("ClipboardHistoryManager", "Failed to register screenshot content observer", e)
        }
    }

    private fun unregisterScreenshotObserver() {
        screenshotObserver?.let {
            try {
                latinIME.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e("ClipboardHistoryManager", "Failed to unregister screenshot observer", e)
            }
            screenshotObserver = null
        }
    }

    private fun onScreenshotMediaChanged() {
        if (!shouldObserveScreenshots()) {
            syncScreenshotObserver()
            return
        }
        checkForNewScreenshot()
    }

    private fun checkForNewScreenshot() {
        val editorEpoch = latinIME.editorGeneration()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (!shouldObserveScreenshots() || editorEpoch != latinIME.editorGeneration()) return@launch
                val dao = clipboardDao
                var processedAny = false

                for (screenshot in queryRecentScreenshots().asReversed()) {
                    if (!shouldObserveScreenshots() || editorEpoch != latinIME.editorGeneration()) break
                    val uriString = screenshot.uri.toString()
                    if (!claimScreenshotUri(uriString)) continue

                    var processed = false
                    try {
                        val timeStamp = screenshot.dateAdded * 1000L
                        if (!ScreenshotSuggestionPolicy.isRecent(timeStamp, System.currentTimeMillis())) {
                            continue
                        }
                        val imageClip = RecentClip.Image(
                            uri = screenshot.uri,
                            mimeType = normalizeImageMimeType(screenshot.mimeType).mimeType,
                            label = "Screenshot",
                            timeStamp = timeStamp
                        )

                        val cachedUri = cacheImageClip(imageClip)
                        if (cachedUri != null && dao != null && shouldObserveScreenshots()
                            && editorEpoch == latinIME.editorGeneration()) {
                            dao.addClip(
                                timeStamp,
                                false,
                                encodeImageHistoryClip(cachedUri, imageClip.mimeType, imageClip.label)
                            )
                            val localScreenshot = imageClip.copy(uri = cachedUri)
                            latestScreenshotSuggestion = localScreenshot
                            rememberImageSuggestion(localScreenshot)
                            processed = true
                            processedAny = true
                        }
                    } catch (e: Exception) {
                        Log.w("ClipboardHistoryManager", "Failed processing screenshot (${e.javaClass.simpleName})")
                    } finally {
                        completeScreenshotUri(uriString, screenshot.dateAdded, processed)
                    }
                }

                if (processedAny) {
                    refreshClipboardSuggestion()
                }
            } catch (e: Exception) {
                Log.e("ClipboardHistoryManager", "Failed checking for new screenshot", e)
            }
        }
    }

    private data class ScreenshotMedia(
        val uri: Uri,
        val mimeType: String,
        val dateAdded: Long
    )

    private fun queryRecentScreenshotsFromSaf(treeUri: Uri): List<ScreenshotMedia> {
        val screenshots = mutableListOf<ScreenshotMedia>()
        val recentSinceMillis = System.currentTimeMillis() - (SCREENSHOT_RECENT_WINDOW_SECONDS * 1000L)
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            var newestDocId: String? = null
            var newestMimeType: String? = null
            var newestLastModified = 0L

            latinIME.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val dateIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (idIndex == -1 || mimeIndex == -1 || dateIndex == -1) return emptyList()

                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else null
                    if (name != null && (name.startsWith(".") || name.contains(".pending", ignoreCase = true))) {
                        continue
                    }
                    val childDocId = cursor.getString(idIndex) ?: continue
                    if (childDocId.contains("/.") || childDocId.contains(".pending", ignoreCase = true)) {
                        continue
                    }
                    val lastModified = cursor.getLong(dateIndex)
                    val mime = cursor.getString(mimeIndex)
                    if (mime != null && ClipDescription.compareMimeTypes(mime, "image/*")) {
                        if (lastModified > newestLastModified) {
                            newestLastModified = lastModified
                            newestDocId = childDocId
                            newestMimeType = mime
                        }
                    }
                }
            }

            if (newestDocId != null && newestLastModified >= recentSinceMillis) {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, newestDocId)
                screenshots.add(
                    ScreenshotMedia(
                        uri = documentUri,
                        mimeType = normalizeImageMimeType(newestMimeType ?: "image/png").mimeType,
                        dateAdded = newestLastModified / 1000L
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("ClipboardHistoryManager", "Failed querying screenshots from SAF (${e.javaClass.simpleName})")
        }
        return screenshots
    }

    private fun queryRecentScreenshots(): List<ScreenshotMedia> {
        val folderUriStr = latinIME.prefs().getString(PREF_SCREENSHOTS_FOLDER_URI, null)
        if (folderUriStr != null) {
            val safResults = queryRecentScreenshotsFromSaf(Uri.parse(folderUriStr))
            if (safResults.isNotEmpty() || !hasScreenshotReadPermission()) {
                return safResults
            }
        }
        if (!hasScreenshotReadPermission()) return emptyList()
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Images.Media.DATA)
            }
        }.toTypedArray()
        val recentSinceSeconds = System.currentTimeMillis() / 1000L - SCREENSHOT_RECENT_WINDOW_SECONDS
        val (selection, selectionArgs) = screenshotSelection(recentSinceSeconds)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
        val screenshots = mutableListOf<ScreenshotMedia>()
        latinIME.contentResolver.query(
            imageCollectionUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            }
            if (idIndex == -1 || dateIndex == -1 || mimeIndex == -1 || pathIndex == -1) return emptyList()

            var checkedRows = 0
            while (cursor.moveToNext() && checkedRows < MAX_SCREENSHOT_ROWS_TO_CHECK) {
                checkedRows++
                val path = cursor.getString(pathIndex)
                if (!isScreenshotPath(path)) continue

                val id = cursor.getLong(idIndex)
                val itemUri = ContentUris.withAppendedId(imageCollectionUri(), id)
                val mimeType = cursor.getString(mimeIndex)
                    ?.takeIf { ClipDescription.compareMimeTypes(it, "image/*") }
                    ?: latinIME.contentResolver.getType(itemUri)
                        ?.takeIf { ClipDescription.compareMimeTypes(it, "image/*") }
                    ?: continue
                screenshots.add(ScreenshotMedia(itemUri, normalizeImageMimeType(mimeType).mimeType, cursor.getLong(dateIndex)))
            }
        }
        return screenshots
    }

    private fun imageCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun screenshotSelection(recentSinceSeconds: Long): Pair<String, Array<String>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pathSelection = SCREENSHOT_RELATIVE_PATHS.joinToString(" OR ") {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            }
            val selection = "($pathSelection) AND ${MediaStore.Images.Media.DATE_ADDED} >= ? " +
                    "AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ? " +
                    "AND ${MediaStore.Images.Media.IS_PENDING} = 0"
            val args = SCREENSHOT_RELATIVE_PATHS.map { "$it%" } +
                    listOf(recentSinceSeconds.toString(), "image/%")
            selection to args.toTypedArray()
        } else {
            @Suppress("DEPRECATION")
            val pathSelection = screenshotAbsoluteDirectoryPrefixes().joinToString(" OR ") {
                "${MediaStore.Images.Media.DATA} LIKE ?"
            }
            @Suppress("DEPRECATION")
            val selection = "($pathSelection) AND ${MediaStore.Images.Media.DATE_ADDED} >= ? " +
                    "AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val args = screenshotAbsoluteDirectoryPrefixes().map { "$it%" } +
                    listOf(recentSinceSeconds.toString(), "image/%")
            selection to args.toTypedArray()
        }
    }

    private fun isScreenshotPath(path: String?): Boolean {
        val normalizedPath = path?.replace('\\', '/') ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SCREENSHOT_RELATIVE_PATHS.any { normalizedPath.startsWith(it, ignoreCase = true) }
        } else {
            screenshotAbsoluteDirectoryPrefixes()
                .map { it.replace('\\', '/') }
                .any { normalizedPath.startsWith(it, ignoreCase = true) }
        }
    }

    private fun screenshotAbsoluteDirectoryPrefixes(): List<String> {
        @Suppress("DEPRECATION")
        val root = Environment.getExternalStorageDirectory()
        @Suppress("DEPRECATION")
        return listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots"),
            File(root, "Screenshots")
        ).map { it.absolutePath.trimEnd('/', '\\') + File.separator }
    }

    private fun claimScreenshotUri(uriString: String): Boolean = synchronized(this) {
        if (processedScreenshotUris.contains(uriString)
            || uriString == lastProcessedImageUri
            || screenshotInFlightUris.contains(uriString)
        ) {
            false
        } else {
            screenshotInFlightUris.add(uriString)
            true
        }
    }

    private fun completeScreenshotUri(uriString: String, dateAdded: Long, processed: Boolean) {
        val processedSnapshot = synchronized(this) {
            if (processed) rememberProcessedScreenshotUriLocked(uriString)
            screenshotInFlightUris.remove(uriString)
            if (processed) processedScreenshotUris.toList() else null
        }
        if (processedSnapshot != null) {
            persistProcessedScreenshotUris(processedSnapshot, uriString, dateAdded)
        }
    }

    private fun restoreProcessedScreenshotUris() {
        synchronized(this) {
            if (processedScreenshotUris.isNotEmpty()) return

            val persistedUris = latinIME.prefs()
                .getString(PREF_PROCESSED_SCREENSHOT_MEDIA_URIS, null)
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toList()
                .orEmpty()
            val migratedUri = latinIME.prefs().getString(PREF_LAST_SCREENSHOT_MEDIA_URI, null)
                ?.takeIf { it.isNotBlank() && it !in persistedUris }

            (persistedUris + listOfNotNull(migratedUri))
                .takeLast(MAX_PROCESSED_SCREENSHOT_URIS)
                .forEach { processedScreenshotUris.addLast(it) }
            lastProcessedImageUri = processedScreenshotUris.lastOrNull()
        }
    }

    private fun rememberProcessedScreenshotUriLocked(uriString: String) {
        processedScreenshotUris.remove(uriString)
        processedScreenshotUris.addLast(uriString)
        while (processedScreenshotUris.size > MAX_PROCESSED_SCREENSHOT_URIS) {
            processedScreenshotUris.removeFirst()
        }
        lastProcessedImageUri = uriString
    }

    private fun persistProcessedScreenshotUris(
        processedUris: List<String>,
        latestUri: String,
        dateAdded: Long
    ) {
        latinIME.prefs().edit {
            putString(PREF_PROCESSED_SCREENSHOT_MEDIA_URIS, processedUris.joinToString("\n"))
            putString(PREF_LAST_SCREENSHOT_MEDIA_URI, latestUri)
            putLong(PREF_LAST_SCREENSHOT_DATE_ADDED, dateAdded)
        }
    }

    override fun onPrimaryClipChanged() {
        releaseClipboardAccessory()
        dismissExtraction()
        if (isHistoryPrivate()) {
            suppressPrivateClipboardSuggestions()
            return // Do not even inspect the OS clipboard or ownership receipt in private mode.
        }
        dontShowCurrentSuggestion = false
        val owned = ownedPrimaryClip
        if (owned != null && currentClipReceipt() != owned) ownedPrimaryClip = null
        // Make sure we read clipboard history content only if history settings is set.
        if (latinIME.mSettings.current.mClipboardHistoryEnabled) {
            fetchPrimaryClip(clipChanged = true)
        }
        refreshClipboardSuggestion()
    }

    fun updatePrimaryClip() {
        if (!latinIME.mSettings.current.mClipboardHistoryEnabled) clearRecoveredTranscripts()
        if (isHistoryPrivate()) {
            // An absent editor is a normal hide/reopen; an actual protected
            // editor or lock is a privacy boundary and erases voice recovery.
            if (latinIME.currentInputEditorInfo != null || isDeviceLocked(latinIME))
                clearRecoveredTranscripts()
            suppressPrivateClipboardSuggestions()
            syncScreenshotObserver()
            return
        }
        syncScreenshotObserver()
        if (shouldObserveScreenshots()) {
            checkForNewScreenshot()
        }
        if (latinIME.mSettings.current.mClipboardHistoryEnabled) {
            fetchPrimaryClip()
        }
    }

    /** Explicit mode changes revoke both visual state and the previous Chrome receipt. */
    fun onClipboardPrivacyBoundary() = suppressPrivateClipboardSuggestions()

    private fun suppressPrivateClipboardSuggestions() {
        SmartPasteLearning.pause()
        invalidateChrome()
        releaseClipboardAccessory()
        clearDecryptedHistory()
        ownedPrimaryClip = null
        latestImageSuggestion = null
        latestScreenshotSuggestion = null
        removeClipboardSuggestion(recordDismissal = false)
    }

    private fun fetchPrimaryClip(clipChanged: Boolean = false) {
        try {
            if (isHistoryPrivate()) return
            val clipData = clipboardManager.primaryClip ?: run {
                if (clipChanged) latestImageSuggestion = null
                return
            }
            if (!ClipboardPrivacyPolicy.mayCollect(
                    latinIME.currentInputEditorInfo,
                    latinIME.mSettings.current.mIncognitoModeEnabled,
                    ClipboardManagerCompat.getClipSensitivity(clipData.description) == true)) {
                latestImageSuggestion = null
                return
            }
            if (clipData.itemCount == 0) {
                if (clipChanged) latestImageSuggestion = null
                return
            }
            val timeStamp = ClipboardManagerCompat.getClipTimestamp(clipData)
            if (latinIME.mSettings.current.mShowScreenshotsInClipboard) {
                recentImageClip(clipData)?.let { imageClip ->
                    val cachedUri = cacheImageClip(imageClip) ?: return
                    clipboardDao?.addClip(timeStamp, false, encodeImageHistoryClip(cachedUri, imageClip.mimeType, imageClip.label))
                    rememberImageSuggestion(imageClip.copy(uri = cachedUri))
                    return
                }
            }
            if (shouldReplaceLatestImageSuggestion(timeStamp, clipChanged)) {
                latestImageSuggestion = null
            }
            val content = ClipboardPrivacyPolicy.boundedPlainText(clipData) ?: return
            if (ClipboardEntityExtractor.excludeFromAutomaticHistory(content) || isHistoryPrivate()) return
            clipboardDao?.addClip(timeStamp, false, content)
        } catch (e: Exception) {
            Log.w("ClipboardHistoryManager", "Failed to fetch primary clip safely (${e.javaClass.simpleName})")
        }
    }

    private fun refreshClipboardSuggestion() {
        latinIME.mHandler.post {
            if (latinIME.isInputViewShown && latinIME.hasSuggestionStripView()) {
                latinIME.setNeutralSuggestionStrip()
            }
        }
    }

    fun toggleClipPinned(id: Long) {
        clipboardDao?.togglePinned(id)
    }

    fun clearHistory() {
        clipboardDao?.clearNonPinned()
        clearRecoveredTranscripts()
        // User explicitly cleared *brrBoard history*, not some other app's
        // clipboard. Clear the system clip only for a still-matching copy made
        // through brrBoard in this process. Never use label-only provenance.
        val owned = ownedPrimaryClip
        if (!isHistoryPrivate() && ClipboardPasteSafety.mayClearOwned(true,
                owned != null && owned == currentClipReceipt())) {
            ClipboardManagerCompat.clearPrimaryClip(clipboardManager)
        }
        ownedPrimaryClip = null
        removeClipboardSuggestion()
        AudioAndHapticFeedbackManager.getInstance().performImportantActionConfirmation()
    }

    /** Only called after brrBoard itself successfully sets the system clipboard. */
    fun rememberOwnedCopy() {
        ownedPrimaryClip = if (isHistoryPrivate()) null else currentClipReceipt()
    }

    private fun currentClipReceipt(requireReliableTimestamp: Boolean = true): OwnedClip? {
        return try {
            val clip = clipboardManager.primaryClip ?: return null
            clipReceipt(clip, requireReliableTimestamp)
        } catch (_: Exception) {
            null // Clipboard access can be denied when IME loses foreground.
        }
    }

    private fun clipReceipt(clip: android.content.ClipData,
                            requireReliableTimestamp: Boolean): OwnedClip? {
        if (clip.itemCount == 0 || clip.description?.hasMimeType("text/plain") != true) return null
        // No OS timestamp is never acceptable for deleting a clipboard owned
        // by another app. For user-confirmed Paste, an exact text digest is
        // sufficient on older Android versions; use stable zero as sentinel.
        val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            clip.description?.timestamp ?: 0L else 0L
        if (requireReliableTimestamp && timestamp <= 0L) return null
        val value = clip.getItemAt(0).text?.toString() ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return OwnedClip(digest, timestamp.coerceAtLeast(0L))
    }

    /** Explicit extraction from the current OS plain-text clip; no URI dereference or history write. */
    private fun extractionText(clip: android.content.ClipData): String? {
        return ClipboardPrivacyPolicy.boundedPlainText(clip)
    }

    private data class ExtractOption(val label: String, val start: Int, val end: Int)

    private fun extractOptions(text: String, contextual: Boolean): List<ExtractOption> {
        if (!contextual) return ClipboardEntityExtractor.extract(text).map { ExtractOption(it.kind.label, it.start, it.end) }
        if (ClipboardEntityExtractor.excludeFromAutomaticHistory(text)) return emptyList()
        return ContextualAccessoryPolicy.extractCandidates(text)
            .map { ExtractOption(it.kind.name.lowercase(java.util.Locale.ROOT), it.start, it.end) }
    }

    fun requestExtractCurrentClip() = requestExtractCurrentClip(false)

    private fun requestExtractCurrentClip(contextual: Boolean) {
        dismissExtraction()
        val values = latinIME.mSettings.current
        if (isHistoryPrivate() || !latinIME.isInputViewShown ||
            !latinIME.mayUseTransientCorrectionMemory(values)) return
        val editor = latinIME.currentInputEditorInfo ?: return
        val ic = latinIME.currentInputConnection ?: return
        val epoch = latinIME.editorGeneration()
        val selection = latinIME.mInputLogic.mConnection
        val start = selection.expectedSelectionStart
        val end = selection.expectedSelectionEnd
        if (start < 0 || end < start) return
        val clip = try { clipboardManager.primaryClip } catch (_: Exception) { null } ?: return
        val source = extractionText(clip) ?: return
        val receipt = clipReceipt(clip, requireReliableTimestamp = false) ?: return
        val entities = extractOptions(source, contextual)
        if (entities.isEmpty()) {
            latinIME.mKeyboardSwitcher.showToast("No supported extractable items", false)
            return
        }
        // Keep only kind/ranges, a digest receipt and masked labels in the dialog.
        val labels = entities.mapIndexed { i, entity -> "${i + 1}. ${entity.label} •••• (paste)" }.toTypedArray()
        val created = android.os.SystemClock.elapsedRealtime()
        try {
            val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(latinIME))
                .setTitle("Extract from current clipboard")
                .setItems(labels) { _, index ->
                    val age = android.os.SystemClock.elapsedRealtime() - created
                    if (age !in 0L until 30_000L || isHistoryPrivate() || !latinIME.isInputViewShown ||
                        epoch != latinIME.editorGeneration() || ic !== latinIME.currentInputConnection ||
                        editor !== latinIME.currentInputEditorInfo ||
                        selection.expectedSelectionStart != start || selection.expectedSelectionEnd != end ||
                        !latinIME.mayUseTransientCorrectionMemory(latinIME.mSettings.current)) return@setItems
                    val current = try { clipboardManager.primaryClip } catch (_: Exception) { null } ?: return@setItems
                    val liveReceipt = try { clipReceipt(current, requireReliableTimestamp = false) } catch (_: Exception) { null }
                    if (liveReceipt != receipt) return@setItems
                    val text = extractionText(current) ?: return@setItems
                    val entity = entities[index]
                    if (entity !in extractOptions(text, contextual)) return@setItems
                    latinIME.insertExtractedClipboardText(text.substring(entity.start, entity.end), epoch, ic)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            val window = dialog.window ?: return
            val token = latinIME.window?.window?.attributes?.token ?: return
            window.attributes = window.attributes.apply {
                this.token = token
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_SECURE)
            dialog.setOnDismissListener {
                if (extractionDialog === dialog) {
                    extractionDialog = null
                    latinIME.mHandler.removeCallbacks(expireExtraction)
                }
            }
            extractionDialog = dialog
            dialog.show()
            latinIME.mHandler.postDelayed(expireExtraction, 30_000)
        } catch (_: Exception) { dismissExtraction() }
    }

    /** Explicit Paste key. Private/sensitive text is never inserted before confirmation. */
    fun requestPrimaryPaste() {
        if (isDeviceLocked(latinIME)) return
        val editor = latinIME.currentInputEditorInfo
        val connection = latinIME.currentInputConnection
        // Android may deny clipboard reads as focus changes; a denial is a no-op.
        val clip = try { clipboardManager.primaryClip } catch (_: Exception) { null } ?: return
        val sourceText = try {
            if (clip.itemCount > 0 && clip.description?.hasMimeType("text/*") == true)
                clip.getItemAt(0).text?.toString() else null
        } catch (_: Exception) { null }
        val privateEditor = ClipboardPrivacyPolicy.isPrivateEditor(
            editor, latinIME.mSettings.current.mIncognitoModeEnabled)
        val action = ClipboardPasteSafety.action(!sourceText.isNullOrEmpty(),
            editor != null && connection != null, privateEditor,
            ClipboardManagerCompat.getClipSensitivity(clip.description) == true)
        when (action) {
            ClipboardPasteSafety.Action.REJECT -> return
            ClipboardPasteSafety.Action.INSERT -> {
                latinIME.onTextInput(sourceText!!)
                return
            }
            ClipboardPasteSafety.Action.CONFIRM -> Unit
        }

        // Capture only identity/epoch, never the clipboard's plaintext, in a
        // deferred callback. If either editor or clipboard changes, fail closed.
        val expectedReceipt = currentClipReceipt(requireReliableTimestamp = false) ?: return
        val expectedEpoch = latinIME.editorGeneration()
        try {
            val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(latinIME))
                .setTitle(R.string.board_private_paste_title)
                .setMessage(R.string.board_private_paste_explanation)
                .setNegativeButton(R.string.board_private_paste_cancel, null)
                .setPositiveButton(R.string.board_private_paste_confirm) { _, _ ->
                    if (isDeviceLocked(latinIME) || latinIME.editorGeneration() != expectedEpoch ||
                        latinIME.currentInputConnection !== connection ||
                        latinIME.currentInputEditorInfo !== editor) return@setPositiveButton
                    // Read one OS ClipData snapshot. Comparing and inserting from
                    // the same object closes a replacement race between two reads.
                    val confirmedClip = try { clipboardManager.primaryClip }
                        catch (_: Exception) { null } ?: return@setPositiveButton
                    val confirmedReceipt = try {
                        clipReceipt(confirmedClip, requireReliableTimestamp = false)
                    }
                        catch (_: Exception) { null }
                    if (confirmedReceipt != expectedReceipt) return@setPositiveButton
                    val confirmedText = try { confirmedClip.getItemAt(0).text?.toString() }
                        catch (_: Exception) { null }
                    if (!confirmedText.isNullOrEmpty()) latinIME.onTextInput(confirmedText)
                }
                .create()
            val window = dialog.window ?: return
            val token = latinIME.window?.window?.attributes?.token ?: return
            window.attributes = window.attributes.apply {
                this.token = token
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.show()
        } catch (_: Exception) {
            latinIME.mKeyboardSwitcher.showToast(
                latinIME.getString(R.string.board_private_paste_unavailable), true)
        }
    }

    fun canRemove(index: Int) = clipboardDao?.isPinned(index) == false

    fun removeEntry(index: Int): ClipboardHistoryEntry? {
        return if (canRemove(index))
            clipboardDao?.deleteClipAt(index)
        else null
    }

    fun restoreEntry(entry: ClipboardHistoryEntry, index: Int): Int {
        return clipboardDao?.restoreClip(entry, index) ?: -1
    }

    fun sortHistoryEntries() {
        clipboardDao?.sort()
    }

    // We do not want to update history while user is visualizing it, so we check retention only
    // when history is about to be shown
    fun prepareClipboardHistory() = clipboardDao?.clearOldClips(true)

    /** Only the actual final recognition dispatch may populate recovery. */
    fun rememberFinalVoiceTranscript(text: String) {
        if (mayAccessVoiceRecovery()) recoveryRepository.add(text)
    }

    private fun mayAccessVoiceRecovery(): Boolean =
        latinIME.mSettings.current.mClipboardHistoryEnabled &&
            latinIME.mayUseTransientCorrectionMemory(latinIME.mSettings.current)

    fun getRecoveredTranscripts(): List<RecoveredTranscript> =
        if (mayAccessVoiceRecovery()) recoveryRepository.all() else emptyList()

    fun getRecoveredTranscript(id: Long): RecoveredTranscript? =
        if (mayAccessVoiceRecovery()) recoveryRepository.get(id) else null

    /** Explicit dismissal only. Paste does not consume or extend the fixed lifetime. */
    fun consumeRecoveredTranscript(id: Long): Boolean = recoveryRepository.consume(id)

    fun clearRecoveredTranscripts() = recoveryRepository.clear()

    fun searchHistoryIds(query: String): List<Long> = if(isHistoryPrivate()) emptyList() else clipboardDao?.searchIds(query).orEmpty()
    fun hasUnpinnedHistory() = !isHistoryPrivate() && clipboardDao?.hasUnpinned()==true

    fun getHistorySize() = if (isHistoryPrivate()) 0 else clipboardDao?.count() ?: 0

    fun getHistoryEntry(position: Int) = if (isHistoryPrivate()) null else clipboardDao?.getAt(position)

    fun getHistoryEntryContent(id: Long) = if (isHistoryPrivate()) null else clipboardDao?.get(id)

    private fun isHistoryPrivate(): Boolean = isDeviceLocked(latinIME) ||
        latinIME.mSettings.current.mInputAttributes.mNoLearning ||
        latinIME.mSettings.current.mInputAttributes.mVaultPrivateEditor ||
        ClipboardPrivacyPolicy.isPrivateEditor(
            latinIME.currentInputEditorInfo, latinIME.mSettings.current.mIncognitoModeEnabled)

    fun clearDecryptedHistory() { cachedClipboardDao?.clearDecryptedHistory() }

    fun setHistoryChangeListener(listener: ClipboardDao.Listener?) {
        clipboardDao?.listener = listener
        if(listener==null) clearDecryptedHistory()
    }

    private sealed class RecentClip(open val timeStamp: Long) {
        data class Text(val content: CharSequence, override val timeStamp: Long) : RecentClip(timeStamp)
        data class Image(
            val uri: Uri,
            val mimeType: String,
            val label: String,
            override val timeStamp: Long
        ) : RecentClip(timeStamp)
    }

    private fun isClipDismissed(clip: RecentClip): Boolean {
        return dismissedClipTimestamp != 0L && clip.timeStamp <= dismissedClipTimestamp &&
            recentClipIdentity(clip) == dismissedClipIdentity
    }

    private fun markClipDismissed(clip: RecentClip) {
        dontShowCurrentSuggestion = true
        dismissedClipTimestamp = clip.timeStamp
        dismissedClipIdentity = recentClipIdentity(clip)
    }

    private fun recentClipIdentity(clip: RecentClip): String {
        // Fixed-size fingerprint stays in memory; never persist copied content.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update((if (clip is RecentClip.Text) "text:" else "image:").toByteArray(Charsets.UTF_8))
        val content = when (clip) {
            is RecentClip.Text -> clip.content.toString()
            is RecentClip.Image -> clip.uri.toString()
        }
        return digest.digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun ClipDescription.imageMimeType(): String? {
        for (i in 0 until mimeTypeCount) {
            val mimeType = getMimeType(i)
            if (ClipDescription.compareMimeTypes(mimeType, "image/*")) return mimeType
        }
        return null
    }

    private fun android.content.ClipData.imageUri(): Uri? {
        for (i in 0 until itemCount) {
            val item = getItemAt(i) ?: continue
            item.uri?.let { return it }
            item.intent?.data?.let { return it }
        }
        return null
    }

    private fun recentImageClip(clipData: android.content.ClipData): RecentClip.Image? {
        if (clipData.itemCount == 0) return null
        val uri = clipData.imageUri() ?: return null
        val description = clipData.description
        val descriptionMimeType = description?.imageMimeType()
            ?.takeUnless { it == "image/*" }
        val resolverMimeType = latinIME.contentResolver.getType(uri)
            ?.takeIf { ClipDescription.compareMimeTypes(it, "image/*") }
        val mimeType = descriptionMimeType ?: resolverMimeType ?: description?.imageMimeType()
            ?: return null
        val labelText = description?.label?.toString().orEmpty()
        val uriText = uri.toString()
        val label = if (
            labelText.contains("screenshot", ignoreCase = true)
            || uriText.contains("screenshot", ignoreCase = true)
        ) {
            "Screenshot"
        } else {
            "Image"
        }
        return RecentClip.Image(uri, normalizeImageMimeType(mimeType).mimeType, label, ClipboardManagerCompat.getClipTimestamp(clipData))
    }

    private fun rememberImageSuggestion(clip: RecentClip.Image) {
        latestImageSuggestion = clip
        dontShowCurrentSuggestion = false
    }

    private fun shouldReplaceLatestImageSuggestion(timeStamp: Long, clipChanged: Boolean): Boolean {
        val imageClip = latestImageSuggestion ?: return true
        return clipChanged || timeStamp >= imageClip.timeStamp
    }

    private fun recentTextClip(editorInfo: EditorInfo?): RecentClip.Text? {
        if (isHistoryPrivate()) return null
        val editor = editorInfo ?: return null
        val dest = SmartPastePolicy.destination(editor)
        if (dest == SmartPastePolicy.Destination.UNSUPPORTED) return null
        val clip = runCatching { clipboardManager.primaryClip }.getOrNull() ?: return null
        val content = ClipboardPrivacyPolicy.boundedPlainText(clip) ?: return null
        // A fallback read timestamp would let an ancient OTP or copy appear newly fresh.
        val timeStamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) clip.description.timestamp else 0L
        val age = System.currentTimeMillis() - timeStamp
        if (timeStamp <= 0L || age !in 0L until RECENT_TIME_MILLIS) return null
        if (dest == SmartPastePolicy.Destination.OTP) {
            if (timeStamp <= otpBlockedThrough || age >= 60_000L ||
                SmartPastePolicy.copiedOtp(content) == null) return null
        } else if (ClipboardEntityExtractor.excludeFromAutomaticHistory(content)) return null
        return RecentClip.Text(content, timeStamp).takeUnless { isClipDismissed(it) }
    }

    private data class RichPaste(
        val uri: Uri, val mimeType: String, val timestamp: Long,
        val isScreenshot: Boolean = false,
    )

    /** Suggested only for locally accessible screenshots, only in image-capable editors. */
    private fun recentScreenshotPaste(editor: EditorInfo?): RichPaste? {
        if (editor == null || !shouldObserveScreenshots()) return null
        val inputType = editor.inputType
        if ((inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return null
        val recent = latestScreenshotSuggestion ?: return null
        if (!ScreenshotSuggestionPolicy.isRecent(recent.timeStamp, System.currentTimeMillis())) return null
        if (recent.uri.scheme != "content") return null
        val supportedTypes = androidx.core.view.inputmethod.EditorInfoCompat.getContentMimeTypes(editor)
        if (supportedTypes.none { ClipDescription.compareMimeTypes(recent.mimeType, it) }) return null
        return RichPaste(recent.uri, recent.mimeType, recent.timeStamp, isScreenshot = true)
    }

    private fun recentRichPaste(editor: EditorInfo?): RichPaste? {
        editor ?: return null
        val type = editor.inputType
        if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT ||
            ClipboardPrivacyPolicy.isPrivateEditor(editor, latinIME.mSettings.current.mIncognitoModeEnabled)) return null
        val supported = androidx.core.view.inputmethod.EditorInfoCompat.getContentMimeTypes(editor)
        if (supported.isEmpty()) return null
        val clip = clipboardManager.primaryClip ?: return null
        if (ClipboardManagerCompat.getClipSensitivity(clip.description) == true) return null
        val timestamp = ClipboardManagerCompat.getClipTimestamp(clip)
        if (System.currentTimeMillis() - timestamp >= RECENT_TIME_MILLIS) return null
        val uri = clip.imageUri()?.takeIf { it.scheme == "content" } ?: return null
        val mime = runCatching { latinIME.contentResolver.getType(uri) }.getOrNull() ?: return null
        if (supported.none { ClipDescription.compareMimeTypes(mime, it) }) return null
        return RichPaste(uri, mime, timestamp)
    }

    fun getClipboardSuggestionView(editorInfo: EditorInfo?, parent: ViewGroup?): View? {
        if (chromePrivate() || !latinIME.mSettings.current.mSuggestClipboardContent ||
            dontShowCurrentSuggestion || parent == null) {
            releaseClipboardAccessory()
            return null
        }
        val dest = SmartPastePolicy.destination(editorInfo)
        val screenshot = if (dest == SmartPastePolicy.Destination.GENERAL) recentScreenshotPaste(editorInfo) else null
        val recentText = recentTextClip(editorInfo)
        val classification = recentText?.let { text ->
            classifiedCopy?.takeIf { it.clip == text } ?: ClassifiedCopy(text,
                if (dest == SmartPastePolicy.Destination.OTP)
                    listOfNotNull(SmartPastePolicy.copiedOtp(text.content.toString()))
                else SmartPastePolicy.classify(text.content.toString()))
        }
        val candidateDecision = classification?.let { snapshot ->
            val age = System.currentTimeMillis() - snapshot.clip.timeStamp
            val kind = SmartPastePolicy.preferredCandidate(snapshot.candidates,dest)?.kind ?: SmartPastePolicy.Kind.TEXT
            val bias = if (learningAllowed() && dest != SmartPastePolicy.Destination.OTP) {
                SmartPasteLearning.initialize(latinIME)
                SmartPasteLearning.bias(kind,dest)
            } else 0
            SmartPastePolicy.decide(snapshot.candidates, dest, age, latinIME.cachedSmartPasteSuffix(),
                latinIME.smartPasteIsComposing(), bias,
                age < CleanLinkPolicy.WINDOW_MS && CleanLinkPolicy.eligible(snapshot.clip.content.toString()))
        }
        // Never displace a newer eligible text item with an older screenshot.
        val clip = recentText?.takeIf { candidateDecision != null &&
            (screenshot == null || screenshot.timestamp < it.timeStamp) }
        val decision = candidateDecision?.takeIf { clip != null }
        val rich = if (clip == null && dest == SmartPastePolicy.Destination.GENERAL)
            screenshot ?: recentRichPaste(editorInfo) else null
        if (clip == null && rich == null) {
            releaseClipboardAccessory()
            return null
        }
        val connection = latinIME.currentInputConnection ?: return null
        val editorEpoch = latinIME.editorGeneration()
        val key = AccessoryKey(clip, rich, editorEpoch, connection, decision?.candidate)
        val existing = clipboardSuggestionView
        if (existing != null && !existing.retired && accessoryKey == key &&
            accessoryKey?.connection === connection) {
            if (SystemClock.elapsedRealtime() >= pasteDeadline) {
                releaseClipboardAccessory()
                return null
            }
            existing.prefersProminent = decision?.prominent ?: true
            return existing
        }
        releaseClipboardAccessory()
        classifiedCopy = classification?.takeIf { clip != null }
        val keyboard = latinIME.mKeyboardSwitcher.keyboard ?: return null
        val binding = ClipboardSuggestionBinding.inflate(LayoutInflater.from(latinIME), parent, false)
        val textView = binding.clipboardSuggestionText
        val isImageChip = rich?.mimeType?.startsWith("image/") == true
        binding.clipboardSuggestionPreview.isGone = !isImageChip
        if (isImageChip) {
            // Recent rich-content paste already requires a supported editor MIME
            // and a content URI. Coil owns/cancels this view's asynchronous load.
            binding.clipboardSuggestionPreview.apply {
                contentDescription = latinIME.getString(R.string.clipboard_image_preview)
                load(rich!!.uri) {
                    crossfade(false)
                    // This chip is ephemeral, not pinned Clipboard history.
                    // Never persist an extra preview of another app's content.
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }
        }
        val clipIcon = keyboard.mIconsSet.getIconDrawable(ToolbarKey.PASTE.name.lowercase())?.let {
            it.constantState?.newDrawable(latinIME.resources)?.mutate() ?: it.mutate()
        }
        val chosen = decision?.candidate
        val insertion = clip?.let { text ->
            chosen?.let { text.content.subSequence(it.start, it.end).toString() } ?: text.content.toString()
        }
        val isOtp = chosen?.kind == SmartPastePolicy.Kind.OTP
        val label = insertion?.let {
            if (isOtp) latinIME.getString(R.string.board_paste_verification_code)
            else helium314.keyboard.latin.suggestions.ClipboardPreviewPolicy.label(it)
        } ?: latinIME.getString(
            if (rich?.isScreenshot == true) R.string.board_paste_from_screenshots
            else if (rich?.mimeType?.startsWith("image/") == true) R.string.board_paste_image
            else R.string.board_paste_file,
        )
        val colors = latinIME.mSettings.current.mColors
        val root = binding.root
        root.configure(colors, label, isImageChip)
        root.prefersProminent = decision?.prominent ?: true
        root.isVerificationCode = isOtp
        clipIcon?.let {
            val size = (18 * latinIME.resources.displayMetrics.density).toInt()
            it.setBounds(0, 0, size, size)
            colors.setColor(it, ColorType.SUGGESTION_AUTO_CORRECT)
        }
        textView.setCompoundDrawablesRelative(if (isImageChip) null else clipIcon, null, null, null)
        binding.clipboardSuggestionPaste.setOnClickListener {
            if (clipboardSuggestionView !== root || root.retired || !root.isShown ||
                !latinIME.isInputViewShown || isHistoryPrivate() || SystemClock.elapsedRealtime() >= pasteDeadline ||
                latinIME.editorGeneration() != editorEpoch ||
                ClipboardPrivacyPolicy.isPrivateEditor(latinIME.currentInputEditorInfo,
                    latinIME.mSettings.current.mIncognitoModeEnabled) ||
                latinIME.currentInputConnection !== connection ||
                (if (clip != null) currentTextSuggestionChanged(clip)
                else (if (rich?.isScreenshot == true) recentScreenshotPaste(latinIME.currentInputEditorInfo)
                      else recentRichPaste(latinIME.currentInputEditorInfo)) != rich)) {
                if (clipboardSuggestionView === root) releaseClipboardAccessory()
                else root.retire()
                return@setOnClickListener
            }
            if (clip != null) {
                if (isOtp) {
                    if (!latinIME.insertCopiedVerificationCode(insertion ?: return@setOnClickListener, editorEpoch, connection)) return@setOnClickListener
                    otpBlockedThrough = maxOf(otpBlockedThrough, clip.timeStamp)
                    dontShowCurrentSuggestion = true
                    releaseClipboardAccessory()
                } else if (decision != null && decision.destination != SmartPastePolicy.Destination.GENERAL &&
                    decision.destination != SmartPastePolicy.Destination.SEARCH) {
                    if (!latinIME.insertSmartPasteEntity(insertion ?: return@setOnClickListener, editorEpoch, connection)) return@setOnClickListener
                    recordPasteDecision(true)
                    markClipDismissed(clip)
                    releaseClipboardAccessory()
                } else {
                    recordPasteDecision(true)
                    markClipDismissed(clip)
                    releaseClipboardAccessory()
                    latinIME.onTextInput(clip.content.toString())
                }
            } else if (rich != null) {
                if (!latinIME.commitKlipyContent(rich.uri, label.toString(), rich.mimeType)) return@setOnClickListener
                dontShowCurrentSuggestion = true
                releaseClipboardAccessory()
            }
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                KeyCode.NOT_SPECIFIED, it, HapticEvent.KEY_PRESS,
            )
            latinIME.mHandler.postResumeSuggestions(false)
        }

        val chrome = binding.clipboardSuggestionChrome
        chrome.isGone = true
        if (clip != null && !isOtp && !chromePrivate() && CleanLinkPolicy.eligible(clip.content.toString())) {
            val receipt = strictTextReceipt()
            val remaining = if (receipt != null && receipt.timestamp == clip.timeStamp)
                chromeRemaining(receipt.timestamp) else 0L
            if (receipt != null && remaining > 0L) {
                val pm = latinIME.packageManager
                val chromeIcon = runCatching {
                    val app = pm.getApplicationInfo("com.android.chrome", 0)
                    // The launcher icon may include a white adaptive plate. Use the
                    // independent inline brand glyph only when Chrome really exists.
                    if (app.enabled) ContextCompat.getDrawable(latinIME, R.drawable.ic_board_chrome_inline)?.mutate() else null
                }.getOrNull()
                if (chromeIcon != null) {
                    val size = (22 * latinIME.resources.displayMetrics.density).toInt()
                    chromeIcon.setBounds(0, 0, size, size)
                    chrome.setCompoundDrawablesRelative(chromeIcon, null, null, null)
                    chrome.isGone = false
                    // A secondary explicit gesture copies the cleaned URL rather than opening it.
                    val copyClean: () -> Boolean = {
                        if (!chrome.isShown || !chrome.isEnabled || clipboardSuggestionView !== root || root.retired ||
                            !latinIME.isInputViewShown || chromePrivate() || latinIME.editorGeneration() != editorEpoch ||
                            latinIME.currentInputConnection !== connection || chromeRemaining(receipt.timestamp) <= 0L ||
                            strictTextReceipt() != receipt || currentTextSuggestionChanged(clip)) false
                        else {
                            val clean = CleanLinkPolicy.cleanForNavigation(clip.content.toString())
                            if (clean == null) false else {
                                try {
                                    if (clean != clip.content.toString()) {
                                        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("brrBoard", clean))
                                        rememberOwnedCopy()
                                    }
                                    chromeBlockedThrough = maxOf(chromeBlockedThrough, receipt.timestamp)
                                    cancelChromeExpiry()
                                    root.hideChrome()
                                    true
                                } catch (_: RuntimeException) { false }
                            }
                        }
                    }
                    chrome.setOnLongClickListener { copyClean() }
                    androidx.core.view.ViewCompat.addAccessibilityAction(chrome,
                        latinIME.getString(R.string.board_context_copy_clean)) { _, _ -> copyClean() }
                    chrome.setOnClickListener {
                        if (!chrome.isShown || clipboardSuggestionView !== binding.root ||
                            !latinIME.isInputViewShown || chromePrivate() ||
                            latinIME.editorGeneration() != editorEpoch ||
                            latinIME.currentInputConnection !== connection ||
                            chromeRemaining(receipt.timestamp) <= 0L ||
                            strictTextReceipt() != receipt || currentTextSuggestionChanged(clip)) {
                            root.hideChrome()
                            return@setOnClickListener
                        }
                        // Only this explicit tap runs the sanitizer. Never replace the clipboard.
                        val clean = CleanLinkPolicy.cleanForNavigation(clip.content.toString())
                        chromeBlockedThrough = maxOf(chromeBlockedThrough, receipt.timestamp)
                        chromeDeadline = 0L
                        cancelChromeExpiry()
                        root.hideChrome()
                        if (clean == null) return@setOnClickListener
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean))
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setPackage("com.android.chrome")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            latinIME.startActivity(intent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(latinIME, R.string.board_chrome_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                        } catch (_: SecurityException) {
                            android.widget.Toast.makeText(latinIME, R.string.board_chrome_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    val expire = Runnable {
                        if (clipboardSuggestionView === root) root.hideChrome()
                    }
                    chromeExpiry = expire
                    latinIME.mHandler.postDelayed(expire, remaining)
                }
            }
        }

        // Reuse the existing one-slot animation and strict copy receipt for other actions.
        if (chrome.isGone && clip != null && !isOtp && !chromePrivate()) {
            val receipt = strictTextReceipt()
            val remaining = if (receipt != null && receipt.timestamp == clip.timeStamp)
                chromeRemaining(receipt.timestamp) else 0L
            // Anchor relative dates to the copy receipt; midnight must not change a revealed event.
            val action = if (receipt != null && remaining > 0L) {
                val zone = java.time.ZoneId.systemDefault()
                val today = java.time.Instant.ofEpochMilli(receipt.timestamp).atZone(zone).toLocalDate()
                ContextualAccessoryPolicy.choose(clip.content.toString(), dest, today, zone)
            } else null
            if (action != null && receipt != null && remaining > 0L) {
                val (iconId, labelId) = when (action.kind) {
                    ContextualAccessoryPolicy.Kind.DIAL -> R.drawable.ic_board_context_dial to R.string.board_context_dial
                    ContextualAccessoryPolicy.Kind.MAIL -> R.drawable.ic_board_context_mail to R.string.board_context_mail
                    ContextualAccessoryPolicy.Kind.MAP -> R.drawable.ic_board_context_map to R.string.board_context_map
                    ContextualAccessoryPolicy.Kind.CLEAN_TEXT -> R.drawable.ic_board_context_clean to R.string.board_context_clean
                    ContextualAccessoryPolicy.Kind.CASE -> R.drawable.ic_board_context_clean to R.string.board_context_case
                    ContextualAccessoryPolicy.Kind.CALENDAR -> R.drawable.ic_board_context_calendar to R.string.board_context_calendar
                    ContextualAccessoryPolicy.Kind.TRACK -> R.drawable.ic_board_context_track to R.string.board_context_track
                    ContextualAccessoryPolicy.Kind.EXTRACT -> R.drawable.ic_board_context_extract to R.string.board_context_extract
                }
                val intent = when (action.kind) {
                    ContextualAccessoryPolicy.Kind.DIAL -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", action.value, null))
                    ContextualAccessoryPolicy.Kind.MAIL -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", action.value, null))
                    ContextualAccessoryPolicy.Kind.MAP -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(action.value)))
                    ContextualAccessoryPolicy.Kind.CALENDAR -> {
                        val draft = requireNotNull(action.calendar)
                        Intent(Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
                            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, draft.beginMillis)
                            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, draft.endMillis)
                            .putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, draft.allDay)
                            .apply { draft.title?.let { putExtra(android.provider.CalendarContract.Events.TITLE, it) } }
                    }
                    ContextualAccessoryPolicy.Kind.TRACK -> Intent(Intent.ACTION_VIEW,
                        Uri.parse(action.value))
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                    ContextualAccessoryPolicy.Kind.CLEAN_TEXT, ContextualAccessoryPolicy.Kind.CASE,
                    ContextualAccessoryPolicy.Kind.EXTRACT -> null
                }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val available = intent == null || runCatching {
                    intent.resolveActivity(latinIME.packageManager) != null
                }.getOrDefault(false)
                val icon = ContextCompat.getDrawable(latinIME, iconId)?.mutate()
                if (available && icon != null) {
                    val size = (22 * latinIME.resources.displayMetrics.density).toInt()
                    icon.setBounds(0, 0, size, size)
                    colors.setColor(icon, ColorType.SUGGESTION_AUTO_CORRECT)
                    chrome.setCompoundDrawablesRelative(icon, null, null, null)
                    chrome.contentDescription = if (action.kind == ContextualAccessoryPolicy.Kind.TRACK && action.carrier != null)
                        latinIME.getString(R.string.board_context_track_carrier, action.carrier)
                    else latinIME.getString(labelId)
                    chrome.isGone = false
                    if (action.kind == ContextualAccessoryPolicy.Kind.DIAL || action.kind == ContextualAccessoryPolicy.Kind.MAIL) {
                        val contact = Intent(Intent.ACTION_INSERT, android.provider.ContactsContract.Contacts.CONTENT_URI)
                            .setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE)
                            .putExtra(if (action.kind == ContextualAccessoryPolicy.Kind.DIAL)
                                android.provider.ContactsContract.Intents.Insert.PHONE
                                else android.provider.ContactsContract.Intents.Insert.EMAIL, action.value)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (runCatching { contact.resolveActivity(latinIME.packageManager) != null }.getOrDefault(false)) {
                            val saveContact: () -> Boolean = {
                                if (!chrome.isShown || !chrome.isEnabled || clipboardSuggestionView !== root || root.retired ||
                                    !latinIME.isInputViewShown || chromePrivate() || latinIME.editorGeneration() != editorEpoch ||
                                    latinIME.currentInputConnection !== connection || chromeRemaining(receipt.timestamp) <= 0L ||
                                    strictTextReceipt() != receipt || currentTextSuggestionChanged(clip)) false
                                else {
                                    chromeBlockedThrough = maxOf(chromeBlockedThrough, receipt.timestamp)
                                    cancelChromeExpiry()
                                    root.hideChrome()
                                    runCatching { latinIME.startActivity(contact) }.isSuccess
                                }
                            }
                            chrome.setOnLongClickListener { saveContact() }
                            androidx.core.view.ViewCompat.addAccessibilityAction(chrome,
                                latinIME.getString(R.string.board_context_contact)) { _, _ -> saveContact() }
                        }
                    }
                    chrome.setOnClickListener {
                        if (!chrome.isShown || clipboardSuggestionView !== root || root.retired ||
                            !latinIME.isInputViewShown || chromePrivate() ||
                            latinIME.editorGeneration() != editorEpoch ||
                            latinIME.currentInputConnection !== connection ||
                            chromeRemaining(receipt.timestamp) <= 0L ||
                            strictTextReceipt() != receipt || currentTextSuggestionChanged(clip)) {
                            root.hideChrome()
                            return@setOnClickListener
                        }
                        chromeBlockedThrough = maxOf(chromeBlockedThrough, receipt.timestamp)
                        chromeDeadline = 0L
                        cancelChromeExpiry()
                        root.hideChrome()
                        if (intent != null) {
                            try {
                                latinIME.startActivity(intent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                android.widget.Toast.makeText(latinIME, R.string.board_context_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                            } catch (_: SecurityException) {
                                android.widget.Toast.makeText(latinIME, R.string.board_context_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else if (action.kind == ContextualAccessoryPolicy.Kind.EXTRACT) {
                            requestExtractCurrentClip(true)
                        } else if (latinIME.insertSmartPasteEntity(action.value, editorEpoch, connection)) {
                            markClipDismissed(clip)
                            releaseClipboardAccessory()
                            latinIME.mHandler.postResumeSuggestions(false)
                        }
                    }
                    val expire = Runnable { if (clipboardSuggestionView === root) root.hideChrome() }
                    chromeExpiry = expire
                    latinIME.mHandler.postDelayed(expire, remaining)
                }
            }
        }

        // One visible paste action, no redundant X. Long press and TalkBack's
        // explicit dismiss action share exactly the same receipt-aware behavior.
        if (chrome.isGone) root.hideChrome()
        val dismissPaste: () -> Boolean = {
            if (clipboardSuggestionView !== root || !root.isShown || root.retired) false
            else {
                if (isOtp && clip != null) {
                    otpBlockedThrough = maxOf(otpBlockedThrough, clip.timeStamp)
                    dontShowCurrentSuggestion = true
                } else {
                    recordPasteDecision(false)
                    if (clip != null) markClipDismissed(clip) else dontShowCurrentSuggestion = true
                }
                removeClipboardSuggestion()
                true
            }
        }
        binding.clipboardSuggestionPaste.setOnLongClickListener { dismissPaste() }
        androidx.core.view.ViewCompat.addAccessibilityAction(
            binding.clipboardSuggestionPaste,
            latinIME.getString(R.string.clipboard_dismiss_suggestion)
        ) { _, _ -> dismissPaste() }
        clipboardSuggestionView = root
        accessoryKey = key
        activeDecision = decision
        val age = System.currentTimeMillis() - (clip?.timeStamp ?: rich!!.timestamp)
        val lifetime = decision?.lifetime ?: if (rich?.isScreenshot == true)
            ScreenshotSuggestionPolicy.MAX_AGE_MILLIS else RECENT_TIME_MILLIS
        val remainingPaste = (lifetime - age).coerceIn(0L, lifetime)
        pasteDeadline = SystemClock.elapsedRealtime() + remainingPaste
        val expirePaste = Runnable {
            if (clipboardSuggestionView === root) {
                if (isOtp && clip != null) otpBlockedThrough = maxOf(otpBlockedThrough, clip.timeStamp)
                releaseClipboardAccessory()
                latinIME.mHandler.postResumeSuggestions(false)
            }
        }
        pasteExpiry = expirePaste
        latinIME.mHandler.postDelayed(expirePaste, remainingPaste)
        return root
    }

    private fun cacheImageClip(clip: RecentClip.Image): Uri? {
        val uriString = clip.uri.toString()
        if (inaccessibleImageClipUris.contains(uriString)) return null

        resolveOwnClipboardCacheFile(latinIME, clip.uri)?.let { cachedFile ->
            return if (ClipboardImageVault.ensureEncrypted(latinIME,cachedFile)) clip.uri else null
        }

        val mimeInfo = normalizeImageMimeType(clip.mimeType)
        val clipboardDir = File(latinIME.cacheDir, "clipboard").apply { mkdirs() }
        val imageFile = File(clipboardDir, cacheFileNameForSource(clip.uri, clip.timeStamp, mimeInfo.mimeType))
        val ready = if (ClipboardImageVault.hasStored(imageFile)) ClipboardImageVault.ensureEncrypted(latinIME,imageFile)
            else ClipboardImageVault.cache(latinIME,clip.uri,imageFile)
        if (!ready) {
            if (!isDeviceLocked(latinIME)) {
                if (inaccessibleImageClipUris.size > 50) inaccessibleImageClipUris.clear()
                inaccessibleImageClipUris.add(uriString)
            }
            return null
        }
        return FileProvider.getUriForFile(latinIME, "${latinIME.packageName}.fileprovider", imageFile)
    }

    fun pasteHistoryEntry(entry: ClipboardHistoryEntry): ClipboardHistoryPasteOutcome {
        if (isHistoryPrivate()) return ClipboardHistoryPasteOutcome.REJECTED
        val outcome = ClipboardHistoryPastePolicy.paste(entry.text) { clip ->
            latinIME.commitKlipyContent(clip.uri, clip.label, normalizeImageMimeType(clip.mimeType).mimeType)
        }
        if (outcome == ClipboardHistoryPasteOutcome.REJECTED &&
            ClipboardImageHistoryClip.decode(entry.text) == null) {
            latinIME.mKeyboardSwitcher.showToast(latinIME.getString(R.string.toast_msg_content_paste_failed), true)
        }
        return outcome
    }

    private fun currentTextSuggestionChanged(clip: RecentClip.Text): Boolean =
        recentTextClip(latinIME.currentInputEditorInfo) != clip

    private fun removeClipboardSuggestion(recordDismissal: Boolean = true) {
        cancelChromeExpiry()
        val clip = if (recordDismissal && !isHistoryPrivate() &&
            activeDecision?.candidate?.kind != SmartPastePolicy.Kind.OTP) recentTextClip(latinIME.currentInputEditorInfo) else null
        if (clip != null) {
            markClipDismissed(clip)
        } else {
            dontShowCurrentSuggestion = true
        }
        val csv = clipboardSuggestionView ?: return
        val wasVisible = csv.isShown
        releaseClipboardAccessory()
        if (wasVisible) {
            latinIME.setNeutralSuggestionStrip()
            latinIME.mHandler.postResumeSuggestions(false)
        }
    }

    companion object {
        data class ImageMimeInfo(val mimeType: String, val extension: String)

        const val PREF_SCREENSHOTS_FOLDER_URI = "clipboard_screenshots_folder_uri"

        private const val PREF_PROCESSED_SCREENSHOT_MEDIA_URIS = "clipboard_processed_screenshot_media_uris"
        private const val PREF_LAST_SCREENSHOT_MEDIA_URI = "clipboard_last_screenshot_media_uri"
        private const val PREF_LAST_SCREENSHOT_DATE_ADDED = "clipboard_last_screenshot_date_added"
        private const val SCREENSHOT_RECENT_WINDOW_SECONDS = 60L
        private const val MAX_SCREENSHOT_ROWS_TO_CHECK = 10
        private const val MAX_PROCESSED_SCREENSHOT_URIS = 20
        private val SCREENSHOT_RELATIVE_PATHS = listOf(
            "Pictures/Screenshots/",
            "DCIM/Screenshots/",
            "Screenshots/"
        )
        private var dontShowCurrentSuggestion: Boolean = false
        private var dismissedClipTimestamp: Long = 0L
        private var dismissedClipIdentity: String? = null
        const val RECENT_TIME_MILLIS = 3 * 60 * 1000L // 3 minutes (for clipboard suggestions)

        fun decodeImageHistoryClip(text: String) = ClipboardImageHistoryClip.decode(text)

        fun encodeImageHistoryClip(uri: Uri, mimeType: String, label: String) =
            ClipboardImageHistoryClip.encode(uri, mimeType, label)

        internal fun normalizeImageMimeType(mimeType: String?): ImageMimeInfo {
            val normalized = mimeType
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase(Locale.US)
                .orEmpty()
            return when (normalized) {
                "image/jpg", "image/pjpeg", "image/jpeg" -> ImageMimeInfo("image/jpeg", "jpg")
                "image/x-png", "image/png", "image/*" -> ImageMimeInfo("image/png", "png")
                "image/x-webp", "image/webp" -> ImageMimeInfo("image/webp", "webp")
                "image/gif" -> ImageMimeInfo("image/gif", "gif")
                "image/heic", "image/heic-sequence" -> ImageMimeInfo("image/heic", "heic")
                "image/heif", "image/heif-sequence" -> ImageMimeInfo("image/heif", "heif")
                "image/x-ms-bmp", "image/bmp" -> ImageMimeInfo("image/bmp", "bmp")
                else -> {
                    if (!normalized.startsWith("image/")) return ImageMimeInfo("image/png", "png")
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(normalized)
                        ?: normalized.substringAfter("image/")
                            .substringBefore('+')
                            .let(::sanitizeImageExtension)
                    ImageMimeInfo(normalized, extension.ifBlank { "img" })
                }
            }
        }

        internal fun cacheFileNameForSource(sourceUri: Uri, timeStamp: Long, mimeType: String): String {
            val extension = normalizeImageMimeType(mimeType).extension
            return "clip_${timeStamp}_${stableCacheToken(sourceUri)}.$extension"
        }

        internal fun isOwnClipboardCacheUri(context: Context, uri: Uri): Boolean {
            return resolveOwnClipboardCacheFile(context, uri) != null
        }

        private fun resolveOwnClipboardCacheFile(context: Context, uri: Uri): File? {
            if (uri.scheme != "content" || uri.authority != "${context.packageName}.fileprovider") return null
            val segments = uri.pathSegments
            if (segments.size < 3 || segments[0] != "cache" || segments[1] != "clipboard") return null
            val clipboardDir = File(context.cacheDir, "clipboard")
            val file = File(context.cacheDir, segments.drop(1).joinToString(File.separator))
            return try {
                val canonicalDir = clipboardDir.canonicalFile
                val canonicalFile = file.canonicalFile
                val dirPath = canonicalDir.path
                val filePath = canonicalFile.path
                if (filePath.startsWith("$dirPath${File.separator}")) canonicalFile else null
            } catch (_: Exception) {
                null
            }
        }

        private fun sanitizeImageExtension(extension: String): String {
            return extension
                .lowercase(Locale.US)
                .filter { it.isLetterOrDigit() }
                .take(12)
                .ifBlank { "img" }
        }

        private fun stableCacheToken(uri: Uri): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(uri.toString().toByteArray(Charsets.UTF_8))
            return digest.take(8).joinToString("") {
                java.lang.String.format(Locale.US, "%02x", it.toInt() and 0xff)
            }
        }
    }
}
