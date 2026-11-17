package com.zaoba.alarm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

/** 终末地母题层：取景角 / 楔形页签 / 镂空巨字 / 刻度圆环 */
public class Motif {

    private static Paint stroke(int color, float w) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(w);
        return p;
    }

    /** 取景角：四角细括号，扣在边缘内侧，和 HUD 那种贴边角标区分开 */
    public static class Viewfinder extends Drawable {
        private final Paint p;
        private final float len;

        public Viewfinder(int color, float lenPx) {
            p = stroke(color, 1.2f);
            len = Math.max(8f, lenPx);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float l = b.left + 2, t = b.top + 2, r = b.right - 2, bo = b.bottom - 2;
            c.drawLine(l, t, l + len, t, p);
            c.drawLine(l, t, l, t + len, p);
            c.drawLine(r - len, t, r, t, p);
            c.drawLine(r, t, r, t + len, p);
            c.drawLine(l, bo - len, l, bo, p);
            c.drawLine(l, bo, l + len, bo, p);
            c.drawLine(r - len, bo, r, bo, p);
            c.drawLine(r, bo - len, r, bo, p);
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }

    /** 楔形页签：右端斜切 */
    public static class Wedge extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float cut;

        public Wedge(int color, float cutPx) {
            fill.setColor(color);
            fill.setStyle(Paint.Style.FILL);
            cut = Math.max(4f, cutPx);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            Path path = new Path();
            path.moveTo(b.left, b.top);
            path.lineTo(b.right - cut, b.top);
            path.lineTo(b.right, b.bottom);
            path.lineTo(b.left, b.bottom);
            path.close();
            c.drawPath(path, fill);
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }

    /** 镂空巨字：描边不填充的数字水印 */
    public static class Watermark extends View {
        private final String text;
        private final int color;
        private final float sizePx;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public Watermark(Context c, String t, int col, float sizeDp) {
            super(c);
            text = t;
            color = col;
            sizePx = sizeDp * c.getResources().getDisplayMetrics().density;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(c.getResources().getDisplayMetrics().density * 1.2f);
            p.setColor(color);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextSize(sizePx);
            p.setLetterSpacing(-0.04f);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = p.measureText(text);
            c.drawText(text, 10, getHeight() - 6, p);
        }
    }

    /** 刻度圆环：60 格，每 5 格加长 */
    public static class TickRing extends View {
        private final Paint p;
        private final int color;
        private final float d;

        public TickRing(Context c, int col) {
            super(c);
            color = col;
            d = c.getResources().getDisplayMetrics().density;
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(col);
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 3 * d;
            if (r <= 0) {
                return;
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(Ui.alpha(color, 0x55));
            c.drawCircle(cx, cy, r, p);
            p.setStrokeWidth(1.4f);
            for (int i = 0; i < 60; i++) {
                double a = Math.toRadians(i * 6 - 90);
                boolean major = (i % 5 == 0);
                float len = (major ? 6f : 3f) * d;
                p.setColor(major ? Ui.alpha(color, 0xCC) : Ui.alpha(color, 0x55));
                float x1 = (float) (cx + Math.cos(a) * r);
                float y1 = (float) (cy + Math.sin(a) * r);
                float x2 = (float) (cx + Math.cos(a) * (r - len));
                float y2 = (float) (cy + Math.sin(a) * (r - len));
                c.drawLine(x1, y1, x2, y2, p);
            }
        }
    }
}
