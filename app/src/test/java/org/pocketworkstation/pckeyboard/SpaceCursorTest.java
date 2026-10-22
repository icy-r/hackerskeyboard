package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

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
public class SpaceCursorTest {

    private final List<Integer> mKeys = new ArrayList<Integer>();
    private LatinKeyboardView mView;
    private Keyboard.Key mSpace;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        LatinIME.sKeyboardSettings.spaceSwipeMode = GlobalKeyboardSettings.SPACE_SWIPE_AUTO;
        mView = (LatinKeyboardView) LayoutInflater.from(context)
                .inflate(R.layout.input_material_you, null);
        LatinKeyboard keyboard = new LatinKeyboard(context, R.xml.kbd_qwerty,
                KeyboardSwitcher.KEYBOARDMODE_NORMAL, 35.0f);
        mView.setKeyboard(keyboard);
        mView.setPadding(0, 0, 0, 0);
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
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == ' ') mSpace = key;
        }
    }

    private void touch(int action, float x, float y, long downTime) {
        MotionEvent e = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
        mView.onTouchEvent(e);
        e.recycle();
    }

    @Test
    public void slidingOnSpaceMovesCursor() {
        float density = mView.getResources().getDisplayMetrics().density;
        float x = mSpace.x + mSpace.width / 4f;
        float y = mSpace.y + mSpace.height / 2f;
        long down = SystemClock.uptimeMillis();
        touch(MotionEvent.ACTION_DOWN, x, y, down);
        for (int i = 1; i <= 10; ++i) {
            touch(MotionEvent.ACTION_MOVE, x + i * 8 * density, y, down);
        }
        touch(MotionEvent.ACTION_UP, x + 80 * density, y, down);

        assertFalse("space must not be typed: " + mKeys, mKeys.contains((int) ' '));
        assertTrue("cursor moved right: " + mKeys,
                mKeys.contains(LatinKeyboardView.KEYCODE_DPAD_RIGHT));
        // 80dp of travel, 16dp to start the slide, then one step per 12dp.
        int rights = 0;
        for (int k : mKeys) if (k == LatinKeyboardView.KEYCODE_DPAD_RIGHT) rights++;
        assertEquals(5, rights);
    }

    @Test
    public void tapOnSpaceTypesSpace() {
        float x = mSpace.x + mSpace.width / 2f;
        float y = mSpace.y + mSpace.height / 2f;
        long down = SystemClock.uptimeMillis();
        touch(MotionEvent.ACTION_DOWN, x, y, down);
        touch(MotionEvent.ACTION_UP, x, y, down);
        assertEquals(1, mKeys.size());
        assertEquals((int) ' ', (int) mKeys.get(0));
    }

    @Test
    public void slideDisabledTypesSpace() {
        LatinIME.sKeyboardSettings.spaceSwipeMode = GlobalKeyboardSettings.SPACE_SWIPE_OFF;
        float density = mView.getResources().getDisplayMetrics().density;
        float x = mSpace.x + mSpace.width / 4f;
        float y = mSpace.y + mSpace.height / 2f;
        long down = SystemClock.uptimeMillis();
        touch(MotionEvent.ACTION_DOWN, x, y, down);
        touch(MotionEvent.ACTION_MOVE, x + 40 * density, y, down);
        touch(MotionEvent.ACTION_UP, x + 40 * density, y, down);
        assertFalse(mKeys.contains(LatinKeyboardView.KEYCODE_DPAD_RIGHT));
        LatinIME.sKeyboardSettings.spaceSwipeMode = GlobalKeyboardSettings.SPACE_SWIPE_AUTO;
    }
}
