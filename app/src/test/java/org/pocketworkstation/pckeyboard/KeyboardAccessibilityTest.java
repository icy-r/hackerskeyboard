package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import androidx.test.core.app.ApplicationProvider;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w411dp-h914dp-port-xxhdpi")
public class KeyboardAccessibilityTest {

    private final List<Integer> mKeys = new ArrayList<Integer>();
    private LatinKeyboardView mView;
    private Keyboard.Key[] mKeyArray;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mView = (LatinKeyboardView) LayoutInflater.from(context)
                .inflate(R.layout.input_material_you, null);
        LatinKeyboard keyboard = new LatinKeyboard(context, R.xml.kbd_full,
                KeyboardSwitcher.KEYBOARDMODE_NORMAL, 35.0f);
        mView.setKeyboard(keyboard);
        mView.setOnKeyboardActionListener(new LatinKeyboardBaseView.OnKeyboardActionListener() {
            public void onPress(int primaryCode) {}
            public void onRelease(int primaryCode) {}
            public void onKey(int primaryCode, int[] keyCodes, int x, int y) { mKeys.add(primaryCode); }
            public void onText(CharSequence text) {}
            public void onCancel() {}
            public boolean swipeLeft() { return false; }
            public boolean swipeRight() { return false; }
            public boolean swipeDown() { return false; }
            public boolean swipeUp() { return false; }
        });
        int width = context.getResources().getDisplayMetrics().widthPixels;
        mView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
        mKeyArray = mView.getKeysForAccessibility();
    }

    private int indexOf(int code) {
        for (int i = 0; i < mKeyArray.length; i++) {
            if (mKeyArray[i].codes[0] == code) return i;
        }
        throw new AssertionError("no key " + code);
    }

    @Test
    public void keysHaveSpokenNames() {
        AccessibilityNodeProvider provider = mView.getAccessibilityNodeProvider();
        assertNotNull(provider);
        assertEquals("Escape", provider.createAccessibilityNodeInfo(
                indexOf(LatinKeyboardView.KEYCODE_ESCAPE)).getContentDescription().toString());
        assertEquals("Control", provider.createAccessibilityNodeInfo(
                indexOf(LatinKeyboardView.KEYCODE_CTRL_LEFT)).getContentDescription().toString());
        assertEquals("Left arrow", provider.createAccessibilityNodeInfo(
                indexOf(LatinKeyboardView.KEYCODE_DPAD_LEFT)).getContentDescription().toString());
        assertEquals("Space", provider.createAccessibilityNodeInfo(
                indexOf(' ')).getContentDescription().toString());
        assertEquals("q", provider.createAccessibilityNodeInfo(
                indexOf('q')).getContentDescription().toString());
    }

    @Test
    public void clickTypesKey() {
        AccessibilityNodeProvider provider = mView.getAccessibilityNodeProvider();
        int q = indexOf('q');
        AccessibilityNodeInfo node = provider.createAccessibilityNodeInfo(q);
        assertTrue(node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK));
        assertTrue(provider.performAction(q, AccessibilityNodeInfo.ACTION_CLICK, null));
        assertEquals(1, mKeys.size());
        assertEquals((int) 'q', (int) mKeys.get(0));
    }
}
