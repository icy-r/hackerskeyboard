package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.PersistableBundle;

import androidx.test.core.app.ApplicationProvider;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ClipboardHistoryTest {

    private ClipboardManager clipboard(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Test
    public void keepsRecentClipsNewestFirstWithoutDuplicates() {
        Context context = ApplicationProvider.getApplicationContext();
        ClipboardHistory history = new ClipboardHistory(context);
        ClipboardManager cm = clipboard(context);
        cm.setPrimaryClip(ClipData.newPlainText("", "one"));
        cm.setPrimaryClip(ClipData.newPlainText("", "two"));
        cm.setPrimaryClip(ClipData.newPlainText("", "one"));
        List<String> items = history.getItems();
        assertEquals(2, items.size());
        assertEquals("one", items.get(0));
        assertEquals("two", items.get(1));
        history.destroy();
    }

    @Test
    public void skipsSensitiveClips() {
        Context context = ApplicationProvider.getApplicationContext();
        ClipboardHistory history = new ClipboardHistory(context);
        ClipData clip = ClipData.newPlainText("", "hunter2");
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        clipboard(context).setPrimaryClip(clip);
        assertTrue(history.getItems().isEmpty());
        history.destroy();
    }

    @Test
    public void limitsHistorySize() {
        Context context = ApplicationProvider.getApplicationContext();
        ClipboardHistory history = new ClipboardHistory(context);
        for (int i = 0; i < 25; ++i) {
            clipboard(context).setPrimaryClip(ClipData.newPlainText("", "clip " + i));
        }
        List<String> items = history.getItems();
        assertEquals(ClipboardHistory.MAX_ITEMS, items.size());
        assertEquals("clip 24", items.get(0));
        history.destroy();
    }

    @Test
    public void labelsAreShortSingleLine() {
        assertEquals("ls -la ⏎ cd /tmp", ClipboardHistory.label("  ls -la\n  cd /tmp \n"));
        String label = ClipboardHistory.label("0123456789012345678901234567890123456789");
        assertEquals(32, label.length());
        assertTrue(label.endsWith("…"));
    }
}
