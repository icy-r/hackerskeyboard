# Modernizing Hacker's Keyboard for current phones — UI/UX research

Status: research notes (October 2026). Nothing in this document is implemented yet.

The keyboard is a 2011 fork of the Gingerbread AOSP LatinIME. It still has the
best desktop-style layout on Android (5 rows, Esc/Tab/Ctrl/Alt/arrows/F-keys),
but the platform, the hardware and user expectations have moved a long way.
This document records what the code does today, where that hurts on modern
phones, and a prioritized plan to fix it.

---

## 1. Where the code is today

| Area | Current state | File |
|---|---|---|
| Build | AGP 3.2.1, `jcenter()`, Support Library 27, `compileSdk/targetSdk 26`, `minSdk 14` | `build.gradle`, `app/build.gradle` |
| Rendering | `android:hardwareAccelerated="false"` app-wide; whole keyboard drawn into a software `Bitmap` buffer (`mBuffer`, `onBufferDraw`) | `AndroidManifest.xml`, `LatinKeyboardBaseView.java:263-900` |
| Themes | 8 fixed themes (ICS, Material black/dark/light, Gingerbread, Stone, Neon) built from 9‑patch PNGs; no day/night switching, no dynamic color | `res/layout/input_*.xml`, `res/drawable-*` |
| Key previews / long-press popups | `PopupWindow`s; clipping is forced on for API 28+, so popups are squeezed inside the keyboard area (the "popup keys don't work right" issue in the README) | `LatinKeyboardBaseView.java:158-175`, `CandidateView.java:111-114` |
| Languages | `res/xml/method.xml` declares **zero `<subtype>`s**; language switching is custom (`LanguageSwitcher`). The system language/IME switcher therefore can't see languages (the README's "language switching doesn't work") | `res/xml/method.xml`, `LanguageSwitcher.java` |
| Keyboard height | Percent of screen height (35% portrait / 55% landscape) | `res/values/size-defaults.xml`, `LatinIME.java:450-460` |
| Key sizes | Defined in inches (`key_height 0.290in`, text `0.11in`) | `res/values/dimens.xml` |
| Haptics | `Vibrator.vibrate(ms)` with a fixed duration; `performHapticFeedback` only as fallback | `LatinIME.java:3304-3323` |
| Accessibility | None. No `AccessibilityDelegate`/node provider, no content descriptions; TalkBack users cannot use the keyboard | (grep finds nothing) |
| Settings | Five separate `PreferenceActivity` screens (deprecated framework API), plain Holo look | `LatinIMESettings.java`, `PrefScreen*.java` |
| Setup | `Main` activity: a `TableLayout` of six buttons plus an HTML blurb | `Main.java`, `res/layout/main.xml` |
| Notification | `PendingIntent` flags `0`, unexported-receiver flags missing, no `POST_NOTIFICATIONS` | `LatinIME.java:480-540` |
| Voice | Legacy `com.google.android.voiceime` trigger | `LatinIME.java:21` |

---

## 2. What changed on phones since 2011, and why it matters here

1. **Tall 19.5:9–21:9 screens.** 35% of a 2400 px tall screen is very tall,
   and on a 6.1" phone a 5-row full layout makes keys about 6 mm wide. Sizing
   by percent of screen height gives poor results at both ends.
2. **Gesture navigation.** The bottom gesture handle sits right under the
   space bar. Keyboards now reserve the navigation-bar inset and tint it to
   match the keyboard; the system draws the IME back/switch buttons there.
3. **Foldables, tablets and desktop mode.** Window sizes change at runtime
   (unfold, split screen, freeform/desktop windows on Android 16). One-handed,
   floating and split layouts are now standard keyboard features.
4. **Material You (Android 12+).** Users expect the keyboard to follow
   wallpaper colors and system dark mode automatically.
5. **Haptics hardware.** Modern linear actuators feel best with the system's
   tuned `KEYBOARD_TAP`/`KEYBOARD_PRESS` effects. A fixed buzz of N ms
   feels mushy and ignores the user's system haptics setting.
