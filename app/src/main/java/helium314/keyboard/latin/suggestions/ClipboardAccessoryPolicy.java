// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;
import helium314.keyboard.latin.SuggestedWords;

/** Geometry/idle decision only; never access clipboard or editor contents. */
public final class ClipboardAccessoryPolicy {
    private ClipboardAccessoryPolicy() {}
    public static boolean compactFits(int middleWidthPx, float density) {
        return density > 0 && middleWidthPx >= Math.round(244f * density);
    }
    public static boolean preferCompact(boolean hasTyped, boolean wordContent, boolean liveWords, boolean external) {
        return hasTyped && wordContent && liveWords && !external;
    }
    public static boolean keepFullForIdle(boolean hasTyped, int inputStyle) {
        return !hasTyped && (inputStyle == SuggestedWords.INPUT_STYLE_PREDICTION
            || inputStyle == SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION
            || inputStyle == SuggestedWords.INPUT_STYLE_NONE);
    }
}
