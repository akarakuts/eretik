package com.eretik.heretic;

import android.content.Context;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import org.libsdl.app.SDLActivity;

import com.eretik.heretic.KeyButtonView.Mode;

/**
 * Builds the on-screen touch-control overlay on top of the SDL surface.
 *
 * Layout (landscape):
 *   top-left:   RUN (toggle shift), STR (toggle strafe-latch for the stick)
 *   top-right:  ESC, MAP, WPN (cycle 1-7) / ], [, ART (enter)
 *   bottom-left:  virtual 8-way joystick (move/turn/strafe)
 *   bottom-right: FIRE (ctrl), USE (space), FLY up (pgup), FLY down (ins)
 *
 * Each control is one {@code place(...)} line; adding a new button means
 * adding one line here.
 */
public final class TouchControls {

    // ---- element sizes (dp) ----
    private static final int SIZE_STICK = 180;
    private static final int SIZE_LATCH = 64;    // RUN / STR
    private static final int SIZE_SMALL = 60;    // top-right cluster
    private static final int SIZE_FIRE = 104;
    private static final int SIZE_USE = 72;
    private static final int SIZE_FLY = 56;

    // ---- margins (dp) ----
    private static final int MARGIN = 16;
    private static final int MARGIN_EDGE = 28;
    private static final int GAP = 8;

    private static final int[] WEAPON_KEYS = {
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7
    };

    /** Screen corner a control is anchored to; margins grow inward from it. */
    private enum Anchor {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT}

    private TouchControls() {}

    public static void setup(SDLActivity activity) {
        View contentView = SDLActivity.getContentView();
        if (!(contentView instanceof RelativeLayout)) {
            return;
        }
        RelativeLayout root = (RelativeLayout) contentView;

        JoystickView joystick = new JoystickView(activity);
        place(root, joystick, activity, SIZE_STICK, SIZE_STICK,
                Anchor.BOTTOM_LEFT, MARGIN_EDGE, MARGIN_EDGE);

        // ---- top-left column: RUN / STR latches ----
        place(root, button(activity, "RUN", Mode.TOGGLE, KeyEvent.KEYCODE_SHIFT_LEFT),
                activity, SIZE_LATCH, SIZE_LATCH, Anchor.TOP_LEFT, MARGIN, MARGIN);

        KeyButtonView strafe = button(activity, "STR", Mode.TOGGLE, -1);
        strafe.setListener((button, active) -> joystick.setStrafeMode(active));
        place(root, strafe, activity, SIZE_LATCH, SIZE_LATCH,
                Anchor.TOP_LEFT, MARGIN, MARGIN + SIZE_LATCH + GAP);

        // ---- top-right rows (right to left): ESC MAP WPN / ] [ ART ----
        int row2 = MARGIN + SIZE_SMALL + GAP;
        int col2 = MARGIN + SIZE_SMALL + GAP;
        int col3 = MARGIN + 2 * (SIZE_SMALL + GAP);

        place(root, button(activity, "ESC", Mode.TAP, KeyEvent.KEYCODE_ESCAPE),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, MARGIN, MARGIN);
        place(root, button(activity, "MAP", Mode.TAP, KeyEvent.KEYCODE_TAB),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, col2, MARGIN);
        place(root, new KeyButtonView(activity, "WPN", WEAPON_KEYS),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, col3, MARGIN);

        place(root, button(activity, "]", Mode.TAP, KeyEvent.KEYCODE_RIGHT_BRACKET),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, MARGIN, row2);
        place(root, button(activity, "[", Mode.TAP, KeyEvent.KEYCODE_LEFT_BRACKET),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, col2, row2);
        place(root, button(activity, "ART", Mode.TAP, KeyEvent.KEYCODE_ENTER),
                activity, SIZE_SMALL, SIZE_SMALL, Anchor.TOP_RIGHT, col3, row2);

        // ---- bottom-right cluster: FIRE / USE / FLY up / FLY down ----
        place(root, button(activity, "FIRE", Mode.HOLD, KeyEvent.KEYCODE_CTRL_LEFT),
                activity, SIZE_FIRE, SIZE_FIRE, Anchor.BOTTOM_RIGHT, MARGIN_EDGE, MARGIN_EDGE);
        place(root, button(activity, "USE", Mode.HOLD, KeyEvent.KEYCODE_SPACE),
                activity, SIZE_USE, SIZE_USE, Anchor.BOTTOM_RIGHT,
                MARGIN_EDGE + SIZE_FIRE + 24, MARGIN_EDGE + 16);
        place(root, button(activity, "F+", Mode.HOLD, KeyEvent.KEYCODE_PAGE_UP),
                activity, SIZE_FLY, SIZE_FLY, Anchor.BOTTOM_RIGHT,
                MARGIN_EDGE + 24, MARGIN_EDGE + SIZE_FIRE + 24);
        place(root, button(activity, "F-", Mode.HOLD, KeyEvent.KEYCODE_INSERT),
                activity, SIZE_FLY, SIZE_FLY, Anchor.BOTTOM_RIGHT,
                MARGIN_EDGE + 24, MARGIN_EDGE + SIZE_FIRE + 92);
    }

    private static KeyButtonView button(Context context, String label, Mode mode, int keyCode) {
        return new KeyButtonView(context, label, mode, keyCode);
    }

    /**
     * Adds a view anchored to a screen corner.
     *
     * @param horizMarginDp distance from the left/right screen edge (dp)
     * @param vertMarginDp  distance from the top/bottom screen edge (dp)
     */
    private static void place(RelativeLayout root, View view, Context context,
                              int widthDp, int heightDp, Anchor anchor,
                              int horizMarginDp, int vertMarginDp) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                dp(context, widthDp), dp(context, heightDp));
        lp.addRule(anchor == Anchor.TOP_LEFT || anchor == Anchor.TOP_RIGHT
                ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(anchor == Anchor.TOP_LEFT || anchor == Anchor.BOTTOM_LEFT
                ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT);
        if (anchor == Anchor.TOP_LEFT || anchor == Anchor.BOTTOM_LEFT) {
            lp.leftMargin = dp(context, horizMarginDp);
        } else {
            lp.rightMargin = dp(context, horizMarginDp);
        }
        if (anchor == Anchor.TOP_LEFT || anchor == Anchor.TOP_RIGHT) {
            lp.topMargin = dp(context, vertMarginDp);
        } else {
            lp.bottomMargin = dp(context, vertMarginDp);
        }
        root.addView(view, lp);
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}
