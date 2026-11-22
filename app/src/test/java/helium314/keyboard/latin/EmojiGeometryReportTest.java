// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EmojiGeometryReportTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @After public void cleanup() {
        EmojiGeometryReport.erase(context);
        final long deadline = android.os.SystemClock.uptimeMillis() + 2000;
        while (EmojiGeometryReport.hasReport(context)
                && android.os.SystemClock.uptimeMillis() < deadline) {
            try { Thread.sleep(10); }
            catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Test public void storesOnlyGeometryProtocolAndCopiesOnlyOnRequest() {
        Assert.assertFalse(EmojiGeometryReport.hasReport(context));
        EmojiGeometryReport.record(context, "typed text must not pass this protocol");
        EmojiGeometryReport.record(context, "geometry-only host=some.app");
        EmojiGeometryReport.record(context, "geometry-only frame=private-text");
        Assert.assertFalse(EmojiGeometryReport.hasReport(context));

        EmojiGeometryReport.record(context,
                "geometry-only input=100,200:400x300 measured=400x300 localY=0.0 "
                + "translationY=0.0 scrollY=0 padding=0,0,0,0 parent=400x300 "
                + "parentPadding=0,0,0,0 layout=-1x-1 margins=0,0,0,0 "
                + "windowInsets=bars:0,0 ime=0,800 gestures=0,24 orientation=1 density=3.0 "
                + "panelSearch=false keyboardState=EMOJI toolbarMode=EXPANDABLE");
        awaitReport();
        Assert.assertTrue(EmojiGeometryReport.hasReport(context));
        Assert.assertTrue(EmojiGeometryReport.copyLatest(context));
    }

    @Test public void eraseRemovesTheLastDeviceSample() {
        EmojiGeometryReport.record(context, "geometry-only input=100x300");
        awaitReport();
        Assert.assertTrue(EmojiGeometryReport.hasReport(context));
        EmojiGeometryReport.erase(context);
        Assert.assertFalse(EmojiGeometryReport.hasReport(context));
    }

    private static void awaitReport() {
        final long deadline = android.os.SystemClock.uptimeMillis() + 2000;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            if (EmojiGeometryReport.hasReport(ApplicationProvider.getApplicationContext())) return;
            try { Thread.sleep(10); }
            catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
