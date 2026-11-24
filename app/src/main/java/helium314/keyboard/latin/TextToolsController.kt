// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.app.AlertDialog
import android.os.SystemClock
import android.text.Spanned
import android.view.WindowManager
import android.view.inputmethod.InputConnection
import helium314.keyboard.latin.inputlogic.ContextualTextActions
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.security.MessageDigest
import helium314.keyboard.latin.inputlogic.DynamicTextTemplate
import helium314.keyboard.latin.settings.TextTemplateStore
import helium314.keyboard.latin.settings.TextReplacementStore
import helium314.keyboard.latin.inputlogic.LiteralTextReplacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dialog keeps a range and digest, never a selected-text preview. */
class TextToolsController(private val ime: LatinIME) {
    private var active: AlertDialog? = null
    private var loadJob: Job? = null
    private var requestGeneration = 0L
    private var selectionStart = -1
    private var selectionEnd = -1
    private val expire = Runnable { dismiss() }
    fun dismiss() {
        requestGeneration++
        loadJob?.cancel(); loadJob = null
        ime.mHandler.removeCallbacks(expire)
        val dialog = active
        active = null; selectionStart = -1; selectionEnd = -1
        dialog?.dismiss()
    }
    fun selectionChanged(start: Int,end: Int) {
        if (selectionStart >= 0 && (start != selectionStart || end != selectionEnd)) dismiss()
    }
    private fun readPlainSelection(ic: InputConnection): String? {
        val text = try { ic.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES) }
            catch (_: RuntimeException) { null } ?: return null
        if (text.isEmpty() || text.length > ContextualTextActions.MAX_CHARS ||
            (text is Spanned && text.getSpans(0,text.length,Any::class.java).isNotEmpty())) return null
        return text.toString()
    }
    private fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    private fun cursorContext(ic: InputConnection): Pair<String,String>? = try {
        val before = ic.getTextBeforeCursor(8,0)
        val after = ic.getTextAfterCursor(8,0)
        if (before == null || after == null || before.length > 8 || after.length > 8) null
        else before.toString() to after.toString()
    } catch (_: RuntimeException) { null }
    private fun requestTemplates(ic: InputConnection, editor: android.view.inputmethod.EditorInfo,
                                 epoch: Long, start: Int, locale: java.util.Locale) {
        val original = cursorContext(ic) ?: return
        val beforeDigest = digest(original.first)
        val afterDigest = digest(original.second)
        val created = SystemClock.elapsedRealtime()
        val generation = requestGeneration
        selectionStart = start; selectionEnd = start
        fun current(): Boolean = generation == requestGeneration &&
            SystemClock.elapsedRealtime()-created in 0L until 30_000L &&
            ime.isInputViewShown && epoch == ime.editorGeneration() &&
            ic === ime.currentInputConnection && editor === ime.currentInputEditorInfo &&
            ime.mInputLogic.mConnection.expectedSelectionStart == start &&
            ime.mInputLogic.mConnection.expectedSelectionEnd == start &&
            ime.mayUseTransientCorrectionMemory(ime.mSettings.current)
        loadJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            val saved = withContext(Dispatchers.IO) { TextTemplateStore.load(ime) }
            if (!current()) { if (generation == requestGeneration) dismiss(); return@launch }
            if (saved == null || saved.templates.isEmpty()) {
                ime.mKeyboardSwitcher.showToast("Select text or add templates in BR Brain",false)
                dismiss()
                return@launch
            }
            try {
                val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(ime))
                    .setTitle("Insert text template")
                    .setItems(saved.templates.map { it.name }.toTypedArray()) { _, index ->
                        if (!current() || TextTemplateStore.revision() != saved.revision) return@setItems
                        val live = cursorContext(ic) ?: return@setItems
                        if (!MessageDigest.isEqual(beforeDigest,digest(live.first)) ||
                            !MessageDigest.isEqual(afterDigest,digest(live.second))) return@setItems
                        val rendered = DynamicTextTemplate.renderNow(saved.templates[index],locale) ?: return@setItems
                        if (!ime.mInputLogic.commitVerifiedTemplate(rendered.text,rendered.cursor,start,
                                epoch,ic,live.first,live.second))
                            ime.mKeyboardSwitcher.showToast("Finish the current word and try again",false)
                    }.setNegativeButton(android.R.string.cancel,null).create()
                val window = dialog.window ?: run { dismiss(); return@launch }
                val token = ime.window?.window?.attributes?.token ?: run { dismiss(); return@launch }
                window.attributes = window.attributes.apply {
                    this.token = token
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_SECURE)
                dialog.setOnDismissListener {
                    if (active === dialog) {
                        active = null; selectionStart = -1; selectionEnd = -1
                        ime.mHandler.removeCallbacks(expire)
                        requestGeneration++
                    }
                }
                active = dialog
                dialog.show()
                ime.mHandler.postDelayed(expire,(30_000L-(SystemClock.elapsedRealtime()-created)).coerceAtLeast(0L))
            } catch (_: RuntimeException) { dismiss() }
        }
    }

    private fun requestReplacementRules(ic: InputConnection, editor: android.view.inputmethod.EditorInfo,
                                        epoch: Long, start: Int, end: Int, fingerprint: ByteArray,
                                        created: Long) {
        dismiss()
        val generation = requestGeneration
        selectionStart = start; selectionEnd = end
        fun current(): Boolean = generation == requestGeneration &&
            SystemClock.elapsedRealtime()-created in 0L until 30_000L &&
            ime.isInputViewShown && epoch == ime.editorGeneration() &&
            ic === ime.currentInputConnection && editor === ime.currentInputEditorInfo &&
            ime.mInputLogic.mConnection.expectedSelectionStart == start &&
            ime.mInputLogic.mConnection.expectedSelectionEnd == end &&
            ime.mayUseTransientCorrectionMemory(ime.mSettings.current)
        loadJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            val saved = withContext(Dispatchers.IO) { TextReplacementStore.load(ime) }
            if (!current()) { if (generation == requestGeneration) dismiss(); return@launch }
            if (saved == null || saved.rules.isEmpty()) {
                ime.mKeyboardSwitcher.showToast("Add replacement rules in BR Brain settings",false)
                dismiss(); return@launch
            }
            try {
                val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(ime))
                    .setTitle("Find & replace in selection")
                    .setItems(saved.rules.map { it.name }.toTypedArray()) { _, index ->
                        if (!current() || TextReplacementStore.revision() != saved.revision) return@setItems
                        val before = readPlainSelection(ic) ?: return@setItems
                        if (!MessageDigest.isEqual(fingerprint,digest(before))) return@setItems
                        val rule = saved.rules[index]
                        val result = LiteralTextReplacement.replace(before,rule.find,rule.replacement,
                            rule.matchCase,rule.wholeWord,rule.all)
                        if (result == null) {
                            ime.mKeyboardSwitcher.showToast("No safe matching replacement",false)
                            return@setItems
                        }
                        if (current() && ime.mInputLogic.commitVerifiedSelectionEdit(before,result.text,start,end,epoch,ic))
                            ime.mKeyboardSwitcher.showToast("Replacement applied",false)
                    }.setNegativeButton(android.R.string.cancel,null).create()
                val window = dialog.window ?: run { dismiss(); return@launch }
                val token = ime.window?.window?.attributes?.token ?: run { dismiss(); return@launch }
                window.attributes = window.attributes.apply {
                    this.token = token
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_SECURE)
                dialog.setOnDismissListener { if (active === dialog) dismiss() }
                active = dialog
                dialog.show()
                ime.mHandler.postDelayed(expire,(30_000L-(SystemClock.elapsedRealtime()-created)).coerceAtLeast(0L))
            } catch (_: RuntimeException) { dismiss() }
        }
    }

    fun request() {
        dismiss()
        val values = ime.mSettings.current
        if (!ime.isInputViewShown || !ime.mayUseTransientCorrectionMemory(values)) return
        val ic = ime.currentInputConnection ?: return
        val editor = ime.currentInputEditorInfo ?: return
        val epoch = ime.editorGeneration()
        val connection = ime.mInputLogic.mConnection
        val start = connection.expectedSelectionStart
        val end = connection.expectedSelectionEnd
        if (start >= 0 && end == start) {
            requestTemplates(ic,editor,epoch,start,values.mLocale)
            return
        }
        if (start < 0 || end <= start || end-start > ContextualTextActions.MAX_CHARS) {
            ime.mKeyboardSwitcher.showToast("Select plain text first",false)
            return
        }
        val selected = readPlainSelection(ic) ?: return
        if (selected.length != end-start) return
        val fingerprint = digest(selected)
        val now = SystemClock.elapsedRealtime()
        val actions = ContextualTextActions.Action.entries
        val locale = values.mLocale
        try {
            val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(ime))
                .setTitle("Edit selected text")
                .setItems((actions.map { it.label } + "Find & replace").toTypedArray()) { _, index ->
                    if (SystemClock.elapsedRealtime()-now !in 0L until 30_000L ||
                        !ime.isInputViewShown || epoch != ime.editorGeneration() ||
                        ic !== ime.currentInputConnection || editor !== ime.currentInputEditorInfo ||
                        connection.expectedSelectionStart != start || connection.expectedSelectionEnd != end ||
                        !ime.mayUseTransientCorrectionMemory(ime.mSettings.current)) return@setItems
                    val before = readPlainSelection(ic) ?: return@setItems
                    if (!MessageDigest.isEqual(fingerprint,digest(before))) return@setItems
                    if (index == actions.size) {
                        requestReplacementRules(ic,editor,epoch,start,end,fingerprint,now)
                        return@setItems
                    }
                    val after = ContextualTextActions.transform(before,actions[index],locale) ?: return@setItems
                    ime.mInputLogic.commitVerifiedSelectionEdit(before,after,start,end,epoch,ic)
                }.setNegativeButton(android.R.string.cancel,null).create()
            val window = dialog.window ?: return
            val token = ime.window?.window?.attributes?.token ?: return
            window.attributes = window.attributes.apply {
                this.token = token
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_SECURE)
            dialog.setOnDismissListener { if (active === dialog) {
                active = null; selectionStart = -1; selectionEnd = -1
                ime.mHandler.removeCallbacks(expire)
            } }
            active = dialog; selectionStart = start; selectionEnd = end
            dialog.show()
            ime.mHandler.postDelayed(expire,30_000)
        } catch (_: RuntimeException) { dismiss() }
    }
}
