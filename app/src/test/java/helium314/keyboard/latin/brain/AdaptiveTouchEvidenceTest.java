package helium314.keyboard.latin.brain;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdaptiveTouchEvidenceTest {
    @Test public void binsReflectDirectionAndNoSequence() throws Exception {
        AdaptiveTouchEvidence e=new AdaptiveTouchEvidence();
        assertTrue(e.observe(4,0,0,0,false));
        assertTrue(e.observe(4,0,.75,-.75,true));
        assertEquals(1,e.countAt(4,12)); assertEquals(1,e.countAt(4,4));
        assertEquals(1,e.mismatch(4));
        assertFalse(e.observe(4,1,0,0,false));
        assertFalse(e.observe(4,0,Double.NaN,0,false));
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        e.writeTo(new DataOutputStream(b));
        assertEquals(26*27,b.size());
        AdaptiveTouchEvidence restored=AdaptiveTouchEvidence.readFrom(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));
        assertEquals(2,restored.count(4)); assertEquals(1,restored.mismatch(4));
        assertEquals(1,restored.countAt(4,4));
    }
    @Test public void mergeStaysBoundedAndCopyIsIndependent() {
        AdaptiveTouchEvidence e=new AdaptiveTouchEvidence(), batch=new AdaptiveTouchEvidence();
        for(int i=0;i<32;i++) assertTrue(batch.observe(7,3,-.2,.2,true));
        for(int i=0;i<100;i++) e.merge(batch);
        assertTrue(e.count(7)<=255); assertEquals(e.count(7),e.mismatch(7));
        AdaptiveTouchEvidence copy=e.copy(); int before=copy.count(7);
        e.observe(7,3,0,0,false);
        assertEquals(before,copy.count(7));
    }
    @Test public void legacyRecordKeepsOriginalModelAndDoesNotInventHeatmap() throws Exception {
        AdaptiveTouchModel model=new AdaptiveTouchModel();
        assertTrue(model.merge(2,32,6.4,-3.2));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x41545031); out.writeInt(1); out.writeLong(42L); model.writeTo(out);
        AdaptiveTouchRecord migrated=AdaptiveTouchRecord.read(bytes.toByteArray());
        assertEquals(model.snapshot().offsetX(2),migrated.models.get(42L).snapshot().offsetX(2),1e-9);
        assertEquals(0,migrated.evidence.get(42L).count(4));
        assertTrue(migrated.evidence.get(42L).observe(4,2,.4,-.4,true));
        AdaptiveTouchRecord reread=AdaptiveTouchRecord.read(AdaptiveTouchRecord.encode(migrated.models,migrated.evidence));
        assertEquals(1,reread.evidence.get(42L).mismatch(4));
        assertEquals(32,reread.models.get(42L).snapshot().evidence(2));
        byte[] invalid=java.util.Arrays.copyOf(AdaptiveTouchRecord.encode(migrated.models,migrated.evidence),100);
        try { AdaptiveTouchRecord.read(invalid); fail("truncated record"); }
        catch(IOException expected) {}
    }
    @Test public void incompatibleLayoutDoesNotPartiallyMerge() {
        AdaptiveTouchEvidence e=new AdaptiveTouchEvidence(), batch=new AdaptiveTouchEvidence();
        e.observe(4,1,0,0,false); batch.observe(4,2,0,0,false); batch.observe(0,0,0,0,false);
        assertFalse(e.compatible(batch));
        try { e.merge(batch); fail("incompatible region"); } catch(IllegalArgumentException expected) {}
        assertEquals(0,e.count(0));
    }
    @Test public void fullStoreFitsExistingAuthenticatedVaultLimit() {
        assertTrue(8+12*(8+6*(4+8+8)+26*27)<16*1024-64);
    }
}
