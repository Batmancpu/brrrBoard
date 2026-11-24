package helium314.keyboard.keyboard;
import org.junit.Test;
import static org.junit.Assert.*;
public class LayerMotionPolicyTest {
    @Test public void onlyAlphabetSymbolCrossing() {
        assertTrue(LayerMotionPolicy.alphabetSymbolSwitch(KeyboardId.ELEMENT_ALPHABET,KeyboardId.ELEMENT_SYMBOLS));
        assertTrue(LayerMotionPolicy.alphabetSymbolSwitch(KeyboardId.ELEMENT_SYMBOLS_SHIFTED,KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED));
        assertFalse(LayerMotionPolicy.alphabetSymbolSwitch(KeyboardId.ELEMENT_ALPHABET,KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED));
        assertFalse(LayerMotionPolicy.alphabetSymbolSwitch(KeyboardId.ELEMENT_ALPHABET,KeyboardId.ELEMENT_NUMPAD));
        assertFalse(LayerMotionPolicy.alphabetSymbolSwitch(KeyboardId.ELEMENT_SYMBOLS,KeyboardId.ELEMENT_EMOJI_RECENTS));
    }
}
