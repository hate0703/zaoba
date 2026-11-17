package com.zaoba.alarm;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Ui {

    // 这些不是 final：切主题时整体换掉，所有引用自动跟着变
    public static int INK   = 0xFF0A0D12;
    public static int CARD  = 0xFF11161E;
    public static int CARD2 = 0xFF191F28;
    public static int LINE  = 0xFF232C38;
    public static int TX    = 0xFFECEAE3;
    public static int MU    = 0xFF8792A4;
    public static int DIM   = 0xFF5D6879;
    public static int AM    = 0xFFF2A65A;
    public static int PM    = 0xFF6FA8C9;
    public static int OK    = 0xFF6FBF9B;

    public static void use(Theme t) {
        INK = t.bg;
        CARD = t.card;
        CARD2 = t.card2;
        LINE = t.line;
        TX = t.text;
        MU = t.sub;
        DIM = t.dim;
        AM = t.accent;
        PM = t.accent2;
        OK = t.ok;
    }

    /** 当前主题下「一张卡」长什么样：柔光 / 技术面板 / 实块 / 左缘发光条 / 角标框 */
    public static android.graphics.drawable.Drawable cardDrawable(Context c) {
        Theme t = Theme.current(c);
        float r = t.cardRadius;
        if (t.motifs) {
            android.graphics.drawable.Drawable base = bg(c, alpha(t.bg, 0xF2), r);
            android.graphics.drawable.Drawable vf = new Motif.Viewfinder(t.line, dp(c, 18));
            return new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{base, vf});
        }
        if (t.frameStyle == 1) {
            android.graphics.drawable.Drawable base = bg(c, alpha(t.bg, 0xF2), r);
            android.graphics.drawable.Drawable frame = new Hud.Frame(t.line, t.frameColor != 0 ? t.frameColor : t.accent, dp(c, 10), r);
            return new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{base, frame});
        }
        if (t.frameStyle == 2) {
            return new Hud.Cut(alpha(t.card, 0xFF), alpha(t.accent, 0x77), dp(c, 12));
        }
        switch (t.cardStyle) {
            case Theme.STYLE_PANEL: {
                GradientDrawable g = bg(c, t.card, r);
                g.setStroke(dp(c, 1), t.line);
                return g;
            }
            case Theme.STYLE_BLOCK: {
                GradientDrawable g = grad(c, t.card2, t.card, r);
                g.setStroke(dp(c, 1), alpha(t.accent, 0x30));
                return g;
            }
            case Theme.STYLE_STRIPE: {
                GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{alpha(t.accent, 0x38), t.card, t.card});
                g.setCornerRadius(dp(c, r));
                g.setStroke(dp(c, 1), t.line);
                return g;
            }
            case Theme.STYLE_PAPER: {
                return new Squircle(t.card, r, c.getResources().getDisplayMetrics().density);
            }
            default: {
                return grad(c, t.card2, t.card, r);
            }
        }
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static TextView tv(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView bold(Context c, String s, float sp, int color) {
        TextView t = tv(c, s, sp, color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static GradientDrawable bg(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** 双色渐变，横竖都柔和 */
    public static GradientDrawable grad(Context c, int top, int bottom, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{top, bottom});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    /** 给颜色加透明度：a = 0..255 */
    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static GradientDrawable bg(Context c, int fill, float radiusDp, int stroke) {
        GradientDrawable g = bg(c, fill, radiusDp);
        g.setStroke(dp(c, 1), stroke);
        return g;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(cardDrawable(c));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static View gap(Context c, int dpValue) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpValue)));
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }
}
