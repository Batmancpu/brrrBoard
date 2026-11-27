// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;
import org.junit.Test;
import static org.junit.Assert.*;
public class PanelMotionPolicyTest {
    @Test public void clipboardAlwaysSwitchesImmediately() {
        assertFalse(PanelMotionPolicy.canMorph(true,false,false,true,true));
        assertFalse(PanelMotionPolicy.canMorph(true,true,false,true,true));
        assertFalse(PanelMotionPolicy.canMorph(true,false,true,true,true));
        assertFalse(PanelMotionPolicy.supportsArtwork(false,false,true));
        assertFalse(PanelMotionPolicy.supportsArtwork(true,false,true));
    }
    @Test public void emojiBrowseStillMorphsInBothDirections() {
        assertTrue(PanelMotionPolicy.canMorph(true,true,false,false,true));
        assertTrue(PanelMotionPolicy.canMorph(true,false,true,false,true));
        assertFalse(PanelMotionPolicy.canMorph(false,true,false,false,true));
        assertFalse(PanelMotionPolicy.canMorph(true,true,false,false,false));
        assertFalse(PanelMotionPolicy.canMorph(true,false,false,false,true));
        assertTrue(PanelMotionPolicy.supportsArtwork(true,false,false));
        assertTrue(PanelMotionPolicy.supportsArtwork(false,true,false));
    }
    @Test public void speculativeWarmupSkipsConstrainedDevices() {
        assertTrue(PanelMotionPolicy.canPrewarm(false,false));
        assertFalse(PanelMotionPolicy.canPrewarm(true,false));
        assertFalse(PanelMotionPolicy.canPrewarm(false,true));
    }
}
