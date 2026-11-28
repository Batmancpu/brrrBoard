// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.snippets

import android.app.AlertDialog
import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.ViewCompat
import helium314.keyboard.latin.AppsManager
import helium314.keyboard.latin.ClipboardEntityExtractor
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.ClipboardImageHistoryClip
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.brain.SmartPastePolicy
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.databinding.ClipboardSuggestionBinding
import helium314.keyboard.latin.suggestions.ClipboardAccessoryView
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext
import java.util.Calendar

/** Main-thread UI receipts, no background monitoring or word-model involvement. */
internal class SmartSnippetController(
    private val ime: LatinIME, private val history: ClipboardHistoryManager
) {
    private val session = SmartSnippetSession()
    private var root: ClipboardAccessoryView? = null
    private var visibleRule: SnippetRule? = null
    private var visibleEpoch = -1L
    private var visibleConnection: InputConnection? = null
    private var expiry: Runnable? = null
    private var refresh: Runnable? = null
    private var dialog: AlertDialog? = null
    private var configuredRules: List<SnippetRule> = emptyList()
    private var notBefore = 0L
    private var activePackage: String? = null

    private fun privateEditor() = !ime.mayUseTransientCorrectionMemory(ime.mSettings.current)
    private fun eligibleEditor(editor: EditorInfo?) = !privateEditor() &&
        editor?.packageName != ime.packageName &&
        ime.mSettings.current.mClipboardHistoryEnabled &&
        !ime.mSettings.current.isApplicationSpecifiedCompletionsOn() &&
        SmartPastePolicy.destination(editor) == SmartPastePolicy.Destination.GENERAL &&
        editor?.hintText?.toString()?.let {
            !Regex("(?i)\\b(search|password|username|user name|login|credit card|security code|card number|cvv|cvc)\\b").containsMatchIn(it.take(160))
        } != false &&
        editor?.fieldName?.let {
            !Regex("(?i)(search|password|username|login|cardnumber|cvv|cvc)").containsMatchIn(it.take(160))
        } != false
    private fun panelOpen() = ime.mKeyboardSwitcher.run {
        isShowingClipboardHistory || isShowingEmojiPalettes || isShowingAiWritingTools ||
            isShowingAccessPointMenu || isShowingKlipyPalettes
    }
    private fun clock() = SystemClock.elapsedRealtime()
    private fun boundary(): SnippetBoundary = when (ime.cachedSmartSnippetBoundary()) {
        1 -> SnippetBoundary.EMPTY
        2 -> SnippetBoundary.NEW_LINE
        else -> SnippetBoundary.NONE
    }
    private fun eligible(rule: SnippetRule, boundary: SnippetBoundary, calendar: Calendar, now: Long): Boolean {
        val pkg = activePackage ?: return false
        return session.eligible(rule,pkg,now,SmartSnippetStore.today(calendar),calendar.get(Calendar.DAY_OF_WEEK),
            calendar.get(Calendar.HOUR_OF_DAY),boundary,visibleRule?.id == rule.id && root?.retired == false)
    }
    private fun candidates(boundary: SnippetBoundary): List<SnippetRule> {
        val now = clock()
        val cal = Calendar.getInstance()
        return configuredRules.filter { eligible(it,boundary,cal,now) }
    }

    fun beginEditor(editor: EditorInfo?) {
        hide()
        dismissDialog()
        if (!eligibleEditor(editor)) {
            if (privateEditor()) { SmartSnippetStore.discardCache(); session.reset() }
            configuredRules = emptyList()
            if (editor?.packageName != activePackage) session.reset()
            activePackage = editor?.packageName
            return
        }
        val pkg = editor?.packageName ?: return
        if (pkg != activePackage) notBefore = 0L
        configuredRules = SmartSnippetStore.snapshot(ime).filter { it.matches(pkg) }
        session.enter(pkg,clock(),configuredRules)
        activePackage = pkg
        candidates(SnippetBoundary.NONE) // Start each fixed window on editor entry, not on first render.
    }
    fun resumeEditor(editor: EditorInfo?) {
        // Startup callbacks can arrive before onWindowShown; do not renew an existing visit.
        if (activePackage != editor?.packageName || configuredRules.isEmpty()) beginEditor(editor)
        else if (eligibleEditor(editor)) session.enter(editor!!.packageName,clock(),configuredRules)
        if (configuredRules.isNotEmpty()) scheduleRefresh(80)
    }
    fun windowHidden() {
        session.hide(clock()); hide(); dismissDialog(); configuredRules = emptyList()
    }
    fun privacyBoundary() {
        hide(); dismissDialog(); session.reset(); configuredRules = emptyList(); activePackage = null
        SmartSnippetStore.discardCache()
    }
    fun hide() {
        expiry?.let { ime.mHandler.removeCallbacks(it) }; expiry = null
        refresh?.let { ime.mHandler.removeCallbacks(it) }; refresh = null
        val old = root
        root = null; visibleRule = null; visibleConnection = null; visibleEpoch = -1L
        old?.retire()
    }
    fun onTyping() {
        if (configuredRules.isEmpty() && root == null) return
        notBefore = clock() + 120L
        hide()
    }
    fun selectionChanged(start: Int, end: Int) {
        if (configuredRules.isEmpty() && root == null) return
        if (start < 0 || start != end || !eligibleEditor(ime.currentInputEditorInfo) ||
            boundary() == SnippetBoundary.NONE) {
            if (root != null) { hide(); ime.mHandler.postResumeSuggestions(false) }
            return
        }
        if (root == null && candidates(boundary()).isNotEmpty()) scheduleRefresh(350)
    }
    private fun scheduleRefresh(delay: Long) {
        refresh?.let { ime.mHandler.removeCallbacks(it) }
        val epoch = ime.editorGeneration()
        val task = Runnable {
            refresh = null
            if (ime.editorGeneration() == epoch && ime.isInputViewShown && !panelOpen())
                ime.tryShowSmartSnippet()
        }
        refresh = task
        ime.mHandler.postDelayed(task,delay)
    }
    private fun textEntry(rule: SnippetRule): ClipboardHistoryEntry? {
        val entry = history.getHistoryEntryContent(rule.id) ?: return null
        return entry.takeIf { it.isPinned && !ClipboardImageHistoryClip.hasImageHistoryPrefix(it.text) &&
            !ClipboardEntityExtractor.excludeFromAutomaticHistory(it.text) &&
            SmartSnippetStore.fingerprint(it.text) == rule.fingerprint }
    }
    private fun liveBoundary(connection: InputConnection, expected: SnippetBoundary): Boolean {
        return try {
            val before = connection.getTextBeforeCursor(1,0) ?: return false
            val after = connection.getTextAfterCursor(1,0) ?: return false
            if (after.isNotEmpty()) false else when(expected) {
                SnippetBoundary.EMPTY -> before.isEmpty()
                SnippetBoundary.NEW_LINE -> before.toString() == "\n"
                else -> false
            }
        } catch (_: RuntimeException) { false }
    }

    fun suggestion(editor: EditorInfo?, parent: ViewGroup): View? {
        if (configuredRules.isEmpty() && root == null) return null
        if (clock() < notBefore || !eligibleEditor(editor) || panelOpen() ||
            ime.smartPasteIsComposing() || activePackage != editor?.packageName) {
            hide(); return null
        }
        val b = boundary()
        if (b == SnippetBoundary.NONE) { hide(); return null }
        val connection = ime.currentInputConnection ?: return null
        val epoch = ime.editorGeneration()
        val eligible = candidates(b)
        val shown = visibleRule
        val existing = root
        if (existing != null && !existing.retired && shown != null && shown in eligible &&
            visibleConnection === connection && visibleEpoch == epoch) return existing
        hide()
        if (eligible.isEmpty() || !liveBoundary(connection,b)) return null
        val rule = eligible.firstOrNull { textEntry(it) != null } ?: return null
        val binding = ClipboardSuggestionBinding.inflate(LayoutInflater.from(ime),parent,false)
        val view = binding.root
        view.isSmartSnippet = true
        val colors = ime.mSettings.current.mColors
        val title = if (eligible.size > 1) rule.title + "  ·  +" + (eligible.size - 1) else rule.title
        view.configure(colors,title,false)
        view.prefersProminent = true
        binding.clipboardSuggestionPreview.isGone = true
        binding.clipboardSuggestionText.setCompoundDrawablesRelative(
            ContextCompat.getDrawable(ime,android.R.drawable.ic_menu_edit)?.mutate()?.apply {
                val size = (18 * ime.resources.displayMetrics.density).toInt()
                setBounds(0,0,size,size); colors.setColor(this,ColorType.SUGGESTION_AUTO_CORRECT)
            },null,null,null)
        val close = binding.clipboardSuggestionChrome
        close.isGone = false
        close.setCompoundDrawablesRelative(
            ContextCompat.getDrawable(ime,android.R.drawable.ic_menu_close_clear_cancel)?.mutate()?.apply {
                val size = (20 * ime.resources.displayMetrics.density).toInt()
                setBounds(0,0,size,size); colors.setColor(this,ColorType.SUGGESTION_AUTO_CORRECT)
            },null,null,null)
        close.contentDescription = "Dismiss smart suggestions for this app visit"
        close.setOnClickListener {
            if (root !== view || view.retired || !view.isShown) return@setOnClickListener
            configuredRules.forEach { session.consume(it.id) }
            hide(); ime.mHandler.postResumeSuggestions(false)
        }
        binding.clipboardSuggestionPaste.contentDescription = "Paste " + rule.title +
            if (eligible.size > 1) ". Long press for other smart snippets." else ""
        binding.clipboardSuggestionPaste.setOnClickListener {
            if (root === view && !view.retired && view.isShown) paste(rule,epoch,connection)
        }
        binding.clipboardSuggestionPaste.setOnLongClickListener {
            if (root !== view || view.retired || !view.isShown) false else {
                chooseSnippet(eligible,epoch,connection); true
            }
        }
        ViewCompat.addAccessibilityAction(binding.clipboardSuggestionPaste,"Smart snippet options") { _,_ ->
            if (root !== view || view.retired || !view.isShown) false else {
                chooseSnippet(eligible,epoch,connection); true
            }
        }
        root = view; visibleRule = rule; visibleConnection = connection; visibleEpoch = epoch
        session.markShown(rule.id)
        val task = Runnable {
            if (root === view) { hide(); ime.mHandler.postResumeSuggestions(false) }
        }
        expiry = task
        ime.mHandler.postDelayed(task,(session.deadline(rule.id)-clock()).coerceAtLeast(0L))
        return view
    }

    private fun paste(rule: SnippetRule, epoch: Long, connection: InputConnection) {
        if (!ime.isInputViewShown || ime.editorGeneration() != epoch ||
            ime.currentInputConnection !== connection || !eligibleEditor(ime.currentInputEditorInfo) ||
            ime.smartPasteIsComposing() || panelOpen() || clock() < notBefore) return
        val b = boundary()
        val live = SmartSnippetStore.snapshot(ime).firstOrNull { it == rule } ?: return
        if (!eligible(live,b,Calendar.getInstance(),clock()) || !liveBoundary(connection,b)) {
            hide(); ime.mHandler.postResumeSuggestions(false); return
        }
        val entry = textEntry(live) ?: return
        // Reuse the guarded explicit commit path; never pass snippet text to word learning.
        if (ime.insertSmartPasteEntity(entry.text,epoch,connection)) {
            session.consume(live.id)
            if (!SmartSnippetStore.markUsed(ime,live)) toast("Daily usage could not be saved")
            configuredRules = SmartSnippetStore.snapshot(ime).filter { it.matches(activePackage.orEmpty()) }
            hide(); ime.mHandler.postResumeSuggestions(false)
        }
    }

    private fun chooseSnippet(rules: List<SnippetRule>, epoch: Long, connection: InputConnection) {
        val choices = rules.filter { textEntry(it) != null }
        if (choices.isEmpty()) return
        val names = (choices.map { "Paste " + it.title } + "Dismiss for this visit").toTypedArray()
        showDialog(AlertDialog.Builder(theme()).setTitle("Smart snippets").setItems(names) { _,i ->
            if (i < choices.size) paste(choices[i],epoch,connection)
            else {
                configuredRules.forEach { session.consume(it.id) }; hide(); ime.mHandler.postResumeSuggestions(false)
            }
        }.setNegativeButton(android.R.string.cancel,null).create())
    }

    private fun theme(): Context = getPlatformDialogThemeContext(ime)
    private fun toast(message: String) = ime.mKeyboardSwitcher.showToast(message,false)
    fun dismissDialog() {
        val old = dialog; dialog = null; old?.dismiss()
    }
    private fun showDialog(next: AlertDialog) {
        if (privateEditor() || !ime.isInputViewShown) return
        dismissDialog()
        try {
            val window = next.window ?: return
            val token = ime.window?.window?.attributes?.token ?: return
            window.attributes = window.attributes.apply {
                this.token = token
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_SECURE)
            next.setOnDismissListener { if (dialog === next) dialog = null }
            dialog = next
            next.show()
        } catch (_: RuntimeException) { dismissDialog() }
    }
    private fun isValidText(entry: ClipboardHistoryEntry) =
        !ClipboardImageHistoryClip.hasImageHistoryPrefix(entry.text) &&
        !ClipboardEntityExtractor.excludeFromAutomaticHistory(entry.text)
    fun itemMenu(id: Long) {
        if (privateEditor()) { history.toggleClipPinned(id); return }
        val entry = history.getHistoryEntryContent(id) ?: return
        val canSuggest = isValidText(entry)
        val options = mutableListOf(if (entry.isPinned) "Unpin" else "Pin")
        if (canSuggest) options += "Smart suggestion"
        options += "Manage Smart snippets"
        val epoch = ime.editorGeneration()
        showDialog(AlertDialog.Builder(theme()).setTitle("Clipboard item")
            .setItems(options.toTypedArray()) { _,index ->
                if (ime.editorGeneration() != epoch || privateEditor()) return@setItems
                if (index == 0) history.toggleClipPinned(id)
                else if (index == 1 && canSuggest) configure(id)
                else manage()
            }.setNegativeButton(android.R.string.cancel,null).create())
    }
    fun beforeUnpin(id: Long): Boolean {
        if (SmartSnippetStore.snapshot(ime).none { it.id == id }) return true
        if (!SmartSnippetStore.remove(ime,id)) { toast("Could not remove smart suggestion"); return false }
        session.forget(id); configuredRules = configuredRules.filterNot { it.id == id }
        if (visibleRule?.id == id) hide()
        return true
    }
    fun manage() {
        if (privateEditor()) return
        val rules = SmartSnippetStore.snapshot(ime)
        if (rules.isEmpty()) { toast("Long-press a clipboard text item to create a smart suggestion"); return }
        val epoch = ime.editorGeneration()
        showDialog(AlertDialog.Builder(theme()).setTitle("Smart snippets")
            .setItems(rules.map { it.title + if (it.enabled) "" else " (off)" }.toTypedArray()) { _,index ->
                if (epoch != ime.editorGeneration() || privateEditor()) return@setItems
                val rule = rules[index]
                if (textEntry(rule) == null) {
                    showDialog(AlertDialog.Builder(theme()).setTitle("Snippet no longer available")
                        .setMessage("The pinned clipboard entry is missing or changed.")
                        .setPositiveButton("Remove rule") { _,_ ->
                            if (epoch == ime.editorGeneration() && !privateEditor()) SmartSnippetStore.remove(ime,rule.id)
                        }.setNegativeButton(android.R.string.cancel,null).create())
                } else configure(rule.id)
            }.setNegativeButton(android.R.string.cancel,null).create())
    }
    private fun configure(id: Long) {
        if (privateEditor()) return
        val entry = history.getHistoryEntryContent(id)?.takeIf(::isValidText) ?: return
        if (!SmartSnippetStore.storageAvailable(ime)) { toast("Smart snippet storage unavailable"); return }
        val existing = SmartSnippetStore.snapshot(ime).firstOrNull {
            it.id == id && it.fingerprint == SmartSnippetStore.fingerprint(entry.text)
        }
        val pkg = ime.currentInputEditorInfo?.packageName.orEmpty()
        val appLabel = AppsManager(ime).getPackagesAndNames().firstOrNull { it.first == pkg }?.second?.take(48) ?: "Saved"
        val next = existing ?: SnippetRule(id,SmartSnippetStore.fingerprint(entry.text),
            title = appLabel + " snippet " + (SmartSnippetStore.snapshot(ime).size + 1),
            packages = setOf(pkg))
        editRule(next,existing != null,ime.editorGeneration())
    }

    private fun editRule(rule: SnippetRule, existing: Boolean, epoch: Long) {
        if (epoch != ime.editorGeneration() || privateEditor()) return
        val ctx = theme()
        var draft = rule
        val density = ctx.resources.displayMetrics.density
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * density).toInt()
            setPadding(pad, (8*density).toInt(),pad,(8*density).toInt())
        }
        fun label(text: String) { body.addView(TextView(ctx).apply { this.text = text; textSize = 14f }) }
        fun button(text: String, action: () -> Unit) {
            body.addView(Button(ctx).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } })
        }
        fun check(text: String, value: Boolean, action: (Boolean)->Unit) {
            body.addView(CheckBox(ctx).apply { this.text = text; isChecked = value; setOnCheckedChangeListener { _,checked -> action(checked) } })
        }
        fun spinner(labels: List<String>, selection: Int, action: (Int)->Unit) {
            val spin = Spinner(ctx)
            spin.adapter = ArrayAdapter(ctx,android.R.layout.simple_spinner_dropdown_item,labels)
            spin.setSelection(selection)
            spin.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?,view: View?,position: Int,id: Long) { action(position) }
            }
            body.addView(spin,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(48*density).toInt()))
        }
        label(rule.title)
        check("Automatic suggestions",rule.enabled) { draft = draft.copy(enabled=it) }
        label("Suggest in")
        button(if(rule.allApps) "All apps" else "${rule.packages.size} selected app(s)") { chooseApps(draft,existing,epoch) }
        label("Frequency")
        spinner(listOf("Once per app visit","Once daily until pasted","Every keyboard opening"),
            SnippetCadence.values().indexOf(rule.cadence)) { draft = draft.copy(cadence=SnippetCadence.values()[it]) }
        label("Suggestion window")
        val durations = listOf(30,60,120,300)
        spinner(listOf("30 seconds","1 minute","2 minutes","5 minutes"),durations.indexOf(rule.durationSeconds)) {
            draft = draft.copy(durationSeconds=durations[it])
        }
        check("Reappear when the field is empty",rule.emptyAgain) { draft = draft.copy(emptyAgain=it) }
        check("Reappear at the end of a new empty line",rule.newLine) { draft = draft.copy(newLine=it) }
        button("Schedule · " + if(rule.days == 127 && rule.startHour == 0 && rule.endHour == 24) "Any time" else "Custom") {
            chooseSchedule(draft,existing,epoch)
        }
        label("Private title only. Rename in Settings → Tools → Clipboard.")
        val scroll = ScrollView(ctx).apply { addView(body); isFillViewport = false }
        val builder = AlertDialog.Builder(ctx).setTitle("Smart suggestion").setView(scroll)
            .setPositiveButton("Save",null).setNegativeButton(android.R.string.cancel,null)
        if (existing) builder.setNeutralButton("Remove") { _,_ ->
            if (epoch == ime.editorGeneration() && !privateEditor() && SmartSnippetStore.remove(ime,rule.id)) {
                session.forget(rule.id); configuredRules = configuredRules.filterNot { it.id == rule.id }; hide()
            }
        }
        val next = builder.create()
        showDialog(next)
        next.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            if (epoch != ime.editorGeneration() || privateEditor()) { next.dismiss(); return@setOnClickListener }
            val live = history.getHistoryEntryContent(rule.id)
            if (live == null || !isValidText(live) || SmartSnippetStore.fingerprint(live.text) != rule.fingerprint ||
                !draft.valid()) { toast("Choose at least one app and a valid clipboard item"); return@setOnClickListener }
            if (!live.isPinned) history.toggleClipPinned(live.id)
            if (history.getHistoryEntryContent(live.id)?.isPinned != true || !SmartSnippetStore.save(ime,draft)) {
                toast("Could not save smart suggestion"); return@setOnClickListener
            }
            session.forget(rule.id)
            configuredRules = SmartSnippetStore.snapshot(ime).filter { it.matches(activePackage.orEmpty()) }
            hide(); next.dismiss(); toast("Smart suggestion saved")
        }
    }

    private fun chooseApps(rule: SnippetRule, existing: Boolean, epoch: Long) {
        if (epoch != ime.editorGeneration() || privateEditor()) return
        val apps = AppsManager(ime).getPackagesAndNames().toMap().toMutableMap()
        (rule.packages + ime.currentInputEditorInfo?.packageName.orEmpty()).filter { it.isNotBlank() }
            .forEach { if (it !in apps) apps[it] = it }
        val list = apps.entries.sortedBy { it.value.lowercase() }
        val selected = rule.packages.toMutableSet()
        var all = rule.allApps
        val names = (listOf("All apps") + list.map { it.value + "\n" + it.key }).toTypedArray()
        val checked = BooleanArray(names.size) { i -> if (i == 0) all else list[i-1].key in selected }
        val next = AlertDialog.Builder(theme()).setTitle("Choose applications")
            .setMultiChoiceItems(names,checked) { _,i,on ->
                if (i == 0) all = on
                else if (on) selected.add(list[i-1].key) else selected.remove(list[i-1].key)
            }.setPositiveButton("Done",null)
            .setNegativeButton("Back") { _,_ -> editRule(rule,existing,epoch) }.create()
        showDialog(next)
        next.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            if ((!all && selected.isEmpty()) || selected.size > 64) { toast("Choose 1–64 apps, or All apps"); return@setOnClickListener }
            editRule(rule.copy(allApps=all,packages=selected.toSet()),existing,epoch)
        }
    }
    private fun chooseSchedule(rule: SnippetRule,existing: Boolean,epoch: Long) {
        if (epoch != ime.editorGeneration() || privateEditor()) return
        val ctx = theme()
        var days = rule.days
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(24,0,24,0) }
        listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday").forEachIndexed { i,name ->
            layout.addView(CheckBox(ctx).apply {
                text = name; isChecked = days and (1 shl i) != 0
                setOnCheckedChangeListener { _,on -> days = if(on) days or (1 shl i) else days and (1 shl i).inv() }
            })
        }
        fun hourPicker(label: String,value: Int,limit: Int): Spinner {
            layout.addView(TextView(ctx).apply { text = label })
            return Spinner(ctx).apply {
                adapter = ArrayAdapter(ctx,android.R.layout.simple_spinner_dropdown_item,
                    (0..limit).map { "%02d:00".format(it) })
                setSelection(value); layout.addView(this)
            }
        }
        val start = hourPicker("From",rule.startHour,23)
        val end = hourPicker("Until",rule.endHour,24)
        val scroll = ScrollView(ctx).apply { addView(layout) }
        val next = AlertDialog.Builder(ctx).setTitle("Schedule").setView(scroll)
            .setPositiveButton("Done",null).setNegativeButton("Back") { _,_ -> editRule(rule,existing,epoch) }.create()
        showDialog(next)
        next.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            if (days == 0) { toast("Select at least one day"); return@setOnClickListener }
            editRule(rule.copy(days=days,startHour=start.selectedItemPosition,endHour=end.selectedItemPosition),existing,epoch)
        }
    }
}
