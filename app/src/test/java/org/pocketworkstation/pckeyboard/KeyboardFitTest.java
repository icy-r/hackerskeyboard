package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Landscape with a side navigation bar: the keyboard window is narrower than the
 * display, and every key must still fit inside it.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w914dp-h411dp-land-xxhdpi")
public class KeyboardFitTest {

    @Test
    public void keysFitNarrowerWindow() {
        Context context = ApplicationProvider.getApplicationContext();
        LatinKeyboardView view = (LatinKeyboardView) LayoutInflater.from(context)
                .inflate(R.layout.input_material_you, null);
        view.setPadding(0, 0, 0, 0);
        LatinKeyboard keyboard = new LatinKeyboard(context, R.xml.kbd_full,
                KeyboardSwitcher.KEYBOARDMODE_NORMAL, 55.0f);
        view.setKeyboard(keyboard);

        int display = context.getResources().getDisplayMetrics().widthPixels;
        int window = display - 48 * 3; // 48dp navigation bar at xxhdpi
        view.measure(View.MeasureSpec.makeMeasureSpec(window, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        assertEquals(window, view.getMeasuredWidth());
        int widest = 0;
        for (Keyboard.Key key : keyboard.getKeys()) {
            assertTrue("key " + key + " ends at " + (key.x + key.width) + " > " + window,
                    key.x + key.width <= window + 1);
            widest = Math.max(widest, key.x + key.width);
        }
        // The keys use the available width rather than leaving a gap.
        assertTrue("keys end at " + widest, widest >= window - 10);

        // Save a picture for review, with the "navigation bar" area shown in grey.
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        android.graphics.Bitmap keys = android.graphics.Bitmap.createBitmap(
                view.getMeasuredWidth(), view.getMeasuredHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(keys));
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(display,
                view.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        canvas.drawColor(0xFF808080);
        canvas.drawBitmap(keys, 0, 0, null);
        java.io.File dir = new java.io.File("build/screenshots");
        dir.mkdirs();
        try (java.io.FileOutputStream os = new java.io.FileOutputStream(
                new java.io.File(dir, "landscape_side_navbar.png"))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void fullWidthWindowUnchanged() {
        Context context = ApplicationProvider.getApplicationContext();
        LatinKeyboardView view = (LatinKeyboardView) LayoutInflater.from(context)
                .inflate(R.layout.input_material_you, null);
        view.setPadding(0, 0, 0, 0);
        LatinKeyboard keyboard = new LatinKeyboard(context, R.xml.kbd_full,
                KeyboardSwitcher.KEYBOARDMODE_NORMAL, 55.0f);
        view.setKeyboard(keyboard);
        int before = keyboard.getMinWidth();
        int display = context.getResources().getDisplayMetrics().widthPixels;
        view.measure(View.MeasureSpec.makeMeasureSpec(display, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        assertEquals(before, keyboard.getMinWidth());
    }
}
