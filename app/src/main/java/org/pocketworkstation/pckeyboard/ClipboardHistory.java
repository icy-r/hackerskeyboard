/*
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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Recently copied text, for pasting from the suggestion strip. Kept in memory only,
 * and clips marked as sensitive (for example by password managers) are never stored.
 */
class ClipboardHistory implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "PCKeyboard/Clipboard";
    static final int MAX_ITEMS = 10;
    private static final int MAX_LABEL_LENGTH = 32;
    // ClipDescription.EXTRA_IS_SENSITIVE, API 33; apps may set it on older versions too.
    private static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";

    private final ClipboardManager mClipboard;
    private final ArrayList<String> mItems = new ArrayList<String>();

    ClipboardHistory(Context context) {
        mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (mClipboard != null) mClipboard.addPrimaryClipChangedListener(this);
    }

    void destroy() {
        if (mClipboard != null) mClipboard.removePrimaryClipChangedListener(this);
    }

    @Override
    public void onPrimaryClipChanged() {
        addCurrentClip();
    }

    private void addCurrentClip() {
        if (mClipboard == null) return;
        ClipData clip;
        try {
            clip = mClipboard.getPrimaryClip();
        } catch (SecurityException e) {
            // Only the current default keyboard may read the clipboard in the background.
            Log.w(TAG, "Clipboard not readable: " + e);
            return;
        }
        if (clip == null || clip.getItemCount() == 0 || isSensitive(clip.getDescription())) {
            return;
        }
        CharSequence text = clip.getItemAt(0).getText();
        if (TextUtils.isEmpty(text)) return;
        String s = text.toString();
        mItems.remove(s);
        mItems.add(0, s);
        while (mItems.size() > MAX_ITEMS) mItems.remove(mItems.size() - 1);
    }

    private static boolean isSensitive(ClipDescription description) {
        if (description == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        PersistableBundle extras = description.getExtras();
        return extras != null && extras.getBoolean(EXTRA_IS_SENSITIVE, false);
    }

    /** Most recent first. */
    List<String> getItems() {
        addCurrentClip();
        return new ArrayList<String>(mItems);
    }

    /** A short single-line label for showing a clip in the suggestion strip. */
    static String label(String text) {
        String s = text.trim().replaceAll("\\s*\\n\\s*", " ⏎ ").replaceAll("\\s+", " ");
        if (s.length() > MAX_LABEL_LENGTH) s = s.substring(0, MAX_LABEL_LENGTH - 1) + "…";
        return s;
    }
}
