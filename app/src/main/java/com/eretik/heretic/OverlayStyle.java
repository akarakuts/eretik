// Eretik — Heretic port for Android
// Copyright (C) 2026 Aleksey Karakuts
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see <https://www.gnu.org/licenses/>.

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
