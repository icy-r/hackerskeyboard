/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

public class Main extends Activity {

    private final static String MARKET_URI = "market://search?q=pub:\"Klaus Weidner\"";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The layout has its own large title.
        if (getActionBar() != null) getActionBar().hide();
        setContentView(R.layout.main);
        SystemBarInsets.apply(this);
        String html = getString(R.string.main_body);
        html += "<p><i>Version: " + getString(R.string.auto_version) + "</i></p>";
        Spanned content = Html.fromHtml(html);
        TextView description = (TextView) findViewById(R.id.main_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);

        ((TextView) findViewById(R.id.main_step1_title)).setText(R.string.main_step1_title);
        ((TextView) findViewById(R.id.main_step1_summary)).setText(R.string.main_step1_summary);
        ((TextView) findViewById(R.id.main_step2_title)).setText(R.string.main_step2_title);
        ((TextView) findViewById(R.id.main_step2_summary)).setText(R.string.main_step2_summary);

        findViewById(R.id.main_step1_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
            }
        });

        findViewById(R.id.main_step2_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });

        final Activity that = this;

        findViewById(R.id.main_setup_btn_input_lang).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(that, InputLanguageSelection.class), 0);
            }
        });

        findViewById(R.id.main_setup_btn_get_dicts).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                try {
                    startActivity(it);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(
                                    R.string.no_market_warning), Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        findViewById(R.id.main_setup_btn_settings).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(that, LatinIMESettings.class), 0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSetupSteps();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The keyboard picker is a dialog, so this activity isn't resumed after it.
        if (hasFocus) updateSetupSteps();
    }

    private boolean isKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
            if (getPackageName().equals(info.getPackageName())) return true;
        }
        return false;
    }

    private boolean isKeyboardSelected() {
        String current = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        return current != null && current.startsWith(getPackageName() + "/");
    }

    private void updateSetupSteps() {
        boolean enabled = isKeyboardEnabled();
        boolean selected = enabled && isKeyboardSelected();
        updateStep(1, enabled, true, R.string.main_step1_button);
        updateStep(2, selected, enabled, R.string.main_step2_button);
    }

    private void updateStep(int step, boolean done, boolean available, int buttonText) {
        TextView badge = (TextView) findViewById(step == 1 ? R.id.main_step1_badge : R.id.main_step2_badge);
        Button button = (Button) findViewById(step == 1 ? R.id.main_step1_button : R.id.main_step2_button);
        badge.setText(done ? "\u2713" : Integer.toString(step));
        button.setText(done ? R.string.main_step_done : buttonText);
        // A finished step can still be revisited (e.g. to switch keyboards again).
        button.setEnabled(available);
        badge.setAlpha(available ? 1.0f : 0.4f);
    }
}