6. **Platform policy.** From **31 Aug 2026, new apps and updates on Google Play
   must target API 36 (Android 16)**, and existing apps must target at least
   API 35 to stay visible to new users on newer Android versions
   ([Android developers](https://developer.android.com/google/play/requirements/target-sdk),
   [Play Console help](https://support.google.com/googleplay/android-developer/answer/11926878)).
   The app is at 26, so this is a hard blocker for shipping any UI work at all.

---

## 3. Recommendations, prioritized

### P0 — Platform baseline (blocks everything else)

These aren't visible features, but without them the app can't ship to Play,
and several current UX bugs come from them.

- **Toolchain:** move to Gradle 8.x/AGP 8.x, `mavenCentral()` instead of the
  dead `jcenter()`, AndroidX (`androidx.core`, `appcompat`, `preference`),
  `compileSdk/targetSdk 36`. Raise `minSdk` to 23 or 26; this removes most
  reflection and version hacks (e.g. the reflective `setLayerType` lookup at
  `LatinKeyboardBaseView.java:446`).
- **Crash-level fixes needed once you target ≥31/33/34:**
  - `PendingIntent.getBroadcast(..., 0)` → add `FLAG_IMMUTABLE` (required from API 31, crashes otherwise).
  - `registerReceiver(...)` for app-internal broadcasts → `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` (required from API 34).
  - Request `POST_NOTIFICATIONS` (API 33) before enabling the "show keyboard" notification, or drop that feature.
- **Remove `android:hardwareAccelerated="false"`** and the software bitmap
  buffer. Draw keys directly with HW acceleration and invalidate only the
  rects of changed keys. This gives smoother 90/120 Hz feedback, less memory
  (a full-screen-width ARGB buffer is several MB on a 1440p phone), and lets
  you use shadows, rounded corners and animations for free.
- **Declare IME subtypes** in `method.xml`, one per supported language/layout,
  and map them onto `LanguageSwitcher`. Then implement
  `onCurrentInputMethodSubtypeChanged`, and use
  `shouldOfferSwitchingToNextInputMethod()` / `switchToNextInputMethod()` for
  the globe key. This fixes system-level language switching and makes
  languages show up in Android's settings and in the nav-bar IME switcher.
- **Fix popups:** stop relying on `PopupWindow` drawing outside the input
  view. The modern AOSP approach is to make the input view taller than the
  keys (a transparent "preview area" above the top row) and draw key
  previews and long-press panels inside it. In `onComputeInsets` set
  `touchableInsets = TOUCHABLE_INSETS_REGION` so the transparent part passes
  touches through to the app. This fixes the clipped long-press popups on
  Android 9+.
- **Navigation-bar insets:** read the navigation-bar inset (`WindowInsets`) in
  the input view, add it as bottom padding below the last key row, and paint
  that padding with the keyboard background. On gesture navigation this
  keeps the space bar off the gesture handle. Test with both 3-button and
  gesture nav, since Android 15+ enforces edge-to-edge for apps targeting
  API 35.

### P1 — Visual refresh

- **Theme engine instead of PNG themes.** Replace the 9‑patch PNG sets with
  drawables built in code (`GradientDrawable`/`Canvas.drawRoundRect`) from a
  small set of color tokens: background, key, functional key, pressed, label,
  hint label, accent. Each existing theme then becomes a token set rather
  than ~30 PNGs per density.
- **Material You + auto dark mode.** Add a "System (dynamic)" theme that
  reads `android.R.color.system_accent1_*` / `system_neutral1_*` on API 31+,
  and switches day/night with `Configuration.uiMode`. Keep "Material black"
  for AMOLED fans.
- **Key styling options:** key corner radius, key gap, border on/off, and key
  "pop" shadow. All of these are cheap once keys are drawn in code.
- **Legibility on a 5-row layout:**
  - Separate scale sliders already exist (`pref_label_scale_v2`,
    `pref_top_row_scale`). Add a live preview next to them.
  - Draw hint labels (the small secondary symbol) in a lighter weight and
    color token rather than shrinking them. Offer a "hints only on long-press"
    mode for small phones.
  - Use the system font (or `Roboto Flex`/a monospace option) instead of
    the default `Typeface`, and respect the system font-scale setting.
- **Size by physical height, not screen percent.** Store a target row height
  in dp (about 46–56 dp per row on phones) and clamp it to a maximum share of
  the window. Add a **drag-to-resize handle** in the toolbar
  (Gboard/SwiftKey style) instead of only a percent slider in settings.
- **Better press feedback:** a short scale/fade animation on press, and key
  previews that animate out instead of disappearing on the next frame.

### P1 — Modern interaction features

Ordered roughly by value to this keyboard's audience (terminal/SSH/coding
users first):

1. **Toolbar row** above the keys, shown when there are no suggestions:
   clipboard, cursor/selection pad, undo/redo, one-handed toggle, emoji,
   settings. It collapses into the suggestion strip while typing. This also
   gives a home to features that are currently hidden behind swipe actions
   (`prefs_actions.xml`) that most users never discover.
2. **Clipboard history** with pinning, and with the content hidden when
   `ClipDescription.EXTRA_IS_SENSITIVE` is set (API 33+). This is very
   useful for SSH users pasting tokens and commands.
3. **Cursor control:** swipe on the space bar to move the cursor, swipe left
   from Backspace to delete words, and a full-screen "trackpad" mode on
   long-press of space. These complement the arrow keys.
4. **Modifier UX:** clearer one-shot vs locked state for Shift/Ctrl/Alt/Meta
   (outline = one-shot, filled = locked), and a small "Ctrl active" chip in
   the toolbar, since modifiers in a terminal are easy to leave on by mistake.
5. **One-handed, floating and split modes.** One-handed mode docks a narrower
   keyboard left or right, which matters on phones over 6.5". Floating mode
   is useful on tablets and in desktop windowing. Split mode suits tablets
   and unfolded foldables. Use Jetpack WindowManager to detect fold posture
   and window size class, and pick the 4-row or 5-row layout per size class
   rather than per portrait/landscape only.
6. **Emoji panel.** Currently there is only `popup_smileys.xml`. Use
   `androidx.emoji2` for rendering and a searchable recent/category grid.
   This is a low priority for hackers but expected by everyone else.
7. **Inline autofill suggestions** (`onCreateInlineSuggestionsRequest`,
   API 30+), so password managers show chips in the suggestion strip.
8. **Privacy:** honour `IME_FLAG_NO_PERSONALIZED_LEARNING` (incognito
   fields) and add a manual incognito toggle. Never learn from password
   fields (already partly handled at `LatinIME.java:786-840`).
9. **Voice input:** replace the legacy `voiceime` trigger with the platform
   `SpeechRecognizer` (on-device where available) or a hand-off to the
   system voice IME.
10. **Custom layouts:** allow importing/exporting user layouts (JSON) and a
    configurable extension row. Unexpected Keyboard's per-key swipe symbols
    and FlorisBoard/HeliBoard's layout files are good references.

### P1 — Haptics and sound

- Prefer `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`
  (or `KEYBOARD_PRESS`/`KEYBOARD_RELEASE`, API 27+). It respects the system
  "touch feedback" setting and uses the OEM-tuned effect.
- Keep the custom-strength slider as an option using
  `VibrationEffect.createOneShot(ms, amplitude)` when
  `Vibrator.hasAmplitudeControl()`. Use `VibratorManager` on API 31+.
- Use distinct, softer feedback for modifier and functional keys.

### P1 — Accessibility (currently missing entirely)

- Implement an `ExploreByTouchHelper` (or `AccessibilityNodeProvider`) on
  `LatinKeyboardBaseView` that exposes each key as a virtual node with a
  spoken label. "Ctrl", "Escape", "F5" and "left arrow" need explicit
  descriptions; the glyph alone isn't enough.
- Support lift-to-type when TalkBack is on.
- Add a high-contrast theme and ensure label/background contrast ≥ 4.5:1
  in every theme token set.
- Keep keys at least 7 mm or 44 dp tall even in 5-row mode. When the window
  is too small, fall back to the 4-row layout rather than shrinking below
  that.

### P2 — Settings and onboarding

- **Setup wizard** to replace the `Main` button grid: Step 1 enable the
  keyboard → Step 2 select it → Step 3 pick a layout and theme with a live
  preview → Step 4 try it in a test field. Detect each step's completion
  (via `InputMethodManager.getEnabledInputMethodList()` and
  `Settings.Secure.DEFAULT_INPUT_METHOD`) and advance automatically.
- **Settings UI:** migrate the five `PreferenceActivity` screens to one
  AndroidX `PreferenceFragmentCompat` host with a Material 3 theme and a
  pinned live keyboard preview at the top. Add search, and group settings
  as Appearance / Layout / Typing / Gestures / Feedback / Languages /
  Advanced.
- **Backup:** add settings import/export to a file, since `LatinIMEBackupAgent`
  only covers Google backup.
- **Plain-language labels:** e.g. "4-row Gingerbread layout" →
  "Compact (4 rows)", "Full 5-row layout" → "PC layout (5 rows)", and
  "Render mode" → hidden under Advanced (moot after HW acceleration).

### P2 — Typing intelligence (optional)

The 2011 dictionary and suggestion engine is the weakest part compared with
modern keyboards, but rebuilding it is a large project. Cheaper wins:
better next-word suggestions from the existing bigram dictionary, an
"undo autocorrect" chip, and turning autocorrect off by default in code and
terminal fields (`TYPE_TEXT_VARIATION_URI`, `VISIBLE_PASSWORD`, Termux and
other apps that set `TYPE_NULL`). Glide typing can wait: the open-source
options are limited, and this keyboard's audience values precise keys more.

---

## 4. Suggested roadmap

| Phase | Scope | Outcome |
|---|---|---|
| 1. Baseline | P0 list: build, targetSdk 36, PendingIntent/receiver/notification fixes, HW rendering, subtypes, in-view popups, nav-bar insets | Installable from Play again, popups and language switch fixed |
| 2. Look | Token-based theme engine, dynamic color, day/night, rounded keys, dp sizing + resize handle | Looks native on Android 12–16 |
| 3. Hacker UX | Toolbar, clipboard history, cursor pad, modifier states, haptics | Most visible quality-of-life gain for the core audience |
| 4. Reach | Accessibility, one-handed/floating/split, foldable size classes, setup wizard, new settings | Usable by more people on more devices |
| 5. Extras | Emoji panel, inline autofill, custom layout import, voice, suggestion tweaks | Feature parity with modern open-source keyboards |

Phases 1 and 2 touch the same classes (`LatinKeyboardBaseView`,
`KeyboardSwitcher`, `LatinIME`). Do them together, and consider splitting
`LatinIME.java` (3,500+ lines) into input-handling, view and settings
components as part of that work.

---

## 5. Reference keyboards

- **HeliBoard**: a maintained fork of the newer AOSP LatinIME. It shows how
  modern AOSP handles subtypes, in-view key previews, toolbar, clipboard and
  insets.
- **FlorisBoard**: a from-scratch Kotlin/Compose keyboard with a token-based
  theme engine, clipboard manager and good settings UX.
- **Unexpected Keyboard**: the closest competitor for terminal users. Keys
  carry Esc/Ctrl/Tab/arrows as corner swipe gestures, which allows a compact
  layout without losing keys.

Licenses differ (Hacker's Keyboard is Apache‑2.0; check each project's
license before copying code rather than ideas).
