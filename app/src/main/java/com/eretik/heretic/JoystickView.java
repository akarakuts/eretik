package com.eretik.heretic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * Virtual 8-way digital joystick (bottom-left).
 * Vertical axis: forward/back (DPAD_UP/DOWN).
 * Horizontal axis: turn (DPAD_LEFT/RIGHT) or strafe (,/.) when strafe mode latched.
 * Diagonals press two keys at once, like a keyboard.
 */
public class JoystickView extends View {

    /** Fraction of the radius the stick must travel before a direction engages. */
    private static final float DEADZONE = 0.28f;
    /** Knob radius relative to the base radius. */
    private static final float KNOB_RATIO = 0.38f;
    /** Direction guide ticks around the base (one per emulated direction). */
    private static final int TICK_COUNT = 8;
    /** Tick endpoints relative to the base radius. */
    private static final float TICK_INNER_RATIO = 0.86f;
    private static final float TICK_OUTER_RATIO = 0.96f;

    private final Paint basePaint = OverlayStyle.fillPaint();
    private final Paint borderPaint = OverlayStyle.strokePaint();
    private final Paint knobPaint = OverlayStyle.fillPaint();

    private int trackedPointer = -1;
    private float touchX, touchY;      // relative to center, clamped to radius
    private boolean active;
    private boolean strafeMode;

    private final Set<Integer> pressedKeys = new HashSet<>();

    public JoystickView(Context context) {
        super(context);
    }

    public void setStrafeMode(boolean strafeMode) {
        if (this.strafeMode != strafeMode) {
            this.strafeMode = strafeMode;
            // Re-evaluate the currently held direction with the new mapping.
            updateKeys(normalizedX(), normalizedY());
        }
    }

    private float normalizedX() { return touchX / radius(); }
    private float normalizedY() { return touchY / radius(); }

    private float radius() { return getWidth() / 2f; }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = radius();
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        canvas.drawCircle(cx, cy, r - 2, basePaint);
        canvas.drawCircle(cx, cy, r - 2, borderPaint);
        drawDirectionTicks(canvas, cx, cy, r);

        float knobR = r * KNOB_RATIO;
        canvas.drawCircle(cx + touchX, cy + touchY, knobR, knobPaint);
        canvas.drawCircle(cx + touchX, cy + touchY, knobR, borderPaint);
    }

    private void drawDirectionTicks(Canvas canvas, float cx, float cy, float r) {
        for (int i = 0; i < TICK_COUNT; i++) {
            double angle = i * (2 * Math.PI / TICK_COUNT);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            canvas.drawLine(cx + cos * r * TICK_INNER_RATIO, cy + sin * r * TICK_INNER_RATIO,
                    cx + cos * r * TICK_OUTER_RATIO, cy + sin * r * TICK_OUTER_RATIO,
                    borderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                trackedPointer = event.getPointerId(0);
                active = true;
                handleMove(event.getX(0), event.getY(0));
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (!active) {
                    trackedPointer = event.getPointerId(index);
                    active = true;
                    handleMove(event.getX(index), event.getY(index));
                }
                return true;

            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (event.getPointerId(i) == trackedPointer) {
                        handleMove(event.getX(i), event.getY(i));
                        break;
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(index) == trackedPointer) {
                    releaseStick();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (trackedPointer == -1 || event.getPointerId(index) == trackedPointer) {
                    releaseStick();
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void handleMove(float x, float y) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = x - cx;
        float dy = y - cy;
        float r = radius();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > r) {
            dx = dx / len * r;
            dy = dy / len * r;
        }
        touchX = dx;
        touchY = dy;
        invalidate();
        updateKeys(dx / r, dy / r);
    }

    private void releaseStick() {
        releaseAllKeys();
        active = false;
        trackedPointer = -1;
        touchX = touchY = 0;
        invalidate();
    }

    private void updateKeys(float nx, float ny) {
        Set<Integer> wanted = new HashSet<>(4);
        if (ny < -DEADZONE) wanted.add(KeyEvent.KEYCODE_DPAD_UP);
        if (ny > DEADZONE)  wanted.add(KeyEvent.KEYCODE_DPAD_DOWN);
        if (nx < -DEADZONE) wanted.add(horizontalKey(true));
        if (nx > DEADZONE)  wanted.add(horizontalKey(false));
        syncKeys(wanted);
    }

    /** Key emitted for horizontal deflection: strafe keys when latched, turn keys otherwise. */
    private int horizontalKey(boolean left) {
        if (strafeMode) {
            return left ? KeyEvent.KEYCODE_COMMA : KeyEvent.KEYCODE_PERIOD;
        }
        return left ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    /** Diffs the wanted key set against the pressed one and emits SDL events. */
    private void syncKeys(Set<Integer> wanted) {
        for (Integer key : pressedKeys) {
            if (!wanted.contains(key)) {
                SDLActivity.onNativeKeyUp(key);
            }
        }
        for (Integer key : wanted) {
            if (!pressedKeys.contains(key)) {
                SDLActivity.onNativeKeyDown(key);
            }
        }
        pressedKeys.clear();
        pressedKeys.addAll(wanted);
    }

    private void releaseAllKeys() {
        for (Integer key : pressedKeys) {
            SDLActivity.onNativeKeyUp(key);
        }
        pressedKeys.clear();
    }
}
