package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Renders the keyboard view for each theme to PNG files under
 * app/build/screenshots, so visual changes can be reviewed without a device.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, qualifiers = "w411dp-h914dp-port-xxhdpi")
public class KeyboardRenderTest {

    private static final int[] THEMES = {
        R.layout.input_material_you,
        R.layout.input_ics,
        R.layout.input_material_dark,
        R.layout.input_material_light,
        R.layout.input_material_black,
    };

    private File render(Context context, int layoutId, int xmlId, String name) throws Exception {
        // Match the app's default "show ASCII punctuation hints" mode.
        LatinIME.sKeyboardSettings.hintMode = Integer.parseInt(
                context.getString(R.string.default_hint_mode));
        LatinKeyboardView view = (LatinKeyboardView) LayoutInflater.from(context)
                .inflate(layoutId, null);
        LatinKeyboard keyboard = new LatinKeyboard(context, xmlId,
                KeyboardSwitcher.KEYBOARDMODE_NORMAL, 35.0f);
        view.setKeyboard(keyboard);

        int width = context.getResources().getDisplayMetrics().widthPixels;
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        File dir = new File("build/screenshots");
        dir.mkdirs();
        File out = new File(dir, name + ".png");
        try (FileOutputStream os = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
        return out;
    }

    @Test
    public void renderThemes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Context themed = context;
        for (int layoutId : THEMES) {
            String name = context.getResources().getResourceEntryName(layoutId);
            assertTrue(render(themed, layoutId, R.xml.kbd_full, name + "_full").length() > 0);
            assertTrue(render(themed, layoutId, R.xml.kbd_qwerty, name + "_4row").length() > 0);
        }
    }

    private void renderActivity(String name) throws Exception {
        android.app.Activity activity = Robolectric.buildActivity(Main.class).setup().get();
        View root = activity.getWindow().getDecorView();
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir = new File("build/screenshots");
        dir.mkdirs();
        try (FileOutputStream os = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
    }

    @Test
    public void renderSetupScreen() throws Exception {
        renderActivity("setup_light");
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp-port-night-xxhdpi")
    public void renderSetupScreenNight() throws Exception {
        renderActivity("setup_night");
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp-port-night-xxhdpi")
    public void renderNight() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(render(context, R.layout.input_material_you, R.xml.kbd_full,
                "input_material_you_night_full").length() > 0);
    }
}
