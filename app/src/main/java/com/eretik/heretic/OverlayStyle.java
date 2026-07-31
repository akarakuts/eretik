package com.eretik.heretic;

import android.graphics.Paint;

/**
 * Shared visual style of the touch-control overlay.
 * Single place for all colors and stroke widths used by the overlay views.
 */
final class OverlayStyle {

    static final int COLOR_FILL_IDLE = 0x30FFFFFF;
    static final int COLOR_FILL_PRESSED = 0x50FFC080;
    static final int COLOR_FILL_LATCHED = 0x5080C0FF;
    static final int COLOR_BORDER = 0x60FFFFFF;
    static final int COLOR_TEXT = 0xB0FFFFFF;

    static final float BORDER_WIDTH_IDLE = 3f;
    static final float BORDER_WIDTH_PRESSED = 5f;

    private OverlayStyle() {}

    static Paint fillPaint() {
        Paint paint = basePaint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_FILL_IDLE);
        return paint;
    }

    static Paint strokePaint() {
        Paint paint = basePaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(BORDER_WIDTH_IDLE);
        paint.setColor(COLOR_BORDER);
        return paint;
    }

    static Paint textPaint() {
        Paint paint = basePaint();
        paint.setColor(COLOR_TEXT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        return paint;
    }

    private static Paint basePaint() {
        return new Paint(Paint.ANTI_ALIAS_FLAG);
    }
}
