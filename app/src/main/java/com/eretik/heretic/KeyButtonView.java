package com.eretik.heretic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

/**
 * Round translucent on-screen button that emits SDL key events.
 * Behavior depends on {@link Mode}: held key, latched key or a short pulse.
 */
public class KeyButtonView extends View {

    public enum Mode {
        /** Key down while the finger is down (fire, use, fly...). */
        HOLD,
        /** Each tap flips the key state (run, strafe-latch). */
        TOGGLE,
        /** Short press-release pulse (weapon cycle, menu keys). */
        TAP
    }

    public interface Listener {
        void onToggled(KeyButtonView button, boolean active);
    }

    /** Duration of the key press emitted in TAP mode. */
    private static final long TAP_PULSE_MS = 90;
    /** Labels longer than this are drawn with the smaller font scale. */
    private static final int SHORT_LABEL_MAX_LENGTH = 2;
    private static final float LONG_LABEL_SCALE = 0.55f;
    private static final float SHORT_LABEL_SCALE = 0.8f;

    private final Paint fillPaint = OverlayStyle.fillPaint();
    private final Paint borderPaint = OverlayStyle.strokePaint();
    private final Paint textPaint = OverlayStyle.textPaint();

    private final Mode mode;
    private final String label;
    private final int keyCode;
    private final int[] keyCycle;   // TAP with cycling (weapons), null otherwise
    private int cycleIndex;
    private boolean active;
    private boolean fingerDown;
    private Listener listener;

    /** Button emitting a single key code (a negative keyCode means a pure latch). */
    public KeyButtonView(Context context, String label, Mode mode, int keyCode) {
        this(context, label, mode, keyCode, null);
    }

    /** TAP-mode button cycling through the given key codes (weapon selection). */
    public KeyButtonView(Context context, String label, int[] keyCycle) {
        this(context, label, Mode.TAP, -1, keyCycle.clone());
    }

    private KeyButtonView(Context context, String label, Mode mode, int keyCode, int[] keyCycle) {
        super(context);
        this.label = label;
        this.mode = mode;
        this.keyCode = keyCode;
        this.keyCycle = keyCycle;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 2;

        fillPaint.setColor(fingerDown ? OverlayStyle.COLOR_FILL_PRESSED
                : active ? OverlayStyle.COLOR_FILL_LATCHED
                : OverlayStyle.COLOR_FILL_IDLE);
        borderPaint.setStrokeWidth(fingerDown ? OverlayStyle.BORDER_WIDTH_PRESSED
                : OverlayStyle.BORDER_WIDTH_IDLE);
        canvas.drawCircle(cx, cy, r, fillPaint);
        canvas.drawCircle(cx, cy, r, borderPaint);

        float scale = label.length() > SHORT_LABEL_MAX_LENGTH
                ? LONG_LABEL_SCALE : SHORT_LABEL_SCALE;
        textPaint.setTextSize(r * scale);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        canvas.drawText(label, cx, cy - (metrics.ascent + metrics.descent) / 2f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                fingerDown = true;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                onPress();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                fingerDown = false;
                onRelease();
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void onPress() {
        switch (mode) {
            case HOLD:
                if (keyCode >= 0) {
                    SDLActivity.onNativeKeyDown(keyCode);
                }
                break;
            case TOGGLE:
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
            case TAP:
                int key = keyCycle != null ? keyCycle[cycleIndex] : keyCode;
                SDLActivity.onNativeKeyDown(key);
                postDelayed(() -> SDLActivity.onNativeKeyUp(key), TAP_PULSE_MS);
                if (keyCycle != null) {
                    cycleIndex = (cycleIndex + 1) % keyCycle.length;
                }
                break;
        }
    }

    private void onRelease() {
        if (mode == Mode.HOLD && keyCode >= 0) {
            SDLActivity.onNativeKeyUp(keyCode);
        }
    }
}
