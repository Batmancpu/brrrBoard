/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.latin.suggestions;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import helium314.keyboard.accessibility.AccessibilityUtils;
import helium314.keyboard.keyboard.KeyboardTypeface;
import helium314.keyboard.latin.PunctuationSuggestions;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.common.Colors;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.SettingsValues;
import helium314.keyboard.latin.utils.ResourceUtils;
import helium314.keyboard.latin.utils.ViewLayoutUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

final class SuggestionStripLayoutHelper {
    private static final int DEFAULT_SUGGESTIONS_COUNT_IN_STRIP = 3;
    private static final int CHIP_SUGGESTIONS_COUNT_IN_STRIP = 5;
    private static final float DEFAULT_CENTER_SUGGESTION_PERCENTILE = 0.40f;
    private static final int DEFAULT_MAX_MORE_SUGGESTIONS_ROW = 2;
    private static final int PUNCTUATIONS_IN_STRIP = 5;
    private static final float MIN_TEXT_XSCALE = 0.70f;

    public final int mPadding;
    private final int mWordViewPaddingLeft;
    private final int mWordViewPaddingTop;
    private final int mWordViewPaddingRight;
    private final int mWordViewPaddingBottom;
    private final int mChipHorizontalPadding;
    private final int mChipHorizontalMargin;
    private final int mChipVerticalMargin;
    private final float mChipElevation;
    public final int mDividerWidth;
    public final int mSuggestionsStripHeight;
    private final int mSuggestionsCountInStrip;
    public final int mMoreSuggestionsRowHeight;
    private int mMaxMoreSuggestionsRow;
    public final float mMinMoreSuggestionsWidth;
    public final int mMoreSuggestionsBottomGap;
    private boolean mMoreSuggestionsAvailable;

    // The index of these {@link ArrayList} is the position in the suggestion strip. The indices
    // increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
    // The position of the most important suggestion is in {@link #mCenterPositionInStrip}
    private final ArrayList<TextView> mWordViews;
    private final ArrayList<View> mDividerViews;
    private final ArrayList<TextView> mDebugInfoViews;

    private final int mColorValidTypedWord;
    private final int mColorTypedWord;
    private final int mColorAutoCorrect;
    private final int mColorSuggested;
    private final float mAlphaObsoleted;
    private final float mCenterSuggestionWeight;
    private final int mCenterPositionInStrip;
    private final int mTypedWordPositionWhenAutocorrect;

    private static final CharacterStyle BOLD_SPAN = new StyleSpan(Typeface.BOLD);
    private static final CharacterStyle UNDERLINE_SPAN = new UnderlineSpan();

    private final int mSuggestionStripOptions;
    // These constants are the flag values of
    // {@link R.styleable#SuggestionStripView_suggestionStripOptions} attribute.
    private static final int AUTO_CORRECT_BOLD = 0x01;
    private static final int AUTO_CORRECT_UNDERLINE = 0x02;
    private static final int VALID_TYPED_WORD_BOLD = 0x04;

    public SuggestionStripLayoutHelper(final Context context, final AttributeSet attrs,
            final int defStyle, final ArrayList<TextView> wordViews,
            final ArrayList<View> dividerViews, final ArrayList<TextView> debugInfoViews) {
        mWordViews = wordViews;
        mDividerViews = dividerViews;
        mDebugInfoViews = debugInfoViews;

        final TextView wordView = wordViews.get(0);
        final View dividerView = dividerViews.get(0);
        mWordViewPaddingLeft = wordView.getPaddingLeft();
        mWordViewPaddingTop = wordView.getPaddingTop();
        mWordViewPaddingRight = wordView.getPaddingRight();
        mWordViewPaddingBottom = wordView.getPaddingBottom();
        mPadding = wordView.getCompoundPaddingLeft() + wordView.getCompoundPaddingRight();
        dividerView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDividerWidth = dividerView.getMeasuredWidth();

        final Resources res = wordView.getResources();
        mSuggestionsStripHeight = res.getDimensionPixelSize(
                R.dimen.config_suggestions_strip_height);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SuggestionStripView, defStyle, R.style.SuggestionStripView);
        mSuggestionStripOptions = a.getInt(R.styleable.SuggestionStripView_suggestionStripOptions, 0);
        mAlphaObsoleted = ResourceUtils.getFraction(a, R.styleable.SuggestionStripView_alphaObsoleted, 1.0f);

        final Colors colors = Settings.getValues().mColors;
        mColorValidTypedWord = colors.get(ColorType.SUGGESTION_VALID_WORD);
        mColorTypedWord = colors.get(ColorType.SUGGESTION_TYPED_WORD);
        mColorAutoCorrect = colors.get(ColorType.SUGGESTION_AUTO_CORRECT);
        mColorSuggested = colors.get(ColorType.SUGGESTED_WORD);

