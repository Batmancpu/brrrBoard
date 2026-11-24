// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic

import android.app.AlertDialog
import android.os.SystemClock
import android.text.Spanned
import android.view.WindowManager
import android.view.inputmethod.InputConnection
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.security.MessageDigest

/** Dialog keeps a range and digest, never a selected-text preview. */
class TextToolsController(private val ime: LatinIME) {
    private var active: AlertDialog? = null
    private var selectionStart = -1
    private var selectionEnd = -1
    private val expire = Runnable { dismiss() }
    fun dismiss() {
        ime.mHandler.removeCallbacks(expire)
        val dialog = active
        active = null; selectionStart = -1; selectionEnd = -1
        dialog?.dismiss()
    }
    fun selectionChanged(start: Int,end: Int) {
        if (active != null && (start != selectionStart || end != selectionEnd)) dismiss()
    }
    private fun readPlainSelection(ic: InputConnection): String? {
        val text = try { ic.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES) }
            catch (_: RuntimeException) { null } ?: return null
        if (text.isEmpty() || text.length > ContextualTextActions.MAX_CHARS ||
            (text is Spanned && text.getSpans(0,text.length,Any::class.java).isNotEmpty())) return null
        return text.toString()
    }
    private fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
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
                .setItems(actions.map { it.label }.toTypedArray()) { _, index ->
                    if (SystemClock.elapsedRealtime()-now !in 0L until 30_000L ||
                        !ime.isInputViewShown || epoch != ime.editorGeneration() ||
                        ic !== ime.currentInputConnection || editor !== ime.currentInputEditorInfo ||
                        connection.expectedSelectionStart != start || connection.expectedSelectionEnd != end ||
                        !ime.mayUseTransientCorrectionMemory(ime.mSettings.current)) return@setItems
                    val before = readPlainSelection(ic) ?: return@setItems
                    if (!MessageDigest.isEqual(fingerprint,digest(before))) return@setItems
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
