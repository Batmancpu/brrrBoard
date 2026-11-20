package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditorSessionGenerationTest {
    @Test public void staleEditorAndPrivacyCallbacksAreRejected() {
        final EditorSessionGeneration session = new EditorSessionGeneration();
        final long original = session.current();
        assertTrue(session.accepts(original));
        final long switched = session.invalidate();
        assertFalse(session.accepts(original));
        assertTrue(session.accepts(switched));
        session.invalidate(); // incognito toggle, hide or editor restart
        assertFalse(session.accepts(switched));
    }
}
