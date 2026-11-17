package com.zaoba.alarm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** 主题预览小样：用该主题自己的配色 / 圆角 / 字体 / 网格画出迷你界面 */
public class ThemePreview extends View {

    private final Theme t;
    private final float d;

    public ThemePreview(Context c, Theme th) {
        super(c);
        t = th;
        d = c.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth();
        float h = getHeight();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 底
        p.setStyle(Paint.Style.FILL);
        p.setColor(t.bg);
        cv.drawRect(0, 0, w, h, p);

        // 网格
        if (t.gridBg) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(t.gridColor);
            float st = 16 * d;
            for (float x = 0; x < w; x += st) {
                cv.drawLine(x, 0, x, h, p);
            }
            for (float y = 0; y < h; y += st) {
                cv.drawLine(0, y, w, y, p);
            }
        }

        float m = 9 * d;
        float r = t.cardRadius * d;
        RectF rect = new RectF(m, m, w - m * 3, h - m);

        // 面板
        p.setStyle(Paint.Style.FILL);
        p.setColor(Ui.alpha(t.bg, 0xF2));
        cv.drawRoundRect(rect, r, r, p);

        if (t.cardStyle == Theme.STYLE_SOFT && t.frameStyle == 0) {
            p.setColor(Ui.alpha(t.card2, 0xFF));
            cv.drawRoundRect(rect, r, r, p);
        }

        // 边框
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(t.line);
        cv.drawRoundRect(rect, r, r, p);

        // 四角标
        if (t.frameStyle == 1) {
            p.setStrokeWidth(2.2f);
            p.setColor(t.frameColor != 0 ? t.frameColor : t.accent);
            float len = 7 * d;
            cv.drawLine(rect.left, rect.top, rect.left + len, rect.top, p);
            cv.drawLine(rect.left, rect.top, rect.left, rect.top + len, p);
            cv.drawLine(rect.right - len, rect.top, rect.right, rect.top, p);
            cv.drawLine(rect.right, rect.top, rect.right, rect.top + len, p);
            cv.drawLine(rect.left, rect.bottom - len, rect.left, rect.bottom, p);
            cv.drawLine(rect.left, rect.bottom, rect.left + len, rect.bottom, p);
            cv.drawLine(rect.right - len, rect.bottom, rect.right, rect.bottom, p);
            cv.drawLine(rect.right, rect.bottom - len, rect.right, rect.bottom, p);
        }
        if (t.cardStyle == Theme.STYLE_STRIPE) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Ui.alpha(t.accent, 0x55));
            cv.drawRect(rect.left, rect.top, rect.left + 3 * d, rect.bottom, p);
        }

        // 小标题
        p.setStyle(Paint.Style.FILL);
        p.setColor(t.sub);
        p.setTextSize(8.5f * d);
        p.setTypeface(Typeface.create(t.capsLabels ? "monospace" : "sans-serif", Typeface.NORMAL));
        cv.drawText(t.capsLabels ? "AM · WAKE" : "上午 · 起床闹钟", rect.left + 12 * d, rect.top + 15 * d, p);

        // 时间
        p.setColor(t.accent);
        p.setTextSize(26 * d);
        p.setTypeface(Typeface.create(t.timeFace, Typeface.NORMAL));
        cv.drawText("11:45", rect.left + 11 * d, rect.top + 44 * d, p);

        // 课程条
        p.setColor(t.text);
        p.setTextSize(10 * d);
        p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        cv.drawText("高等数学 · 第 1-2 节", rect.left + 12 * d, rect.bottom - 18 * d, p);

        // 胶囊
        p.setColor(Ui.alpha(t.accent2, 0x33));
        float cw = 34 * d, ch = 11 * d;
        RectF chip = new RectF(rect.right - cw - 10 * d, rect.bottom - 26 * d, rect.right - 10 * d, rect.bottom - 26 * d + ch);
        cv.drawRoundRect(chip, ch / 2, ch / 2, p);
        p.setColor(t.accent2);
        p.setTextSize(7 * d);
        cv.drawText("PM", chip.left + 10 * d, chip.centerY() + 2.5f * d, p);

        // 三色块
        float bx = w - 3 * (11 * d) - 20 * d;
        int[] cs = {t.bg, t.card, t.accent};
        for (int i = 0; i < 3; i++) {
            p.setColor(cs[i]);
            cv.drawRoundRect(new RectF(bx + i * 12 * d, m * 1.2f, bx + i * 12 * d + 9 * d, m * 1.2f + 9 * d), 2 * d, 2 * d, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1);
            p.setColor(Ui.alpha(t.line, 0xFF));
            cv.drawRoundRect(new RectF(bx + i * 12 * d, m * 1.2f, bx + i * 12 * d + 9 * d, m * 1.2f + 9 * d), 2 * d, 2 * d, p);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
