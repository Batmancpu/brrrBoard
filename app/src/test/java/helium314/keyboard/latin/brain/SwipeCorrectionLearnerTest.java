// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import androidx.test.core.app.ApplicationProvider;

import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.utils.KtxKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public final class SwipeCorrectionLearnerTest {
    @Test public void needsRepeatedCorrectionsAndNeverPromotesUnrelatedWords() {
        final Context context = ApplicationProvider.getApplicationContext();
        final SwipeCorrectionLearner learner = SwipeCorrectionLearner.get(context);
        learner.clear();
        assertNull(learner.suggest("hapy"));
        learner.confirm("hapy", "happy");
        assertNull("single accidental correction must have no effect", learner.suggest("hapy"));
        learner.confirm("hapy", "happy");
        assertEquals("happy", learner.suggest("hapy"));
        learner.confirm("hapy", "birthday");
        learner.confirm("hapy", "birthday");
        assertEquals("happy", learner.suggest("hapy"));
        learner.clear();
        assertNull(learner.suggest("hapy"));
    }

    @Test public void snapshotShowsOnlyMeasuredLearningAndReviewablePairs() {
        final Context context = ApplicationProvider.getApplicationContext();
        final SwipeCorrectionLearner learner = SwipeCorrectionLearner.get(context);
        learner.clear();
        final long beforeSwipes = learner.snapshot().swipesThisSession;
        learner.recordSwipeAttempt();
        learner.confirm("hapy", "happy");
        final SwipeCorrectionLearner.Snapshot first = learner.snapshot();
        assertEquals(beforeSwipes + 1, first.swipesThisSession);
        assertTrue(first.storedPairs >= 1);
        assertEquals(0, first.readyPairs);
        assertTrue(first.recentAssociations.get(0).contains("happy"));
        learner.confirm("hapy", "happy");
        assertEquals(1, learner.snapshot().readyPairs);
        assertEquals("happy", learner.suggest("hapy"));
        assertTrue(learner.snapshot().recallOffersThisSession >= 1);
        learner.clear();
        assertEquals(0, learner.snapshot().storedPairs);
    }

    @Test public void isOptInAndBlockedInPrivateEditors() {
        final Context context = ApplicationProvider.getApplicationContext();
        final SharedPreferences prefs = KtxKt.prefs(context);
        prefs.edit().putBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)
                .putBoolean(Settings.PREF_LEARN_SWIPE_CORRECTIONS, false).commit();
        final EditorInfo editor = new EditorInfo();
        editor.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
        assertFalse(SwipeCorrectionLearner.isAllowed(context, editor, false, true));
        prefs.edit().putBoolean(Settings.PREF_LEARN_SWIPE_CORRECTIONS, true).commit();
        assertTrue(SwipeCorrectionLearner.isAllowed(context, editor, false, true));
        assertFalse(SwipeCorrectionLearner.isAllowed(context, editor, true, true));
        assertFalse(SwipeCorrectionLearner.isAllowed(context, editor, false, false));
        editor.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertFalse(SwipeCorrectionLearner.isAllowed(context, editor, false, true));
        editor.imeOptions = 0;
        editor.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        assertFalse(SwipeCorrectionLearner.isAllowed(context, editor, false, true));
        prefs.edit().remove(Settings.PREF_LEARN_SWIPE_CORRECTIONS).apply();
    }
}
