package helium314.keyboard.latin.suggestions;
import org.junit.Test;
import static org.junit.Assert.*;
import helium314.keyboard.latin.SuggestedWords;
public class ClipboardAccessoryPolicyTest {
    @Test public void chromeHasAFullSizeTouchSlot() {
        assertEquals(44, ClipboardAccessoryPolicy.CHROME_SLOT_DP);
    }
    @Test public void protectsWordSlotsOnNarrowKeyboards() {
        assertFalse(ClipboardAccessoryPolicy.compactFits(0, 3));
        assertFalse(ClipboardAccessoryPolicy.compactFits(731, 3));
        assertTrue(ClipboardAccessoryPolicy.compactFits(732, 3));
        assertTrue(ClipboardAccessoryPolicy.compactFits(980, 3));
    }
    @Test public void disabledWordsCannotKeepCompactVisibleIndefinitely() {
        assertTrue(ClipboardAccessoryPolicy.preferCompact(true, true, true, false));
        assertFalse(ClipboardAccessoryPolicy.preferCompact(true, true, false, false));
        assertFalse(ClipboardAccessoryPolicy.preferCompact(true, false, true, false));
        assertFalse(ClipboardAccessoryPolicy.preferCompact(true, true, true, true));
        assertFalse(ClipboardAccessoryPolicy.preferCompact(false, true, true, false));
    }
    @Test public void idlePredictionsDoNotHideFreshClipboard() {
        assertTrue(ClipboardAccessoryPolicy.keepFullForIdle(false, SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION));
        assertTrue(ClipboardAccessoryPolicy.keepFullForIdle(false, SuggestedWords.INPUT_STYLE_PREDICTION));
        assertFalse(ClipboardAccessoryPolicy.keepFullForIdle(false, SuggestedWords.INPUT_STYLE_TYPING));
        assertFalse(ClipboardAccessoryPolicy.keepFullForIdle(false, SuggestedWords.INPUT_STYLE_UPDATE_BATCH));
        assertFalse(ClipboardAccessoryPolicy.keepFullForIdle(false, SuggestedWords.INPUT_STYLE_APPLICATION_SPECIFIED));
        assertFalse(ClipboardAccessoryPolicy.keepFullForIdle(true, SuggestedWords.INPUT_STYLE_PREDICTION));
    }
}
