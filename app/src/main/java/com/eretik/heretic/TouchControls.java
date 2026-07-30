package com.eretik.heretic;

import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.libsdl.app.SDLActivity;

/**
 * Builds the on-screen touch-control overlay on top of the SDL surface.
 *
 * Layout (landscape):
 *   top-left:   RUN (toggle shift), STR (toggle strafe-latch for the stick)
 *   top-right:  ART (enter), ] , [ , WPN (cycle 1-7), MAP (tab), ESC
 *   bottom-left:  virtual 8-way joystick (move/turn/strafe)
 *   bottom-right: FIRE (ctrl), USE (space), FLY up (pgup), FLY down (ins)
 */
public final class TouchControls {

    private TouchControls() {}

    public static void setup(SDLActivity activity) {
        View layoutView = SDLActivity.getContentView();
        if (!(layoutView instanceof RelativeLayout)) {
            return;
        }
        RelativeLayout root = (RelativeLayout) layoutView;

        JoystickView joystick = new JoystickView(activity);
        add(root, joystick, dp(activity, 180), dp(activity, 180),
                RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_LEFT,
                dp(activity, 28), 0, 0, dp(activity, 28));

        // ---- top-left column: RUN / STR latches ----
        KeyButtonView run = new KeyButtonView(activity, "RUN",
                KeyButtonView.MODE_TOGGLE, KeyEvent.KEYCODE_SHIFT_LEFT);
        add(root, run, dp(activity, 64), dp(activity, 64),
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_LEFT,
                dp(activity, 16), dp(activity, 16), 0, 0);

        KeyButtonView strafe = new KeyButtonView(activity, "STR",
                KeyButtonView.MODE_TOGGLE, -1);
        strafe.setListener((button, active) -> joystick.setStrafeMode(active));
        add(root, strafe, dp(activity, 64), dp(activity, 64),
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_LEFT,
                dp(activity, 16), dp(activity, 88), 0, 0);

        // ---- top-right rows (right to left): ESC MAP WPN / ] [ ART ----
        int top = dp(activity, 16);
        int rightMargin = dp(activity, 16);
        int gap = dp(activity, 8);
        int small = dp(activity, 60);
        int top2 = top + small + gap;

        KeyButtonView esc = new KeyButtonView(activity, "ESC",
                KeyButtonView.MODE_TAP, KeyEvent.KEYCODE_ESCAPE);
        add(root, esc, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top, rightMargin, 0);

        KeyButtonView map = new KeyButtonView(activity, "MAP",
                KeyButtonView.MODE_TAP, KeyEvent.KEYCODE_TAB);
        add(root, map, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top, rightMargin + (small + gap), 0);

        int[] weapons = {
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
                KeyEvent.KEYCODE_7
        };
        KeyButtonView wpn = new KeyButtonView(activity, "WPN", weapons);
        add(root, wpn, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top, rightMargin + 2 * (small + gap), 0);

        KeyButtonView invR = new KeyButtonView(activity, "]",
                KeyButtonView.MODE_TAP, KeyEvent.KEYCODE_RIGHT_BRACKET);
        add(root, invR, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top2, rightMargin, 0);

        KeyButtonView invL = new KeyButtonView(activity, "[",
                KeyButtonView.MODE_TAP, KeyEvent.KEYCODE_LEFT_BRACKET);
        add(root, invL, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top2, rightMargin + (small + gap), 0);

        KeyButtonView art = new KeyButtonView(activity, "ART",
                KeyButtonView.MODE_TAP, KeyEvent.KEYCODE_ENTER);
        add(root, art, small, small,
                RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, top2, rightMargin + 2 * (small + gap), 0);

        // ---- bottom-right cluster: FIRE / USE / FLY up / FLY down ----
        KeyButtonView fire = new KeyButtonView(activity, "FIRE",
                KeyButtonView.MODE_HOLD, KeyEvent.KEYCODE_CTRL_LEFT);
        add(root, fire, dp(activity, 104), dp(activity, 104),
                RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, 0, dp(activity, 28), dp(activity, 28));

        KeyButtonView use = new KeyButtonView(activity, "USE",
                KeyButtonView.MODE_HOLD, KeyEvent.KEYCODE_SPACE);
        add(root, use, dp(activity, 72), dp(activity, 72),
                RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, 0, dp(activity, 156), dp(activity, 44));

        KeyButtonView flyUp = new KeyButtonView(activity, "F+",
                KeyButtonView.MODE_HOLD, KeyEvent.KEYCODE_PAGE_UP);
        add(root, flyUp, dp(activity, 56), dp(activity, 56),
                RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, 0, dp(activity, 52), dp(activity, 156));

        KeyButtonView flyDown = new KeyButtonView(activity, "F-",
                KeyButtonView.MODE_HOLD, KeyEvent.KEYCODE_INSERT);
        add(root, flyDown, dp(activity, 56), dp(activity, 56),
                RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_RIGHT,
                0, 0, dp(activity, 52), dp(activity, 224));
    }

    private static void add(RelativeLayout root, View view, int w, int h,
                            int ruleVertical, int ruleHorizontal,
                            int left, int top, int right, int bottom) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
        lp.addRule(ruleVertical);
        lp.addRule(ruleHorizontal);
        lp.leftMargin = left;
        lp.topMargin = top;
        lp.rightMargin = right;
        lp.bottomMargin = bottom;
        root.addView(view, lp);
    }

    private static int dp(SDLActivity activity, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                activity.getResources().getDisplayMetrics());
    }
}
