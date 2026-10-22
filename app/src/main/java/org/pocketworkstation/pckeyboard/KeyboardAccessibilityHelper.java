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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.List;

/**
 * Exposes each key of a keyboard view to accessibility services such as TalkBack as a
 * virtual view with a spoken name. Keys can be activated by double-tap, or by lifting
 * the finger while exploring ("lift to type").
 */
class KeyboardAccessibilityHelper extends ExploreByTouchHelper {
    private final LatinKeyboardBaseView mView;
    private final Rect mTempRect = new Rect();
    private int mLastHoveredKey = INVALID_ID;

    KeyboardAccessibilityHelper(LatinKeyboardBaseView view) {
        super(view);
        mView = view;
    }

    private Keyboard.Key[] keys() {
        return mView.getKeysForAccessibility();
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        Keyboard.Key[] keys = keys();
        if (keys == null) return INVALID_ID;
        int kx = (int) x - mView.getPaddingLeft();
        int ky = (int) y - mView.getPaddingTop();
        for (int i = 0; i < keys.length; i++) {
            Keyboard.Key key = keys[i];
            if (kx >= key.x && kx < key.x + key.width && ky >= key.y && ky < key.y + key.height) {
                return i;
            }
        }
        return INVALID_ID;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        Keyboard.Key[] keys = keys();
        if (keys == null) return;
        for (int i = 0; i < keys.length; i++) virtualViewIds.add(i);
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId,
            AccessibilityNodeInfoCompat node) {
        Keyboard.Key[] keys = keys();
        if (keys == null || virtualViewId < 0 || virtualViewId >= keys.length) {
            // Must set some bounds and description even for stale ids.
            node.setContentDescription("");
            node.setBoundsInParent(new Rect(0, 0, 1, 1));
            return;
        }
        Keyboard.Key key = keys[virtualViewId];
        node.setContentDescription(describe(key));
        node.setClassName(android.widget.Button.class.getName());
        mTempRect.set(key.x, key.y, key.x + key.width, key.y + key.height);
        mTempRect.offset(mView.getPaddingLeft(), mView.getPaddingTop());
        node.setBoundsInParent(mTempRect);
        node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        if (key.sticky) {
            node.setCheckable(true);
            node.setChecked(key.on || key.locked);
        }
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
            Bundle arguments) {
        if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false;
        Keyboard.Key[] keys = keys();
        if (keys == null || virtualViewId < 0 || virtualViewId >= keys.length) return false;
        mView.sendKeyForAccessibility(keys[virtualViewId]);
        invalidateVirtualView(virtualViewId);
        return true;
    }

    /**
     * Lift to type: when the finger leaves the screen while exploring a key, type it.
     * Returns true if the event was handled.
     */
    boolean onHoverEvent(MotionEvent event) {
        // Hover events also come from mice and styluses; only act for touch exploration.
        AccessibilityManager am = (AccessibilityManager) mView.getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled() || !am.isTouchExplorationEnabled()) return false;
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_HOVER_ENTER:
        case MotionEvent.ACTION_HOVER_MOVE:
            mLastHoveredKey = getVirtualViewAt(event.getX(), event.getY());
            break;
        case MotionEvent.ACTION_HOVER_EXIT:
            int key = getVirtualViewAt(event.getX(), event.getY());
            if (key != INVALID_ID && key == mLastHoveredKey) {
                onPerformActionForVirtualView(key, AccessibilityNodeInfoCompat.ACTION_CLICK, null);
            }
            mLastHoveredKey = INVALID_ID;
            break;
        }
        return dispatchHoverEvent(event);
    }

    /** The spoken name of a key. */
    String describe(Keyboard.Key key) {
        Resources res = mView.getResources();
        int code = key.codes != null && key.codes.length > 0 ? key.codes[0] : 0;
        int nameId = 0;
        switch (code) {
        case Keyboard.KEYCODE_SHIFT: nameId = R.string.spoken_shift; break;
        case Keyboard.KEYCODE_DELETE: nameId = R.string.spoken_delete; break;
        case Keyboard.KEYCODE_MODE_CHANGE: nameId = R.string.spoken_symbols; break;
        case LatinKeyboardView.KEYCODE_OPTIONS: nameId = R.string.spoken_settings; break;
        case LatinKeyboardView.KEYCODE_VOICE: nameId = R.string.spoken_voice; break;
        case LatinKeyboardView.KEYCODE_DPAD_UP: nameId = R.string.spoken_up; break;
        case LatinKeyboardView.KEYCODE_DPAD_DOWN: nameId = R.string.spoken_down; break;
        case LatinKeyboardView.KEYCODE_DPAD_LEFT: nameId = R.string.spoken_left; break;
        case LatinKeyboardView.KEYCODE_DPAD_RIGHT: nameId = R.string.spoken_right; break;
        case LatinKeyboardView.KEYCODE_DPAD_CENTER: nameId = R.string.spoken_center; break;
        case LatinKeyboardView.KEYCODE_ESCAPE: nameId = R.string.spoken_escape; break;
        case LatinKeyboardView.KEYCODE_FORWARD_DEL: nameId = R.string.spoken_forward_delete; break;
        case LatinKeyboardView.KEYCODE_CTRL_LEFT: nameId = R.string.spoken_ctrl; break;
        case LatinKeyboardView.KEYCODE_ALT_LEFT: nameId = R.string.spoken_alt; break;
        case LatinKeyboardView.KEYCODE_META_LEFT: nameId = R.string.spoken_meta; break;
        case LatinKeyboardView.KEYCODE_FN: nameId = R.string.spoken_fn; break;
        case LatinKeyboardView.KEYCODE_PAGE_UP: nameId = R.string.spoken_page_up; break;
        case LatinKeyboardView.KEYCODE_PAGE_DOWN: nameId = R.string.spoken_page_down; break;
        case LatinKeyboardView.KEYCODE_HOME: nameId = R.string.spoken_home; break;
        case LatinKeyboardView.KEYCODE_END: nameId = R.string.spoken_end; break;
        case LatinKeyboardView.KEYCODE_INSERT: nameId = R.string.spoken_insert; break;
        case '\n': nameId = R.string.spoken_enter; break;
        case ' ': nameId = R.string.spoken_space; break;
        case '\t': nameId = R.string.spoken_tab; break;
        }
        String name;
        if (nameId != 0) {
            name = res.getString(nameId);
        } else {
            String label = key.getCaseLabel();
            if (!TextUtils.isEmpty(label)) {
                name = label;
            } else if (key.text != null) {
                name = key.text.toString();
            } else if (code > 0) {
                name = new String(Character.toChars(code));
            } else {
                name = res.getString(R.string.spoken_unknown_key);
            }
        }
        if (key.sticky && (key.on || key.locked)) {
            name = res.getString(key.locked ? R.string.spoken_key_locked : R.string.spoken_key_on,
                    name);
        }
        return name;
    }
}
