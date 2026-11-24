package helium314.keyboard.latin.inputlogic;
import org.junit.Test;
import static org.junit.Assert.*;
public class EditorEditJournalTest {
    @Test public void exactExpiryUndoRedo(){
        EditorEditJournal j=new EditorEditJournal();Object c=new Object();
        assertTrue(j.remember(3,"abc","","pre","post",100,2,c));
        assertEquals("abc",j.next(false,30099,2,c).before());
        j.applied(false);assertEquals("",j.next(true,30099,2,c).after());
        j.applied(true);assertNull(j.next(false,30100,2,c));
    }
    @Test public void newEditInvalidatesRedoAndEditorIsolation(){
        EditorEditJournal j=new EditorEditJournal();Object c=new Object();
        assertTrue(j.remember(0,"a","b","","",100,1,c));
        j.applied(false);
        assertTrue(j.remember(0,"a","c","","",110,1,c));
        assertNull(j.next(true,120,1,c));
        assertEquals("c",j.next(false,120,1,c).after());
        assertNull(j.next(false,120,2,c));
    }
}
