package com.zaoba.alarm;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** HUD 控制台画法：网格底、四角标框、切角面板 */
public class Hud {

    private static Paint stroke(int color, float w) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(w);
        return p;
    }

    /** 网点纸底（漫画网点 / halftone）：用 BitmapShader 平铺，滚动不卡 */
    public static class Grain extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int bg;

        public Grain(int bgColor, int dotColor, float stepPx, float radiusPx) {
            bg = bgColor;
            int s = Math.max(6, (int) stepPx);
            android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(s, s,
                    android.graphics.Bitmap.Config.ARGB_8888);
            Canvas c2 = new Canvas(bm);
            c2.drawColor(bgColor);
            Paint dp = new Paint(Paint.ANTI_ALIAS_FLAG);
            dp.setColor(dotColor);
            c2.drawCircle(s * 0.5f, s * 0.5f, Math.max(0.8f, radiusPx), dp);
            fill.setShader(new android.graphics.BitmapShader(bm,
                    android.graphics.Shader.TileMode.REPEAT,
                    android.graphics.Shader.TileMode.REPEAT));
        }

        @Override
        public void draw(Canvas c) {
            Paint b = new Paint();
            b.setColor(bg);
            c.drawRect(getBounds(), b);
            c.drawRect(getBounds(), fill);
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    /** 暗角：四周压暗，把视线赶回画面中间 */
    public static class Vignette extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) {
                return;
            }
            android.graphics.RadialGradient g = new android.graphics.RadialGradient(
                    b.width() / 2f, b.height() * 0.46f,
                    Math.max(b.width(), b.height()) * 0.78f,
                    new int[]{0x00000000, 0x00000000, 0x96000000},
                    new float[]{0f, 0.52f, 1f},
                    android.graphics.Shader.TileMode.CLAMP);
            p.setShader(g);
            c.drawRect(b, p);
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

    /** 整页网格底 */
    public static class Grid extends Drawable {
        private final int bg;
        private final Paint line;
        private final float step;
        private final Paint bgPaint = new Paint();

        public Grid(int bgColor, int lineColor, float stepPx) {
            bg = bgColor;
            line = stroke(lineColor, 1f);
            step = Math.max(8f, stepPx);
            bgPaint.setColor(bgColor);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            c.drawRect(b, bgPaint);
            for (float x = b.left; x <= b.right; x += step) {
                c.drawLine(x, b.top, x, b.bottom, line);
            }
            for (float y = b.top; y <= b.bottom; y += step) {
                c.drawLine(b.left, y, b.right, y, line);
            }
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    /** 细线框 + 四角加粗角标 */
    public static class Frame extends Drawable {
        private final Paint box;
        private final Paint corner;
        private final float len;
        private final float radius;

        public Frame(int lineColor, int cornerColor, float lenPx, float radiusPx) {
            box = stroke(lineColor, 1f);
            corner = stroke(cornerColor, 2.2f);
            len = Math.max(6f, lenPx);
            radius = radiusPx;
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float l = b.left + 1, t = b.top + 1, r = b.right - 1, bo = b.bottom - 1;
            if (radius > 0) {
                android.graphics.RectF rf = new android.graphics.RectF(l, t, r, bo);
                c.drawRoundRect(rf, radius, radius, box);
            } else {
                c.drawRect(l, t, r, bo, box);
            }
            // 四角 L 形
            c.drawLine(l, t, l + len, t, corner);
            c.drawLine(l, t, l, t + len, corner);
            c.drawLine(r - len, t, r, t, corner);
            c.drawLine(r, t, r, t + len, corner);
            c.drawLine(l, bo - len, l, bo, corner);
            c.drawLine(l, bo, l + len, bo, corner);
            c.drawLine(r - len, bo, r, bo, corner);
            c.drawLine(r, bo - len, r, bo, corner);
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

    /** 切角面板：左上 / 右下切掉一角 */
    public static class Cut extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge;
        private final float cut;

        public Cut(int fillColor, int edgeColor, float cutPx) {
            fill.setColor(fillColor);
            fill.setStyle(Paint.Style.FILL);
            edge = stroke(edgeColor, 1f);
            cut = Math.max(4f, cutPx);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float l = b.left + 1, t = b.top + 1, r = b.right - 1, bo = b.bottom - 1;
            Path p = new Path();
            p.moveTo(l + cut, t);
            p.lineTo(r, t);
            p.lineTo(r, bo - cut);
            p.lineTo(r - cut, bo);
            p.lineTo(l, bo);
            p.lineTo(l, t + cut);
            p.close();
            c.drawPath(p, fill);
            c.drawPath(p, edge);
            c.drawLine(l + cut, t, r, t, stroke(edge.getColor(), 2f));
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
}
