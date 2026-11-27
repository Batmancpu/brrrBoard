// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;
import org.junit.Test;
import static org.junit.Assert.*;
public class MotionContinuityTest {
    @Test public void reversingPreservesPositionAndFiniteBoundedMomentum() {
        assertEquals(60f,MotionContinuity.position(60,100,-300,0,180),0f);
        assertEquals(100f,MotionContinuity.position(60,100,-300,1,180),0f);
        assertTrue(MotionContinuity.position(60,100,-300,.01f,180)<60f);
        for(int i=0;i<=100;i++) {
            float x=MotionContinuity.position(60,100,-10000,i/100f,180);
            assertTrue(x>=57f&&x<=100.001f);
        }
    }
    @Test public void taperAndSlotTravelRemainBounded() {
        assertEquals(20f,MotionContinuity.taper(20,1.6f,-1),0f);
        assertEquals(1.6f,MotionContinuity.taper(20,1.6f,2),.001f);
        assertEquals(16f,MotionContinuity.slotOffset(1000,0,100),.001f);
        assertEquals(-16f,MotionContinuity.slotOffset(0,1000,100),.001f);
        assertEquals(0f,MotionContinuity.slotOffset(10,10,100),0f);
    }
}
