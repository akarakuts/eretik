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

    private static final float DEADZONE = 0.28f;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int trackedPointer = -1;
    private float touchX, touchY;      // relative to center, clamped to radius
    private boolean active;
    private boolean strafeMode;

    private final Set<Integer> pressedKeys = new HashSet<>();

    public JoystickView(Context context) {
        super(context);
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(0x30FFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(0x60FFFFFF);
        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(0x50FFFFFF);
    }

    public void setStrafeMode(boolean strafeMode) {
        if (this.strafeMode != strafeMode) {
            this.strafeMode = strafeMode;
            // Re-evaluate currently pressed direction with the new mapping.
            updateKeys(normalizedX(), normalizedY());
        }
    }

    private float normalizedX() { return touchX / radius(); }
    private float normalizedY() { return touchY / radius(); }

    private float radius() { return getWidth() / 2f; }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = radius();
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        canvas.drawCircle(cx, cy, r - 2, basePaint);
        canvas.drawCircle(cx, cy, r - 2, borderPaint);
        float knobR = r * 0.38f;
        canvas.drawCircle(cx + touchX, cy + touchY, knobR, knobPaint);
        canvas.drawCircle(cx + touchX, cy + touchY, knobR, borderPaint);
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
                    releaseAll();
                    active = false;
                    trackedPointer = -1;
                    touchX = touchY = 0;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (trackedPointer == -1 || event.getPointerId(index) == trackedPointer) {
                    releaseAll();
                    active = false;
                    trackedPointer = -1;
                    touchX = touchY = 0;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleMove(float x, float y) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float dx = x - cx, dy = y - cy;
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

    private void updateKeys(float nx, float ny) {
        Set<Integer> wanted = new HashSet<>();
        if (ny < -DEADZONE) wanted.add(KeyEvent.KEYCODE_DPAD_UP);
        if (ny > DEADZONE)  wanted.add(KeyEvent.KEYCODE_DPAD_DOWN);
        if (nx < -DEADZONE) wanted.add(strafeMode ? KeyEvent.KEYCODE_COMMA  : KeyEvent.KEYCODE_DPAD_LEFT);
        if (nx > DEADZONE)  wanted.add(strafeMode ? KeyEvent.KEYCODE_PERIOD : KeyEvent.KEYCODE_DPAD_RIGHT);

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

    private void releaseAll() {
        for (Integer key : pressedKeys) {
            SDLActivity.onNativeKeyUp(key);
        }
        pressedKeys.clear();
    }
}
