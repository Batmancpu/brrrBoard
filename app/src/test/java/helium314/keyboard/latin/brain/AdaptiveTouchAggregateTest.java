package helium314.keyboard.latin.brain;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
public class AdaptiveTouchAggregateTest {
    @Test public void roundTripAndBoundedMerge() throws Exception {
        AdaptiveTouchModel m = new AdaptiveTouchModel();
        assertTrue(m.merge(0,32,6.4,-3.2));
        assertFalse(m.merge(0,33,0,0));
        assertFalse(m.merge(0,1,2,0));
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        m.writeTo(new DataOutputStream(buf));
        AdaptiveTouchModel copy = AdaptiveTouchModel.readFrom(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));
        assertEquals(m.snapshot().offsetX(0),copy.snapshot().offsetX(0),0.000001);
        for(int i=0;i<100;i++) assertTrue(copy.merge(0,32,6.4,0));
        assertEquals(512,copy.snapshot().evidence(0));
        assertTrue(copy.snapshot().offsetX(0)<=.18);
    }
}
