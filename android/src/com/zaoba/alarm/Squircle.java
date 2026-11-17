package com.zaoba.alarm;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * 连续曲率圆角（superellipse / squircle）。
 * 普通圆角在「直边 → 圆弧」的接缝处曲率从 0 跳到 1/r；超椭圆 |x|ⁿ+|y|ⁿ=1（n≈4）
 * 把这段过渡掉，角看起来更饱满，像被手磨圆的料而不是切出来的。
 */
public class Squircle extends Drawable {

    private static final double EXP = 4.0;   // 4 ≈ iOS 图标那种饱满度；2 就是普通椭圆
    private static final int SEG = 9;        // 每个角采样段数

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke;
    private final float radiusPx;
    private Path path;
    private Rect lastBounds;

    public Squircle(int fillColor, float radiusDp, float density) {
        this(fillColor, 0, radiusDp, density);
    }

    public Squircle(int fillColor, int strokeColor, float radiusDp, float density) {
        fill.setColor(fillColor);
        fill.setStyle(Paint.Style.FILL);
        if (strokeColor != 0) {
            stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setColor(strokeColor);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(density);
        } else {
            stroke = null;
        }
        radiusPx = radiusDp * density;
    }

    private static float p(double u) {
        return (float) Math.pow(Math.sin(u), 2.0 / EXP);
    }

    private static float q(double u) {
        return (float) Math.pow(Math.cos(u), 2.0 / EXP);
    }

    private Path build(Rect b) {
        float l = b.left, t = b.top, r = b.right, bo = b.bottom;
        float rr = Math.min(radiusPx, Math.min(b.width(), b.height()) / 2f);
        Path p = new Path();
        if (rr <= 0.5f) {
            p.addRect(l, t, r, bo, Path.Direction.CW);
            return p;
        }
        p.moveTo(l + rr, t);
        p.lineTo(r - rr, t);
        // 右上：从 (r-rr, t) 到 (r, t+rr)
        for (int i = 1; i <= SEG; i++) {
            double u = (Math.PI / 2) * i / SEG;
            p.lineTo(r - rr + rr * p(u), t + rr - rr * q(u));
        }
        p.lineTo(r, bo - rr);
        // 右下：从 (r, bo-rr) 到 (r-rr, bo)
        for (int i = 1; i <= SEG; i++) {
            double u = (Math.PI / 2) * i / SEG;
            p.lineTo(r - rr + rr * q(u), bo - rr + rr * p(u));
        }
        p.lineTo(l + rr, bo);
        // 左下：从 (l+rr, bo) 到 (l, bo-rr)
        for (int i = 1; i <= SEG; i++) {
            double u = (Math.PI / 2) * i / SEG;
            p.lineTo(l + rr - rr * p(u), bo - rr + rr * q(u));
        }
        p.lineTo(l, t + rr);
        // 左上：从 (l, t+rr) 到 (l+rr, t)
        for (int i = 1; i <= SEG; i++) {
            double u = (Math.PI / 2) * i / SEG;
            p.lineTo(l + rr - rr * q(u), t + rr - rr * p(u));
        }
        p.close();
        return p;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (path == null || lastBounds == null || !lastBounds.equals(b)) {
            path = build(b);
            lastBounds = new Rect(b);
        }
        c.drawPath(path, fill);
        if (stroke != null) {
            c.drawPath(path, stroke);
        }
    }

    @Override
    public void setAlpha(int a) {
        fill.setAlpha(a);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
