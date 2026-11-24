// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;
public final class LayerMotionPolicy {
    private LayerMotionPolicy() {}
    private static boolean alphabet(int i) { return i>=KeyboardId.ELEMENT_ALPHABET && i<=KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED; }
    private static boolean symbols(int i) { return i==KeyboardId.ELEMENT_SYMBOLS || i==KeyboardId.ELEMENT_SYMBOLS_SHIFTED; }
    public static boolean alphabetSymbolSwitch(int oldId,int newId) {
        return (alphabet(oldId)&&symbols(newId)) || (symbols(oldId)&&alphabet(newId));
    }
}