        mSuggestionsCountInStrip = a.getInt(
                R.styleable.SuggestionStripView_suggestionsCountInStrip,
                DEFAULT_SUGGESTIONS_COUNT_IN_STRIP);
        mCenterSuggestionWeight = ResourceUtils.getFraction(a,
                R.styleable.SuggestionStripView_centerSuggestionPercentile,
                DEFAULT_CENTER_SUGGESTION_PERCENTILE);
        mMaxMoreSuggestionsRow = a.getInt(
                R.styleable.SuggestionStripView_maxMoreSuggestionsRow,
                DEFAULT_MAX_MORE_SUGGESTIONS_ROW);
        mMinMoreSuggestionsWidth = ResourceUtils.getFraction(a,
                R.styleable.SuggestionStripView_minMoreSuggestionsWidth, 1.0f);
        a.recycle();

        mCenterPositionInStrip = mSuggestionsCountInStrip / 2;
        // Assuming there are at least three suggestions. Also, note that the suggestions are
        // laid out according to script direction, so this is left of the center for LTR scripts
        // and right of the center for RTL scripts.
        mTypedWordPositionWhenAutocorrect = mCenterPositionInStrip - 1;
        mMoreSuggestionsBottomGap = res.getDimensionPixelOffset(
                R.dimen.config_more_suggestions_bottom_gap);
        mMoreSuggestionsRowHeight = res.getDimensionPixelSize(
                R.dimen.config_more_suggestions_row_height);
        mChipHorizontalPadding = res.getDimensionPixelSize(
                R.dimen.config_suggestion_text_horizontal_padding) * 2;
        mChipHorizontalMargin = Math.max(1, mChipHorizontalPadding / 3);
        mChipVerticalMargin = Math.max(2, mSuggestionsStripHeight / 10);
        mChipElevation = 0f;
    }

    public int getMaxMoreSuggestionsRow() {
        return mMaxMoreSuggestionsRow;
    }

    private int getMoreSuggestionsHeight() {
        return mMaxMoreSuggestionsRow * mMoreSuggestionsRowHeight + mMoreSuggestionsBottomGap;
    }

    public void setMoreSuggestionsHeight(final int remainingHeight) {
        final int currentHeight = getMoreSuggestionsHeight();
        if (currentHeight <= remainingHeight) {
            return;
        }
        mMaxMoreSuggestionsRow = (remainingHeight - mMoreSuggestionsBottomGap) / mMoreSuggestionsRowHeight;
    }

    private CharSequence getStyledSuggestedWord(final SuggestedWords suggestedWords,
            final int indexInSuggestedWords) {
        if (indexInSuggestedWords >= suggestedWords.size()) {
            return null;
        }
        final String word = suggestedWords.getLabel(indexInSuggestedWords);
        // TODO: don't use the index to decide whether this is the auto-correction/typed word, as
        // this is brittle
        final boolean isAutoCorrection = suggestedWords.mWillAutoCorrect
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION;
        final boolean isTypedWordValid = suggestedWords.mTypedWordValid
                && indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD;
        if (!isAutoCorrection && !isTypedWordValid) {
            return word;
        }

        final Spannable spannedWord = new SpannableString(word);
        final int options = mSuggestionStripOptions;
        if ((isAutoCorrection && (options & AUTO_CORRECT_BOLD) != 0)
                || (isTypedWordValid && (options & VALID_TYPED_WORD_BOLD) != 0)) {
            addStyleSpan(spannedWord, BOLD_SPAN);
        }
        if (isAutoCorrection && (options & AUTO_CORRECT_UNDERLINE) != 0) {
            addStyleSpan(spannedWord, UNDERLINE_SPAN);
        }
        return spannedWord;
    }

    /**
     * Convert an index of {@link SuggestedWords} to position in the suggestion strip.
     * @param indexInSuggestedWords the index of {@link SuggestedWords}.
     * @param suggestedWords the suggested words list
     * @return Non-negative integer of the position in the suggestion strip.
     *         Negative integer if the word of the index shouldn't be shown on the suggestion strip.
     */
    private int getPositionInSuggestionStrip(final int indexInSuggestedWords,
            final SuggestedWords suggestedWords) {
        final SettingsValues settingsValues = Settings.getValues();
        final boolean shouldOmitTypedWord = shouldOmitTypedWord(suggestedWords.mInputStyle,
                settingsValues.mGestureFloatingPreviewTextEnabled, true);
        return getPositionInSuggestionStrip(indexInSuggestedWords, suggestedWords.mWillAutoCorrect,
                shouldOmitTypedWord, mCenterPositionInStrip, mTypedWordPositionWhenAutocorrect);
    }

    static boolean shouldOmitTypedWord(final int inputStyle,
            final boolean gestureFloatingPreviewTextEnabled,
            final boolean shouldShowUiToAcceptTypedWord) {
        final boolean omitTypedWord = (inputStyle == SuggestedWords.INPUT_STYLE_TYPING)
                || (inputStyle == SuggestedWords.INPUT_STYLE_TAIL_BATCH)
                || (inputStyle == SuggestedWords.INPUT_STYLE_UPDATE_BATCH && gestureFloatingPreviewTextEnabled);
        return shouldShowUiToAcceptTypedWord && omitTypedWord;
    }

    static int getPositionInSuggestionStrip(final int indexInSuggestedWords,
            final boolean willAutoCorrect, final boolean omitTypedWord,
            final int centerPositionInStrip, final int typedWordPositionWhenAutoCorrect) {
        if (omitTypedWord) {
            if (indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD) {
                // Ignore.
                return -1;
            }
            if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION) {
                // Center in the suggestion strip.
                return centerPositionInStrip;
            }
            // If neither of those, the order in the suggestion strip is left of the center first
            // then right of the center, to both edges of the suggestion strip.
            // For example, center-1, center+1, center-2, center+2, and so on.
            final int offsetFromCenter = (indexInSuggestedWords % 2) == 0 ? -(indexInSuggestedWords / 2) : (indexInSuggestedWords / 2);
            return centerPositionInStrip + offsetFromCenter;
        }
        final int indexToDisplayMostImportantSuggestion;
        final int indexToDisplaySecondMostImportantSuggestion;
        if (willAutoCorrect) {
            indexToDisplayMostImportantSuggestion = SuggestedWords.INDEX_OF_AUTO_CORRECTION;
            indexToDisplaySecondMostImportantSuggestion = SuggestedWords.INDEX_OF_TYPED_WORD;
        } else {
            indexToDisplayMostImportantSuggestion = SuggestedWords.INDEX_OF_TYPED_WORD;
            indexToDisplaySecondMostImportantSuggestion = SuggestedWords.INDEX_OF_AUTO_CORRECTION;
        }
        if (indexInSuggestedWords == indexToDisplayMostImportantSuggestion) {
            // Center in the suggestion strip.
            return centerPositionInStrip;
        }
        if (indexInSuggestedWords == indexToDisplaySecondMostImportantSuggestion) {
            // Center-1.
            return typedWordPositionWhenAutoCorrect;
        }
        // If neither of those, the order in the suggestion strip is right of the center first
        // then left of the center, to both edges of the suggestion strip.
        // For example, Center+1, center-2, center+2, center-3, and so on.
        final int n = indexInSuggestedWords + 1;
        final int offsetFromCenter = (n % 2) == 0 ? -(n / 2) : (n / 2);
        return centerPositionInStrip + offsetFromCenter;
    }

    private int getSuggestionTextColor(final SuggestedWords suggestedWords,
            final int indexInSuggestedWords) {
        // Use identity for strings, not #equals : it's the typed word if it's the same object
        final boolean isTypedWord = suggestedWords.getInfo(indexInSuggestedWords).isKindOf(SuggestedWordInfo.KIND_TYPED);

        final int color;
        if (indexInSuggestedWords == SuggestedWords.INDEX_OF_AUTO_CORRECTION && suggestedWords.mWillAutoCorrect) {
            color = mColorAutoCorrect;
        } else if (isTypedWord && suggestedWords.mTypedWordValid) {
            color = mColorValidTypedWord;
        } else if (isTypedWord) {
            color = mColorTypedWord;
        } else {
            color = mColorSuggested;
        }
        if (suggestedWords.mIsObsoleteSuggestions && !isTypedWord) {
            return applyAlpha(color, mAlphaObsoleted);
        }
        return color;
    }

    private static int applyAlpha(final int color, final float alpha) {
        final int newAlpha = (int)(Color.alpha(color) * alpha);
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static void addDivider(final ViewGroup stripView, final View dividerView) {
        stripView.addView(dividerView);
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)dividerView.getLayoutParams();
        params.gravity = Gravity.CENTER;
    }

    /**
     * Ranks decoder-provided Emoji candidates into the visible side slots using local
     * usage/recency order. The center slot and its confidence decision are never changed.
     */
    public SuggestedWords prioritizeEmojiSideSuggestions(final SuggestedWords original,
            final List<String> rankedEmojiOutputs) {
        if (original == null || original.isEmpty() || original.isPunctuationSuggestions()
                || rankedEmojiOutputs == null || rankedEmojiOutputs.isEmpty()) {
            return original;
        }

        final HashMap<String, Integer> rankByEmoji = new HashMap<>();
        for (int i = 0; i < rankedEmojiOutputs.size(); i++) {
            final String emoji = rankedEmojiOutputs.get(i);
            if (emoji != null && !rankByEmoji.containsKey(emoji)) {
                rankByEmoji.put(emoji, i);
            }
        }
        if (rankByEmoji.isEmpty()) {
            return original;
        }

        final ArrayList<Integer> sideSlotIndices = new ArrayList<>(2);
        for (int position = 0; position < mSuggestionsCountInStrip; position++) {
            if (position == mCenterPositionInStrip) {
                continue;
            }
            for (int i = 0; i < original.size(); i++) {
                if (getPositionInSuggestionStrip(i, original) == position) {
                    sideSlotIndices.add(i);
                    break;
                }
            }
        }
        if (sideSlotIndices.isEmpty()) {
            return original;
        }

        final ArrayList<SuggestedWordInfo> infos = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) {
            infos.add(original.getInfo(i));
        }
        final HashSet<Integer> lockedSlots = new HashSet<>();
        final HashSet<String> alreadyPlacedEmojis = new HashSet<>();
        boolean changed = false;
        for (final int targetIndex : sideSlotIndices) {
            // Protect both typed and predicted text slots. This operation may
            // only reorder Emoji that are ALREADY visible in Emoji side slots.
            if (!infos.get(targetIndex).isEmoji()
                    || infos.get(targetIndex).isKindOf(SuggestedWordInfo.KIND_TYPED)) {
                lockedSlots.add(targetIndex);
                continue;
            }
            int bestIndex = -1;
            int bestRank = Integer.MAX_VALUE;
            for (int i = 0; i < infos.size(); i++) {
                if (lockedSlots.contains(i)
                        || getPositionInSuggestionStrip(i, original) == mCenterPositionInStrip) {
                    continue;
                }
                final SuggestedWordInfo candidate = infos.get(i);
                if (!candidate.isEmoji()
                        || candidate.isKindOf(SuggestedWordInfo.KIND_TYPED)
                        || alreadyPlacedEmojis.contains(candidate.getWord())) {
                    continue;
                }
                final Integer rank = rankByEmoji.get(candidate.getWord());
                if (rank != null && rank < bestRank) {
                    bestIndex = i;
                    bestRank = rank;
                }
            }
            if (bestIndex >= 0) {
                final String chosenEmoji = infos.get(bestIndex).getWord();
                if (bestIndex != targetIndex) {
                    Collections.swap(infos, targetIndex, bestIndex);
                    changed = true;
                }
                alreadyPlacedEmojis.add(chosenEmoji);
                lockedSlots.add(targetIndex);
            }
        }
        if (!changed) {
            return original;
        }
        return new SuggestedWords(infos, original.mRawSuggestions, original.mTypedWordInfo,
                original.mTypedWordValid, original.mWillAutoCorrect,
                original.mIsObsoleteSuggestions, original.mInputStyle, original.mSequenceNumber,
                original.mEmojiContextHint);
    }

    /**
     * Layout suggestions to the suggestions strip. And returns the start index of more
     * suggestions.
     *
     * @param suggestedWords suggestions to be shown in the suggestions strip.
     * @param stripView the suggestions strip view.
     * @param placerView the view where the debug info will be placed.
     * @return the start index of more suggestions.
     */
    public int layoutAndReturnStartIndexOfMoreSuggestions(
            final Context context,
            final SuggestedWords suggestedWords,
            final ViewGroup stripView,
            final ViewGroup placerView) {
        if (suggestedWords.isPunctuationSuggestions()) {
            return layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
                    (PunctuationSuggestions)suggestedWords, stripView);
        }

        final int wordCountToShow = suggestedWords.getWordCountToShow();
        final int startIndexOfMoreSuggestions = setupWordViewsAndReturnStartIndexOfMoreSuggestions(
                suggestedWords, mSuggestionsCountInStrip);
        final TextView centerWordView = mWordViews.get(mCenterPositionInStrip);
        // Confidence is a decoder decision, never a visual heuristic.
        final boolean centerConfidence = isCenterConfidenceActive(
                suggestedWords.mWillAutoCorrect, (Integer) centerWordView.getTag());
        final int stripWidth = stripView.getWidth();
        final int centerWidth = getSuggestionWidth(mCenterPositionInStrip, stripWidth);
        if (wordCountToShow == 1 || getTextScaleX(centerWordView.getText(), centerWidth,
                centerWordView.getPaint()) < MIN_TEXT_XSCALE) {
            // Layout only the most relevant suggested word at the center of the suggestion strip
            // by consolidating all slots in the strip.
            final int countInStrip = 1;
            mMoreSuggestionsAvailable = (wordCountToShow > countInStrip);
            layoutWord(context, mCenterPositionInStrip, stripWidth - mPadding, suggestedWords.mWillAutoCorrect);
            stripView.addView(centerWordView);
            setLayoutWeight(centerWordView, 1.0f, ViewGroup.LayoutParams.MATCH_PARENT);
            if (SuggestionStripView.DEBUG_SUGGESTIONS) {
                layoutDebugInfo(mCenterPositionInStrip, placerView, stripWidth);
            }
            final Integer lastIndex = (Integer)centerWordView.getTag();
            return (lastIndex == null ? 0 : lastIndex) + 1;
        }

        final int countInStrip = mSuggestionsCountInStrip;
        mMoreSuggestionsAvailable = (wordCountToShow > countInStrip);
        @SuppressWarnings("unused")
        int x = 0;
        for (int positionInStrip = 0; positionInStrip < countInStrip; positionInStrip++) {
            if (positionInStrip != 0) {
                final View divider = mDividerViews.get(positionInStrip);
                // Retain its measured width so the three slots do not shift when
                // the high-confidence center pill takes over the visual hierarchy.
                divider.setVisibility(centerConfidence ? View.INVISIBLE : View.VISIBLE);
                addDivider(stripView, divider);
                x += divider.getMeasuredWidth();
            }

            final int width = getSuggestionWidth(positionInStrip, stripWidth);
            final TextView wordView = layoutWord(context, positionInStrip, width, suggestedWords.mWillAutoCorrect);
            stripView.addView(wordView);
            setLayoutWeight(wordView, getSuggestionWeight(positionInStrip), ViewGroup.LayoutParams.MATCH_PARENT);
            x += wordView.getMeasuredWidth();

            if (SuggestionStripView.DEBUG_SUGGESTIONS) {
                layoutDebugInfo(positionInStrip, placerView, (int) stripView.getX() + x);
            }
        }
        return startIndexOfMoreSuggestions;
    }

    /**
     * Layout word suggestions as horizontally scrollable chips. This uses the same
     * SuggestedWords instance and suggestion indices as the classic strip; only the
     * visible presentation changes.
     */
    public int layoutChipsAndReturnStartIndexOfMoreSuggestions(
            final Context context,
            final SuggestedWords suggestedWords,
            final ViewGroup chipStripView) {
        final ArrayList<Integer> indexesToShow = getChipSuggestionIndexes(suggestedWords);
        int maxShownIndex = -1;
        final int stripWidth = chipStripView.getRootView() == null
                ? chipStripView.getResources().getDisplayMetrics().widthPixels
                : chipStripView.getRootView().getWidth();
        for (int position = 0; position < indexesToShow.size(); position++) {
            final int indexInSuggestedWords = indexesToShow.get(position);
            final TextView wordView = layoutChipWord(context, suggestedWords,
                    indexInSuggestedWords, position, stripWidth);
            chipStripView.addView(wordView);
            maxShownIndex = Math.max(maxShownIndex, indexInSuggestedWords);
        }
        final int startIndexOfMoreSuggestions = maxShownIndex + 1;
        mMoreSuggestionsAvailable = suggestedWords.size() > startIndexOfMoreSuggestions;
        return startIndexOfMoreSuggestions;
    }

    /**
     * Format appropriately the suggested word in {@link #mWordViews} specified by
     * <code>positionInStrip</code>. When the suggested word doesn't exist, the corresponding
     * {@link TextView} will be disabled and never respond to user interaction. The suggested word
     * may be shrunk or ellipsized to fit in the specified width.
     * <p>
     * The <code>positionInStrip</code> argument is the index in the suggestion strip. The indices
     * increase towards the right for LTR scripts and the left for RTL scripts, starting with 0.
     * The position of the most important suggestion is in {@link #mCenterPositionInStrip}. This
     * usually doesn't match the index in <code>suggedtedWords</code> -- see
     * {@link #getPositionInSuggestionStrip(int,SuggestedWords)}.
     *
     * @param positionInStrip the position in the suggestion strip.
     * @param width the maximum width for layout in pixels.
     * @return the {@link TextView} containing the suggested word appropriately formatted.
     */
    private TextView layoutWord(final Context context, final int positionInStrip,
            final int width, final boolean willAutoCorrect) {
        final TextView wordView = mWordViews.get(positionInStrip);
        resetWordViewForStrip(wordView);
        final Integer suggestionIndex = (Integer)wordView.getTag();
        final boolean isConfidencePill = positionInStrip == mCenterPositionInStrip
                && isCenterConfidenceActive(willAutoCorrect, suggestionIndex);
        final int pillInset;
        if (isConfidencePill) {
            // A single source of truth: the existing decoder-approved correction only.
            // Preserve the entire tap target, shrinking the visual pill inside the slot.
            pillInset = getConfidencePillHorizontalInset(width);
            wordView.setBackground(new InsetDrawable(createConfidencePillBackground(),
                    pillInset, mChipVerticalMargin, pillInset, mChipVerticalMargin));
        } else {
            pillInset = 0;
        }
        final CharSequence word = wordView.getText();
        // Dots below the center word displaced its optical baseline. The full
        // strip still supports press-and-hold/upward flick for more suggestions,
        // and an explicit accessibility action in SuggestionStripView.
        wordView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        wordView.setCompoundDrawablePadding(0);
        // {@link StyleSpan} in a content description may cause an issue of TTS/TalkBack.
        // Use a simple {@link String} to avoid the issue.
        wordView.setContentDescription(
                TextUtils.isEmpty(word)
                    ? context.getResources().getString(R.string.spoken_empty_suggestion)
                    : word.toString());
        final CharSequence text = getEllipsizedTextWithSettingScaleX(
                word, Math.max(1, width - 2 * pillInset), wordView.getPaint());
        final float scaleX = wordView.getTextScaleX();
        wordView.setText(text); // TextView.setText() resets text scale x to 1.0.
        wordView.setTextScaleX(scaleX);
        // A <code>wordView</code> should be disabled when <code>word</code> is empty in order to
        // make it unclickable.
        // With accessibility touch exploration on, <code>wordView</code> should be enabled even
        // when it is empty to avoid announcing as "disabled".
        wordView.setEnabled(!TextUtils.isEmpty(word)
                || AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled());
        return wordView;
    }

    private TextView layoutChipWord(final Context context, final SuggestedWords suggestedWords,
            final int indexInSuggestedWords, final int positionInStrip, final int stripWidth) {
        final TextView wordView = mWordViews.get(positionInStrip);
        final CharSequence word = getStyledSuggestedWord(suggestedWords, indexInSuggestedWords);
        wordView.setTag(indexInSuggestedWords);
        wordView.setText(word);
        wordView.setTextColor(getSuggestionTextColor(suggestedWords, indexInSuggestedWords));
        KeyboardTypeface.applyToTextView(wordView);
        wordView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        wordView.setContentDescription(
                TextUtils.isEmpty(word)
                        ? context.getResources().getString(R.string.spoken_empty_suggestion)
                        : word.toString());

        final int maxChipWidth = Math.max(mSuggestionsStripHeight * 3,
                Math.min(getFallbackStripWidth(context, stripWidth) / 2, mSuggestionsStripHeight * 5));
        wordView.setMaxWidth(maxChipWidth);
        wordView.setMinWidth(context.getResources().getDimensionPixelSize(R.dimen.config_suggestion_min_width));
        wordView.setPadding(mChipHorizontalPadding, 0, mChipHorizontalPadding, 0);
        wordView.setGravity(Gravity.CENTER);
        wordView.setBackground(createChipBackground());
        wordView.setElevation(mChipElevation);

        final CharSequence text = getEllipsizedTextWithSettingScaleX(
                word, maxChipWidth - (mChipHorizontalPadding * 2), wordView.getPaint());
        final float scaleX = wordView.getTextScaleX();
        wordView.setText(text);
        wordView.setTextScaleX(scaleX);
        wordView.setEnabled(!TextUtils.isEmpty(word)
                || AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled());

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                mSuggestionsStripHeight - (mChipVerticalMargin * 2));
        params.gravity = Gravity.CENTER_VERTICAL;
        params.setMargins(mChipHorizontalMargin, mChipVerticalMargin,
                mChipHorizontalMargin, mChipVerticalMargin);
        wordView.setLayoutParams(params);
        return wordView;
    }

    private void layoutDebugInfo(final int positionInStrip, final ViewGroup placerView,
            final int x) {
        final TextView debugInfoView = mDebugInfoViews.get(positionInStrip);
        final CharSequence debugInfo = debugInfoView.getText();
        if (debugInfo == null) {
            return;
        }
        placerView.addView(debugInfoView);
        debugInfoView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int infoWidth = debugInfoView.getMeasuredWidth();
        ViewLayoutUtils.placeViewAt(debugInfoView, x - infoWidth, 0, infoWidth, debugInfoView.getMeasuredHeight());
    }

    private int getSuggestionWidth(final int positionInStrip, final int maxWidth) {
        final int paddings = mPadding * mSuggestionsCountInStrip;
        final int dividers = mDividerWidth * (mSuggestionsCountInStrip - 1);
        final int availableWidth = maxWidth - paddings - dividers;
        return (int)(availableWidth * getSuggestionWeight(positionInStrip));
    }

    private ArrayList<Integer> getChipSuggestionIndexes(final SuggestedWords suggestedWords) {
        final SettingsValues settingsValues = Settings.getValues();
        final boolean shouldOmitTypedWord = shouldOmitTypedWord(suggestedWords.mInputStyle,
                settingsValues.mGestureFloatingPreviewTextEnabled, true);
        final ArrayList<Integer> indexes = new ArrayList<>(CHIP_SUGGESTIONS_COUNT_IN_STRIP);
        if (shouldOmitTypedWord) {
            addChipIndex(indexes, suggestedWords, SuggestedWords.INDEX_OF_AUTO_CORRECTION,
                    shouldOmitTypedWord);
        } else {
            addChipIndex(indexes, suggestedWords,
                    suggestedWords.mWillAutoCorrect
                            ? SuggestedWords.INDEX_OF_AUTO_CORRECTION
                            : SuggestedWords.INDEX_OF_TYPED_WORD,
                    shouldOmitTypedWord);
            addChipIndex(indexes, suggestedWords,
                    suggestedWords.mWillAutoCorrect
                            ? SuggestedWords.INDEX_OF_TYPED_WORD
                            : SuggestedWords.INDEX_OF_AUTO_CORRECTION,
                    shouldOmitTypedWord);
        }

        for (int index = 0; index < suggestedWords.size()
                && indexes.size() < CHIP_SUGGESTIONS_COUNT_IN_STRIP; index++) {
            addChipIndex(indexes, suggestedWords, index, shouldOmitTypedWord);
        }
        return indexes;
    }

    private static void addChipIndex(final ArrayList<Integer> indexes,
            final SuggestedWords suggestedWords, final int indexInSuggestedWords,
            final boolean shouldOmitTypedWord) {
        if (indexInSuggestedWords < 0 || indexInSuggestedWords >= suggestedWords.size()) {
            return;
        }
        if (shouldOmitTypedWord && indexInSuggestedWords == SuggestedWords.INDEX_OF_TYPED_WORD) {
            return;
        }
        if (!indexes.contains(indexInSuggestedWords)) {
            indexes.add(indexInSuggestedWords);
        }
    }

    private static int getFallbackStripWidth(final Context context, final int stripWidth) {
        return stripWidth > 0 ? stripWidth : context.getResources().getDisplayMetrics().widthPixels;
    }

    static boolean isCenterConfidenceActive(final boolean willAutoCorrect,
            @Nullable final Integer centerSuggestionIndex) {
        return willAutoCorrect && centerSuggestionIndex != null
                && centerSuggestionIndex == SuggestedWords.INDEX_OF_AUTO_CORRECTION;
    }

    // Text-driven, neutral mauve contrast tones: the pill is neither a keyboard-wide
    // theme tint nor a new confidence/ranking decision.
    static int confidencePillColor(final int textColor) {
        final int opaqueText = ColorUtils.setAlphaComponent(textColor, 255);
        final boolean lightText = ColorUtils.calculateLuminance(opaqueText) >= 0.18;
        final int mauve = lightText ? Color.rgb(125, 97, 123) : Color.rgb(216, 195, 212);
        // Custom suggestion text shades may be medium gray, not pure black/white.
        // In that case prefer legibility over decorative tint.
        if (ColorUtils.calculateContrast(opaqueText, mauve) < 4.5) {
            return lightText ? Color.rgb(33, 26, 33) : Color.rgb(252, 249, 252);
        }
        return mauve;
    }

    private int getConfidencePillHorizontalInset(final int width) {
        final float density = mWordViews.get(0).getResources().getDisplayMetrics().density;
        final int maxInset = Math.round(16 * density);
        // The visual pill is narrower than its touch target, especially for a
        // long single suggestion. Keep a minimum visible area in compact layouts.
        final int proportionalInset = Math.min(maxInset, width / 10);
        final int maxPillWidth = mSuggestionsStripHeight * 4;
        return Math.max(mChipHorizontalMargin, Math.max(proportionalInset,
                (width - maxPillWidth) / 2));
    }

    private Drawable createConfidencePillBackground() {
        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(confidencePillColor(mColorAutoCorrect));
        background.setCornerRadius(mSuggestionsStripHeight / 2.0f);
        return background;
    }

    private Drawable createChipBackground() {
        final Colors colors = Settings.getValues().mColors;
        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(colors.get(ColorType.AUTOFILL_BACKGROUND_CHIP));
        background.setCornerRadius(mSuggestionsStripHeight / 2.0f);
        return background;
    }

    private void resetWordViewForStrip(final TextView wordView) {
        wordView.setMaxWidth(Integer.MAX_VALUE);
        wordView.setPadding(mWordViewPaddingLeft, mWordViewPaddingTop,
                mWordViewPaddingRight, mWordViewPaddingBottom);
        wordView.setGravity(Gravity.CENTER);
        wordView.setIncludeFontPadding(false);
        wordView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        wordView.setElevation(0f);
        wordView.setBackgroundColor(Color.TRANSPARENT);
        Settings.getValues().mColors.setBackground(wordView, ColorType.STRIP_BACKGROUND);
    }

    private float getSuggestionWeight(final int positionInStrip) {
        if (positionInStrip == mCenterPositionInStrip) {
            return mCenterSuggestionWeight;
        }
        // TODO: Revisit this for cases of 5 or more suggestions
        return (1.0f - mCenterSuggestionWeight) / (mSuggestionsCountInStrip - 1);
    }

    private int setupWordViewsAndReturnStartIndexOfMoreSuggestions(
            final SuggestedWords suggestedWords, final int maxSuggestionInStrip) {
        // Clear all suggestions first
        for (int positionInStrip = 0; positionInStrip < maxSuggestionInStrip; ++positionInStrip) {
            final TextView wordView = mWordViews.get(positionInStrip);
            wordView.setText(null);
            wordView.setTag(null);
            // Make this inactive for touches in {@link #layoutWord(int,int)}.
            if (SuggestionStripView.DEBUG_SUGGESTIONS) {
                mDebugInfoViews.get(positionInStrip).setText(null);
            }
        }
        int count = 0;
        int indexInSuggestedWords;
        for (indexInSuggestedWords = 0; indexInSuggestedWords < suggestedWords.size()
                && count < maxSuggestionInStrip; indexInSuggestedWords++) {
            final int positionInStrip =
                    getPositionInSuggestionStrip(indexInSuggestedWords, suggestedWords);
            if (positionInStrip < 0) {
                continue;
            }
            final TextView wordView = mWordViews.get(positionInStrip);
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(indexInSuggestedWords);
            wordView.setText(getStyledSuggestedWord(suggestedWords, indexInSuggestedWords));
            wordView.setTextColor(getSuggestionTextColor(suggestedWords, indexInSuggestedWords));
            KeyboardTypeface.applyToTextView(wordView);
            if (SuggestionStripView.DEBUG_SUGGESTIONS) {
                mDebugInfoViews.get(positionInStrip).setText(suggestedWords.getDebugString(indexInSuggestedWords));
            }
            count++;
        }
        return indexInSuggestedWords;
    }

    private int layoutPunctuationsAndReturnStartIndexOfMoreSuggestions(
            final PunctuationSuggestions punctuationSuggestions, final ViewGroup stripView) {
        final int countInStrip = Math.min(punctuationSuggestions.size(), PUNCTUATIONS_IN_STRIP);
        for (int positionInStrip = 0; positionInStrip < countInStrip; positionInStrip++) {
            if (positionInStrip != 0) {
                // Punctuation rows never inherit the hidden confidence dividers.
                final View divider = mDividerViews.get(positionInStrip);
                divider.setVisibility(View.VISIBLE);
                addDivider(stripView, divider);
            }

            final TextView wordView = mWordViews.get(positionInStrip);
            resetWordViewForStrip(wordView);
            final String punctuation = punctuationSuggestions.getLabel(positionInStrip);
            // {@link TextView#getTag()} is used to get the index in suggestedWords at
            // {@link SuggestionStripView#onClick(View)}.
            wordView.setTag(positionInStrip);
            wordView.setText(punctuation);
            wordView.setContentDescription(punctuation);
            wordView.setTextScaleX(1.0f);
            wordView.setCompoundDrawables(null, null, null, null);
            wordView.setTextColor(mColorAutoCorrect);
            KeyboardTypeface.applyToTextView(wordView);
            stripView.addView(wordView);
            setLayoutWeight(wordView, 1.0f, mSuggestionsStripHeight);
        }
        mMoreSuggestionsAvailable = (punctuationSuggestions.size() > countInStrip);
        return countInStrip;
    }

    static void setLayoutWeight(final View v, final float weight, final int height) {
        final ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof final LinearLayout.LayoutParams llp) {
            llp.weight = weight;
            llp.width = 0;
            llp.height = height;
        }
    }

    private static float getTextScaleX(@Nullable final CharSequence text, final int maxWidth, final TextPaint paint) {
        paint.setTextScaleX(1.0f);
        final int width = getTextWidth(text, paint);
        if (width <= maxWidth || maxWidth <= 0) {
            return 1.0f;
        }
        return maxWidth / (float) width;
    }

    @Nullable
    private static CharSequence getEllipsizedTextWithSettingScaleX(
            @Nullable final CharSequence text, final int maxWidth, @NonNull final TextPaint paint) {
        if (text == null) {
            return null;
        }
        final float scaleX = getTextScaleX(text, maxWidth, paint);
        if (scaleX >= MIN_TEXT_XSCALE) {
            paint.setTextScaleX(scaleX);
            return text;
        }

        // <code>text</code> must be ellipsized with minimum text scale x.
        paint.setTextScaleX(MIN_TEXT_XSCALE);
        final boolean hasBoldStyle = hasStyleSpan(text, BOLD_SPAN);
        final boolean hasUnderlineStyle = hasStyleSpan(text, UNDERLINE_SPAN);
        // TextUtils.ellipsize erases any span object existed after ellipsized point.
        // We have to restore these spans afterward.
        final CharSequence ellipsizedText = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.MIDDLE);
        if (!hasBoldStyle && !hasUnderlineStyle) {
            return ellipsizedText;
        }
        final Spannable spannableText = (ellipsizedText instanceof Spannable)
                ? (Spannable)ellipsizedText : new SpannableString(ellipsizedText);
        if (hasBoldStyle) {
            addStyleSpan(spannableText, BOLD_SPAN);
        }
        if (hasUnderlineStyle) {
            addStyleSpan(spannableText, UNDERLINE_SPAN);
        }
        return spannableText;
    }

    private static boolean hasStyleSpan(@Nullable final CharSequence text,
            final CharacterStyle style) {
        if (text instanceof Spanned) {
            return ((Spanned)text).getSpanStart(style) >= 0;
        }
        return false;
    }

    private static void addStyleSpan(@NonNull final Spannable text, final CharacterStyle style) {
        text.removeSpan(style);
        text.setSpan(style, 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private static int getTextWidth(@Nullable final CharSequence text, final TextPaint paint) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        final int length = text.length();
        final float[] widths = new float[length];
        final int count;
        final Typeface savedTypeface = paint.getTypeface();
        try {
            paint.setTypeface(getTextTypeface(text));
            count = paint.getTextWidths(text, 0, length, widths);
        } finally {
            paint.setTypeface(savedTypeface);
        }
        int width = 0;
        for (int i = 0; i < count; i++) {
            width += Math.round(widths[i] + 0.5f);
        }
        return width;
    }

    private static Typeface getTextTypeface(@Nullable final CharSequence text) {
        Typeface base = hasStyleSpan(text, BOLD_SPAN) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        return KeyboardTypeface.resolve(text, base);
    }
}
