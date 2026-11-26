package helium314.keyboard.latin.inputlogic;
import org.junit.Test;
import static org.junit.Assert.*;
public class CorrectionSessionMemoryTest {
    @Test public void strictExpiryAndIdentity() {
        CorrectionSessionMemory m=new CorrectionSessionMemory(); Object c=new Object();
        assertTrue(m.remember("teh","the",10,100,7,c));
        assertEquals("teh", m.find(10,13,"the",101,7,c).original());
        assertEquals("the", m.find(10,13,"the",101,7,c).corrected());
        assertNull(m.find(11,14,"the",101,7,c));
        assertNull(m.find(10,13,"too",101,7,c));
        assertEquals("teh",m.find(10,13,"the",30099,7,c).original());
        assertNull(m.find(10,13,"the",30100,7,c));
        assertTrue(m.remember("teh","the",10,100,7,c));
        assertNull(m.find(10,13,"the",101,8,c));
        assertTrue(m.remember("teh","the",10,100,7,c));
        assertNull(m.find(10,13,"the",101,7,new Object()));
        assertTrue(m.remember("teh","the",10,100,7,c));
        assertNull(m.find(10,13,"the",99,7,c));
    }
    @Test public void editAndBoundedReceipts() {
        CorrectionSessionMemory m=new CorrectionSessionMemory(); Object c=new Object();
        for(int i=0;i<9;i++) assertTrue(m.remember("teh","the",i*5,100+i,1,c));
        assertNull(m.find(0,3,"the",109,1,c));
        assertNotNull(m.find(5,8,"the",109,1,c));
        m.invalidateFrom(20);
        assertNotNull(m.find(15,18,"the",109,1,c));
        assertNull(m.find(20,23,"the",109,1,c));
        m.clear(); assertEquals(Long.MAX_VALUE,m.nextExpiry(109));
        assertFalse(m.remember("1234","1235",0,110,1,c));
        assertFalse(m.remember("x@y","z@y",0,110,1,c));
    }
}
