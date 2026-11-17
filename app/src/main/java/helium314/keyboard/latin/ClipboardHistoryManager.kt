// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.isGone
import coil.load
import coil.request.CachePolicy
import helium314.keyboard.compat.ClipboardManagerCompat
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.databinding.ClipboardSuggestionBinding
import helium314.keyboard.latin.permissions.PermissionsUtil
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
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
    private var clipboardSuggestionView: View? = null
    private var clipboardDao: ClipboardDao? = null
    private var screenshotObserver: ContentObserver? = null
    private var lastProcessedImageUri: String? = null
    private val processedScreenshotUris = ArrayDeque<String>()
    private val screenshotInFlightUris = mutableSetOf<String>()
    private val inaccessibleImageClipUris = mutableSetOf<String>()
    @Volatile
    private var latestImageSuggestion: RecentClip.Image? = null
    @Volatile
    private var latestScreenshotSuggestion: RecentClip.Image? = null

    fun onCreate() {
        clipboardManager = latinIME.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(this)
        clipboardDao = ClipboardDao.getInstance(latinIME)
        restoreProcessedScreenshotUris()
        // Migrate away from old preferences containing raw copied text/URI.
        ClipboardDismissalPreferences.removeLegacyRawContent(latinIME.prefs())
        if (latinIME.mSettings.current.mClipboardHistoryEnabled)
            fetchPrimaryClip()
        syncScreenshotObserver()
    }

    fun onDestroy() {
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
                && !ClipboardPrivacyPolicy.isPrivateEditor(latinIME.currentInputEditorInfo, settings.mIncognitoModeEnabled)
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
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (!shouldObserveScreenshots()) return@launch
                val dao = clipboardDao
                var processedAny = false

                for (screenshot in queryRecentScreenshots().asReversed()) {
                    if (!shouldObserveScreenshots()) break
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
                        if (cachedUri != null && dao != null && shouldObserveScreenshots()) {
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
        dontShowCurrentSuggestion = false
        // Make sure we read clipboard history content only if history settings is set.
        if (latinIME.mSettings.current.mClipboardHistoryEnabled) {
            fetchPrimaryClip(clipChanged = true)
        }
        refreshClipboardSuggestion()
    }

    fun updatePrimaryClip() {
        syncScreenshotObserver()
        if (shouldObserveScreenshots()) {
            checkForNewScreenshot()
        }
        if (latinIME.mSettings.current.mClipboardHistoryEnabled) {
            fetchPrimaryClip()
        }
    }

    private fun fetchPrimaryClip(clipChanged: Boolean = false) {
        try {
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
            if (clipData.description?.hasMimeType("text/*") == false) return
            clipData.getItemAt(0)?.let { clipItem ->
                val content = clipItem.coerceToText(latinIME)
                if (TextUtils.isEmpty(content)) return
                clipboardDao?.addClip(timeStamp, false, content.toString())
            }
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
        ClipboardManagerCompat.clearPrimaryClip(clipboardManager)
        removeClipboardSuggestion()
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

    fun getHistorySize() = if (isHistoryPrivate()) 0 else clipboardDao?.count() ?: 0

    fun getHistoryEntry(position: Int) = if (isHistoryPrivate()) null else clipboardDao?.getAt(position)

    fun getHistoryEntryContent(id: Long) = if (isHistoryPrivate()) null else clipboardDao?.get(id)

    private fun isHistoryPrivate(): Boolean = ClipboardPrivacyPolicy.isPrivateEditor(
        latinIME.currentInputEditorInfo, latinIME.mSettings.current.mIncognitoModeEnabled)

    fun setHistoryChangeListener(listener: ClipboardDao.Listener?) {
        clipboardDao?.listener = listener
    }

    fun retrieveClipboardContent(): CharSequence {
        val clipData = clipboardManager.primaryClip ?: return ""
        if (clipData.itemCount == 0) return ""
        return clipData.getItemAt(0)?.coerceToText(latinIME) ?: ""
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
        val editor = editorInfo ?: return null
        val type = editor.inputType
        if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT ||
            ClipboardPrivacyPolicy.isPrivateEditor(editor, latinIME.mSettings.current.mIncognitoModeEnabled)) return null
        val clip = clipboardManager.primaryClip ?: return null
        val description = clip.description
        if (clip.itemCount == 0 || ClipboardManagerCompat.getClipSensitivity(description) == true ||
            !description.hasMimeType("text/plain")) return null
        val timeStamp = ClipboardManagerCompat.getClipTimestamp(clip)
        if (System.currentTimeMillis() - timeStamp > RECENT_TIME_MILLIS) return null
        val content = clip.getItemAt(0).text?.toString()?.takeIf { it.isNotBlank() } ?: return null
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
        if (System.currentTimeMillis() - timestamp > RECENT_TIME_MILLIS) return null
        val uri = clip.imageUri()?.takeIf { it.scheme == "content" } ?: return null
        val mime = runCatching { latinIME.contentResolver.getType(uri) }.getOrNull() ?: return null
        if (supported.none { ClipDescription.compareMimeTypes(mime, it) }) return null
        return RichPaste(uri, mime, timestamp)
    }

    fun getClipboardSuggestionView(editorInfo: EditorInfo?, parent: ViewGroup?): View? {
        // maybe no need to create a new view
        // but a cache has to consider a few possible changes, so better don't implement without need
        clipboardSuggestionView = null

        // get the content, or return null
        if (!latinIME.mSettings.current.mSuggestClipboardContent) return null
        if (dontShowCurrentSuggestion) return null
        if (parent == null) return null
        val screenshot = recentScreenshotPaste(editorInfo)
        val recentText = recentTextClip(editorInfo)
        // Never displace a newer copied text item with an older screenshot.
        val clip = recentText?.takeUnless { screenshot != null && screenshot.timestamp >= it.timeStamp }
        val rich = if (clip == null) screenshot ?: recentRichPaste(editorInfo) else null
        if (clip == null && rich == null) return null
        val connection = latinIME.currentInputConnection ?: return null
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
        KeyboardTypeface.applyToTextView(textView)
        val clipIcon = keyboard.mIconsSet.getIconDrawable(ToolbarKey.PASTE.name.lowercase())
        textView.text = clip?.content?.take(40) ?: latinIME.getString(
            if (rich?.isScreenshot == true) R.string.board_paste_from_screenshots
            else if (rich?.mimeType?.startsWith("image/") == true) R.string.board_paste_image
            else R.string.board_paste_file,
        )
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(if (isImageChip) null else clipIcon, null, null, null)
        textView.setOnClickListener {
            if (latinIME.currentInputConnection !== connection ||
                (if (clip != null) currentTextSuggestionChanged(clip)
                else (if (rich?.isScreenshot == true) recentScreenshotPaste(latinIME.currentInputEditorInfo)
                      else recentRichPaste(latinIME.currentInputEditorInfo)) != rich)) {
                binding.root.isGone = true
                return@setOnClickListener
            }
            if (clip != null) {
                markClipDismissed(clip)
                latinIME.onTextInput(clip.content.toString())
            } else if (rich != null) {
                if (!latinIME.commitKlipyContent(rich.uri, textView.text.toString(), rich.mimeType)) return@setOnClickListener
                dontShowCurrentSuggestion = true
            }
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                KeyCode.NOT_SPECIFIED, it, HapticEvent.KEY_PRESS,
            )
            binding.root.isGone = true
        }

        val closeButton = binding.clipboardSuggestionClose
        val colors = latinIME.mSettings.current.mColors

        // Dismissing a chip is a neutral utility, not an Enter/Search action.
        val closeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 11f * latinIME.resources.displayMetrics.density
            setColor(colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND))
        }
        closeButton.background = closeBg
        closeButton.contentDescription = latinIME.getString(R.string.clipboard_dismiss_suggestion)
        val closeIcon = KeyboardIconsSet.instance.getNewDrawable("ic_close", latinIME)
            ?: keyboard.mIconsSet.getIconDrawable(ToolbarKey.CLOSE_HISTORY.name.lowercase())
        closeButton.setImageDrawable(closeIcon)
        colors.setColor(closeButton, ColorType.FUNCTIONAL_KEY_TEXT)
        closeButton.setOnClickListener {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, it, HapticEvent.KEY_PRESS)
            if (clip != null) markClipDismissed(clip) else dontShowCurrentSuggestion = true
            removeClipboardSuggestion()
        }

        textView.setTextColor(colors.get(ColorType.KEY_TEXT))
        clipIcon?.let { colors.setColor(it, ColorType.KEY_ICON) }
        colors.setBackground(binding.root, ColorType.CLIPBOARD_SUGGESTION_BACKGROUND)

        clipboardSuggestionView = binding.root
        return clipboardSuggestionView
    }

    private fun cacheImageClip(clip: RecentClip.Image): Uri? {
        val uriString = clip.uri.toString()
        if (inaccessibleImageClipUris.contains(uriString)) return null

        resolveOwnClipboardCacheFile(latinIME, clip.uri)?.let { cachedFile ->
            return if (cachedFile.exists() && cachedFile.length() > 0L) clip.uri else null
        }

        val mimeInfo = normalizeImageMimeType(clip.mimeType)
        val clipboardDir = File(latinIME.cacheDir, "clipboard").apply { mkdirs() }
        val imageFile = File(clipboardDir, cacheFileNameForSource(clip.uri, clip.timeStamp, mimeInfo.mimeType))
        if (!imageFile.exists() || imageFile.length() == 0L) {
            if (!copyImageClipAtomically(clip.uri, imageFile)) {
                if (inaccessibleImageClipUris.size > 50) inaccessibleImageClipUris.clear()
                inaccessibleImageClipUris.add(uriString)
                return null
            }
        }
        return FileProvider.getUriForFile(latinIME, "${latinIME.packageName}.fileprovider", imageFile)
    }

    fun pasteHistoryEntry(entry: ClipboardHistoryEntry): ClipboardHistoryPasteOutcome {
        val outcome = ClipboardHistoryPastePolicy.paste(entry.text) { clip ->
            latinIME.commitKlipyContent(clip.uri, clip.label, normalizeImageMimeType(clip.mimeType).mimeType)
        }
        if (outcome == ClipboardHistoryPasteOutcome.REJECTED &&
            ClipboardImageHistoryClip.decode(entry.text) == null) {
            latinIME.mKeyboardSwitcher.showToast(latinIME.getString(R.string.toast_msg_content_paste_failed), true)
        }
        return outcome
    }

    private fun copyImageClipAtomically(sourceUri: Uri, imageFile: File): Boolean {
        val tempFile = File.createTempFile("${imageFile.nameWithoutExtension}_", ".tmp", imageFile.parentFile)
        return try {
            latinIME.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            if (tempFile.length() == 0L) return false

            if (imageFile.exists() && !imageFile.delete()) {
                Log.w("ClipboardHistoryManager", "Failed to replace cached image clipboard clip")
                return false
            }
            if (!tempFile.renameTo(imageFile)) {
                tempFile.copyTo(imageFile, overwrite = true)
            }
            imageFile.length() > 0L
        } catch (e: Exception) {
            if (e is java.io.FileNotFoundException || e is IllegalArgumentException || e is IllegalStateException || e is SecurityException) {
                Log.w("ClipboardHistoryManager", "Screenshot media not ready/accessible (${e.javaClass.simpleName})")
            } else {
                Log.w("ClipboardHistoryManager", "Failed to cache image clipboard clip (${e.javaClass.simpleName})")
            }
            false
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w("ClipboardHistoryManager", "Failed to delete temp image clipboard clip")
            }
        }
    }

    private fun currentTextSuggestionChanged(clip: RecentClip.Text): Boolean =
        recentTextClip(latinIME.currentInputEditorInfo) != clip

    private fun removeClipboardSuggestion() {
        val clip = recentTextClip(latinIME.currentInputEditorInfo)
        if (clip != null) {
            markClipDismissed(clip)
        } else {
            dontShowCurrentSuggestion = true
        }
        val csv = clipboardSuggestionView ?: return
        if (csv.parent != null && !csv.isGone) {
            // clipboard view is shown ->
            latinIME.setNeutralSuggestionStrip()
            latinIME.mHandler.postResumeSuggestions(false)
        }
        csv.isGone = true
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
