package com.eretik.heretic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

/**
 * Round translucent on-screen button that emits an SDL key event.
 * Modes:
 *  - HOLD:   key down while finger is down (fire, use, fly...)
 *  - TOGGLE: each tap flips the key state (run, strafe-latch)
 *  - TAP:    short press-release pulse (weapon cycle, menu keys)
 */
public class KeyButtonView extends View {

    public static final int MODE_HOLD = 0;
    public static final int MODE_TOGGLE = 1;
    public static final int MODE_TAP = 2;

    public interface Listener {
        void onToggled(KeyButtonView button, boolean active);
    }

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();

    private final int mode;
    private int keyCode;
    private int[] keyCycle;          // for MODE_TAP with cycling (weapons)
    private int cycleIndex = 0;
    private boolean active;
    private boolean fingerDown;
    private Listener listener;

    public KeyButtonView(Context context, String label, int mode, int keyCode) {
        super(context);
        this.mode = mode;
        this.keyCode = keyCode;
        init(label);
    }

    public KeyButtonView(Context context, String label, int[] keyCycle) {
        super(context);
        this.mode = MODE_TAP;
        this.keyCycle = keyCycle;
        init(label);
    }

    private void init(String label) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x30FFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(0x60FFFFFF);
        textPaint.setColor(0xB0FFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        setTag(label);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isLatched() {
        return active;
    }

    private String label() {
        Object tag = getTag();
        return tag == null ? "" : tag.toString();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 2;

        fillPaint.setColor(fingerDown ? 0x50FFC080 : (active ? 0x5080C0FF : 0x30FFFFFF));
        canvas.drawCircle(cx, cy, r, fillPaint);
        canvas.drawCircle(cx, cy, r, borderPaint);

        String label = label();
        textPaint.setTextSize(r * (label.length() > 2 ? 0.55f : 0.8f));
        textPaint.getTextBounds(label, 0, label.length(), textBounds);
        canvas.drawText(label, cx, cy + textBounds.height() / 2f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fingerDown = true;
                onPress();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                fingerDown = false;
                onRelease();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void onPress() {
        switch (mode) {
            case MODE_HOLD:
                if (keyCode >= 0) SDLActivity.onNativeKeyDown(keyCode);
                break;
            case MODE_TOGGLE:
                active = !active;
                if (keyCode >= 0) {
                    if (active) {
                        SDLActivity.onNativeKeyDown(keyCode);
                    } else {
                        SDLActivity.onNativeKeyUp(keyCode);
                    }
                }
                if (listener != null) {
                    listener.onToggled(this, active);
                }
                break;
            case MODE_TAP:
                int key = keyCycle != null ? keyCycle[cycleIndex] : keyCode;
                SDLActivity.onNativeKeyDown(key);
                postDelayed(() -> SDLActivity.onNativeKeyUp(key), 90);
                if (keyCycle != null) {
                    cycleIndex = (cycleIndex + 1) % keyCycle.length;
                }
                break;
        }
    }

    private void onRelease() {
        if (mode == MODE_HOLD && keyCode >= 0) {
            SDLActivity.onNativeKeyUp(keyCode);
        }
    }
}
