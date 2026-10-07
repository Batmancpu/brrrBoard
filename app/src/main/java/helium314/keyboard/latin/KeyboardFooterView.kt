// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.LinearLayout
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ToolbarKey

/**
 * iOS-style accessory row below the main keyboard.
 *
 * The row is deliberately outside the keyboard grid so its height does not alter
 * key geometry; navigation-bar insets are added here by KeyboardWrapperView.
 */
class KeyboardFooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var emojiButton: ImageButton? = null
    private var inputMethodButton: ImageButton? = null
    private var emojiAction: Runnable? = null
    private var inputMethodAction: Runnable? = null
    private var navigationBarInset = 0

    init {
        orientation = HORIZONTAL
        clipChildren = false
        clipToPadding = false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        emojiButton = findViewById(R.id.keyboard_footer_emoji)
        inputMethodButton = findViewById(R.id.keyboard_footer_input_method)
        configureButtons()
    }

    fun setActions(emojiAction: Runnable, inputMethodAction: Runnable) {
        this.emojiAction = emojiAction
        this.inputMethodAction = inputMethodAction
        emojiButton?.setOnClickListener { emojiAction.run() }
        inputMethodButton?.setOnClickListener { inputMethodAction.run() }
    }

    fun setNavigationBarInset(inset: Int) {
        val safeInset = inset.coerceAtLeast(0)
        if (safeInset == navigationBarInset) return
        navigationBarInset = safeInset
        setPadding(paddingLeft, paddingTop, paddingRight, safeInset)
        requestLayout()
    }

    fun updateThemeColors(colors: Colors = Settings.getValues().mColors) {
        emojiButton?.let { colors.setColor(it, ColorType.FUNCTIONAL_KEY_TEXT) }
        inputMethodButton?.let { colors.setColor(it, ColorType.FUNCTIONAL_KEY_TEXT) }
    }

    private fun configureButtons() {
        val icons = KeyboardIconsSet.instance
        icons.loadIcons(context)

        emojiButton?.apply {
            contentDescription = context.getString(R.string.show_emoji_key)
            setImageDrawable(
                icons.getNewDrawable(
                    ToolbarKey.EMOJI.name.lowercase(java.util.Locale.US),
                    context
                )
            )
            setOnClickListener { emojiAction?.run() }
        }

        inputMethodButton?.apply {
            contentDescription = context.getString(R.string.language_switch_key_switch_input_method)
            // Deliberately use the microphone glyph while this button invokes the IME picker.
            setImageDrawable(
                icons.getNewDrawable(
                    ToolbarKey.VOICE.name.lowercase(java.util.Locale.US),
                    context
                )
            )
            setOnClickListener { inputMethodAction?.run() }
        }

        updateThemeColors()
    }
}
