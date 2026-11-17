package com.zaoba.alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {

    private static final int REQ_ICS = 7001;
    private static final int REQ_TEXT = 7002;
    private static final int REQ_IMG = 7003;

    private static final String[] TIPS = {
            "起床先喝一杯温水，胃会舒服很多",
            "早饭别省，一个鸡蛋也比空着肚子强",
            "睡前把书包收好，早上能多睡十分钟",
            "闹钟放远一点，逼自己下床去关",
            "衣服一次只洗两件，比攒一周轻松",
            "每小时站起来走两分钟，腰会谢谢你",
            "手机别放枕头边充，睡得更沉",
            "提前五分钟到教室，比卡点舒服得多",
            "保温杯带热水，冬天教室特别干",
            "睡前刷手机，换成听十分钟播客",
            "前一晚把衣服搭好，早上不用想",
            "食堂人多就晚十分钟去，排队省一半",
            "眼睛累了看远处二十秒，比滴眼药水管用",
            "书包里常备纸巾，总会用到",
            "钱别都放一个口袋，丢一次就记住了",
            "答应的事写下来，脑子留给正事",
            "天冷加件外套，感冒一场课就全落了",
            "洗完澡再洗头，第二天不塌"
    };

    private final java.util.Random rnd = new java.util.Random();
    private int tipA = -1;
    private int tipB = -1;

    private int pickTip(int avoidSame) {
        int i = rnd.nextInt(TIPS.length);
        int guard = 0;
        while (TIPS.length > 1 && (i == avoidSame || i == tipA || i == tipB) && guard++ < 40) {
            i = rnd.nextInt(TIPS.length);
        }
        return i;
    }

    private LinearLayout content;
    private ScrollView scroller;
    private TextView[] navItems = new TextView[3];
    private LinearLayout drawer;
    private View scrim;
    private boolean drawerOpen = false;
    private int page = 0;
    private int viewWeek = 1;

    private boolean settingsReady = false;
    private int secIdx = 0;
    private Typeface themeFace;
    private boolean faceTried = false;
    private String[] bellVals;
    private EditText amP, amC, amB, pmP, pmC, pmB, weekE, splitE;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String forced = getIntent().getStringExtra("theme");
        if (forced != null && !forced.isEmpty()) {
            Store.get(this).theme = forced;
            Store.save(this);
            Theme.select(this, forced);
        }
        Ui.use(Theme.current(this));
        getWindow().setStatusBarColor(Ui.INK);
        getWindow().setNavigationBarColor(Ui.INK);
        Store.get(this);
        viewWeek = Planner.weekNo(this, Calendar.getInstance());
        if (viewWeek <= 0) {
            viewWeek = 1;
        }
        int p0 = getIntent().getIntExtra("page", 0);
        if (p0 >= 0 && p0 <= 3) {
            page = p0;
        }
        setContentView(buildShell());
        render();
        askPerms();
        AlarmScheduler.reschedule(this);
        final int scrollTo2 = getIntent().getIntExtra("scrollTo", 0);
        if (scrollTo2 > 0) {
            new android.os.Handler().postDelayed(new Runnable() {
                public void run() {
                    if (scroller != null) scroller.scrollTo(0, scrollTo2);
                }
            }, 600);
        }
        if (getIntent().getBooleanExtra("drawer", false)) {
            new android.os.Handler().postDelayed(new Runnable() {
                public void run() {
                    openDrawer();
                }
            }, 500);
        }
        if (getIntent().getBooleanExtra("test", false)) {
            new android.os.Handler().postDelayed(new Runnable() {
                public void run() {
                    testAlarm();
                }
            }, 1200);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoImport();
        if ("clock".equals(Store.get(this).alarmMode)) {
            AlarmScheduler.Next nx = AlarmScheduler.next(this);
            if (nx != null) {
                String key = Planner.ymd(nx.at) + " " + Planner.fmt(nx.at);
                if (!key.equals(Store.get(this).lastPushed)) {
                    pushToSystemClock();
                }
            }
        }
        if (content != null && page == 0) {
            render();
        }
    }

    private Typeface themed() {
        if (faceTried) {
            return themeFace;
        }
        faceTried = true;
        String path = Theme.current(this).fontAsset;
        if (path != null) {
            try {
                themeFace = Typeface.createFromAsset(getAssets(), path);
            } catch (Exception e) {
                themeFace = null;
            }
        }
        return themeFace;
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }

    private void pickBellTime(final int index, final TextView btn) {
        int hh = 8, mm = 0;
        try {
            String[] p = bellVals[index].split(":");
            hh = Integer.parseInt(p[0]);
            mm = Integer.parseInt(p[1]);
        } catch (Exception e) {
            // 用默认值
        }
        LinearLayout box = Ui.row(this);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(12), 0, dp(4));

        final NumberPicker hp = new NumberPicker(this);
        hp.setMinValue(0);
        hp.setMaxValue(23);
        hp.setValue(hh);
        hp.setFormatter(new NumberPicker.Formatter() {
            public String format(int v) {
                return pad2(v);
            }
        });
        final NumberPicker mp = new NumberPicker(this);
        mp.setMinValue(0);
        mp.setMaxValue(59);
        mp.setValue(mm);
        mp.setFormatter(new NumberPicker.Formatter() {
            public String format(int v) {
                return pad2(v);
            }
        });
        box.addView(hp, Ui.lp(dp(78), dp(150)));
        TextView colon = Ui.bold(this, ":", 22, Ui.TX);
        LinearLayout.LayoutParams clp = Ui.lp(-2, -2);
        clp.leftMargin = dp(4);
        clp.rightMargin = dp(4);
        colon.setLayoutParams(clp);
        box.addView(colon);
        box.addView(mp, Ui.lp(dp(78), dp(150)));

        new AlertDialog.Builder(this)
                .setTitle("第 " + (index + 1) + " 节")
                .setView(box)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String v = pad2(hp.getValue()) + ":" + pad2(mp.getValue());
                        bellVals[index] = v;
                        btn.setText(v);
                        Store.Data dd = Store.get(MainActivity.this);
                        dd.bells.set(index, v);
                        Store.save(MainActivity.this);
                        AlarmScheduler.reschedule(MainActivity.this);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void animateEvening(final LinearLayout box, final boolean show) {
        final int from = show ? 0 : box.getHeight();
        int target;
        if (show) {
            int w = box.getWidth() > 0 ? box.getWidth() : content.getWidth();
            box.setVisibility(View.VISIBLE);
            box.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            target = box.getMeasuredHeight();
        } else {
            target = 0;
        }
        final int to = target;
        android.animation.ValueAnimator va =
                android.animation.ValueAnimator.ofInt(from, to);
        va.setDuration(260);
        va.setInterpolator(new android.view.animation.DecelerateInterpolator());
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                int v = (int) a.getAnimatedValue();
                android.view.ViewGroup.LayoutParams lp = box.getLayoutParams();
                lp.height = v;
                box.setLayoutParams(lp);
            }
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator a) {
                android.view.ViewGroup.LayoutParams lp = box.getLayoutParams();
                lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                box.setLayoutParams(lp);
                if (!show) {
                    box.setVisibility(View.GONE);
                    box.setAlpha(1f);
                    box.setTranslationY(0f);
                }
            }
        });
        if (show) {
            box.setAlpha(0f);
            box.setTranslationY(-dp(14));
            box.animate().alpha(1f).translationY(0f).setDuration(260).start();
        } else {
            box.animate().alpha(0f).translationY(-dp(14)).setDuration(200).start();
        }
        va.start();
    }

    private View buildShell() {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout root = Ui.col(this);
        final Theme th0 = Theme.current(this);
        if (th0.bgStyle == 2) {
            root.setBackground(new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{
                            new Hud.Grain(Ui.INK, th0.dotColor, Ui.dp(this, th0.dotStep), 1.1f),
                            new Hud.Vignette()
                    }));
        } else if (th0.bgStyle == 1) {
            root.setBackground(new Hud.Grid(Ui.INK, th0.gridColor, Ui.dp(this, th0.gridStep)));
        } else {
            root.setBackgroundColor(Ui.INK);
        }
        root.setFitsSystemWindows(true);

        final Theme th = Theme.current(this);
        if (th.techHeader) {
            LinearLayout sys = Ui.row(this);
            sys.setGravity(Gravity.CENTER_VERTICAL);
            sys.setPadding(dp(20), dp(10), dp(20), 0);
            TextView sl = Ui.tv(this, th.headerLeft, 9.5f, Ui.DIM);
            sl.setLetterSpacing(0.22f);
            sl.setLayoutParams(Ui.lp(0, -2, 1f));
            sys.addView(sl);
            int wkNum = Planner.weekNo(this, Calendar.getInstance());
            TextView sr = Ui.tv(this, th.headerRight + " · W" + (wkNum > 0 ? wkNum : "--"), 9.5f, Ui.PM);
            sr.setLetterSpacing(0.22f);
            sys.addView(sr);
            View dotOn = new View(this);
            dotOn.setBackground(Ui.circle(Ui.PM));
            LinearLayout.LayoutParams dolp = Ui.lp(dp(5), dp(5));
            dolp.leftMargin = dp(6);
            dotOn.setLayoutParams(dolp);
            sys.addView(dotOn);
            root.addView(sys);
        }
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(12), dp(20), dp(8));

        LinearLayout burger = Ui.col(this);
        burger.setPadding(dp(8), dp(12), dp(12), dp(12));
        for (int bi = 0; bi < 3; bi++) {
            View bar = new View(this);
            bar.setBackgroundColor(Ui.TX);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(17), dp(2));
            if (bi < 2) {
                blp.bottomMargin = dp(4);
            }
            bar.setLayoutParams(blp);
            burger.addView(bar);
        }
        burger.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openDrawer();
            }
        });
        top.addView(burger);

        TextView brand = Ui.bold(this, th.brand, th.techHeader ? 18 : 21, Ui.TX);
        brand.setLetterSpacing(th.techHeader ? 0.12f : -0.02f);
        top.addView(brand);
        TextView tag = Ui.tv(this, "  " + th.brandSub, 10.5f, Ui.DIM);
        tag.setLetterSpacing(0.12f);
        top.addView(tag);
        root.addView(top);

        ScrollView sv = new ScrollView(this);
        scroller = sv;
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        sv.setFillViewport(true);
        content = Ui.col(this);
        content.setPadding(dp(16), dp(4), dp(16), dp(44));
        sv.addView(content);
        root.addView(sv);

        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        scrim = new View(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDrawer();
            }
        });
        frame.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        drawer = Ui.col(this);
        drawer.setBackgroundColor(Ui.CARD);
        drawer.setElevation(dp(10));
        drawer.setFitsSystemWindows(true);
        drawer.setPadding(dp(18), dp(20), dp(18), dp(24));
        buildDrawer();
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(250), -1, Gravity.START);
        frame.addView(drawer, dlp);
        drawer.setTranslationX(-dp(250));
        return frame;
    }

    private void buildDrawer() {
        Theme th = Theme.current(this);
        LinearLayout head = Ui.col(this);
        head.setPadding(0, dp(6), 0, dp(22));
        TextView bn = Ui.bold(this, th.brand, th.techHeader ? 17 : 20, Ui.TX);
        bn.setLetterSpacing(th.techHeader ? 0.12f : -0.02f);
        head.addView(bn);
        head.addView(Ui.tv(this, th.brandSub, 10.5f, Ui.DIM));
        drawer.addView(head);

        String[] names = {"今天", "课表", "设置"};
        navItems = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = Ui.tv(this, names[i], 14.5f, Ui.TX);
            t.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.bottomMargin = dp(3);
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    switchPage(idx);
                    closeDrawer();
                }
            });
            navItems[i] = t;
            drawer.addView(t);
        }
    }

    private void openDrawer() {
        if (drawerOpen) {
            return;
        }
        drawerOpen = true;
        scrim.setVisibility(View.VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(150).start();
        drawer.animate().translationX(0f).setDuration(200).start();
    }

    private void closeDrawer() {
        if (!drawerOpen) {
            return;
        }
        drawerOpen = false;
        scrim.animate().alpha(0f).setDuration(130).withEndAction(new Runnable() {
            public void run() {
                scrim.setVisibility(View.GONE);
            }
        }).start();
        float w = drawer.getWidth() > 0 ? drawer.getWidth() : dp(250);
        drawer.animate().translationX(-w).setDuration(200).start();
    }

    private void switchPage(int idx) {
        if (page == 2) {
            commitSettings(false);
        }
        page = idx;
        render();
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer();
            return;
        }
        if (page == 3) {
            page = 2;
            render();
            return;
        }
        if (page != 0) {
            switchPage(0);
        } else {
            super.onBackPressed();
        }
    }

    private void render() {
        if (content == null) {
            return;
        }
        int keep = 0;
        for (int i = 0; i < 3; i++) {
            boolean on = (i == (page == 3 ? 2 : page));
            TextView v = navItems[i];
            int pf = v.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG;
            switch (Theme.current(this).navStyle) {
                case Theme.NAV_UNDERLINE:
                    v.setPaintFlags(on ? (pf | android.graphics.Paint.UNDERLINE_TEXT_FLAG) : pf);
                    v.setBackground(null);
                    v.setTextColor(on ? Ui.AM : Ui.DIM);
                    break;
                case Theme.NAV_BLOCK:
                    v.setPaintFlags(pf);
                    v.setBackground(on ? Ui.bg(this, Ui.AM, 6) : null);
                    v.setTextColor(on ? Ui.INK : Ui.DIM);
                    break;
                case Theme.NAV_SEGMENT:
                    v.setPaintFlags(pf);
                    v.setBackground(on ? Ui.bg(this, Ui.alpha(Ui.AM, 0x22), 2, Ui.alpha(Ui.AM, 0x66)) : null);
                    v.setTextColor(on ? Ui.AM : Ui.DIM);
                    break;
                default:
                    v.setPaintFlags(pf);
                    v.setBackground(on ? Ui.bg(this, Ui.alpha(Ui.AM, 0x1C), 12) : null);
                    v.setTextColor(on ? Ui.AM : Ui.DIM);
            }
        }
        settingsReady = false;
        secIdx = 0;
        content.removeAllViews();
        content.setAlpha(0f);
        content.setScaleX(0.985f);
        content.setScaleY(0.985f);
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start();
        if (page == 0) {
            renderToday();
        } else if (page == 1) {
            renderWeek();
        } else if (page == 2) {
            renderSettings();
        } else {
            renderThemePage();
        }
    }

    private String latinOf(String cn) {
        if ("今天".equals(cn)) {
            return "TODAY";
        }
        if ("课表".equals(cn)) {
            return "TIMETABLE";
        }
        if ("设置".equals(cn)) {
            return "SETTINGS";
        }
        if ("主题".equals(cn)) {
            return "THEMES";
        }
        return null;
    }

    private void addTitle(String title, String sub) {
        Theme th = Theme.current(this);
        if (th.titleBar || th.latinSub) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            View bar = new View(this);
            bar.setBackground(Ui.bg(this, th.titleBarColor != 0 ? th.titleBarColor : Ui.AM, 0));
            LinearLayout.LayoutParams blp = Ui.lp(dp(5), dp(th.latinSub ? 38 : 30));
            blp.rightMargin = dp(13);
            bar.setLayoutParams(blp);
            row.addView(bar);
            LinearLayout col = Ui.col(this);
            TextView ct = Ui.bold(this, title, 26, Ui.TX);
            Typeface tf = themed();
            if (tf != null) {
                ct.setTypeface(tf);
                ct.setLetterSpacing(0.02f);
            } else if (th.latinSub || th.serifTitle) {
                ct.setTypeface(Typeface.SERIF, Typeface.BOLD);
                ct.setLetterSpacing(0.03f);
            }
            col.addView(ct);
            if (th.latinSub) {
                String en = latinOf(title);
                if (en != null) {
                    TextView lat = Ui.tv(this, en, 10, Ui.MU);
                    lat.setLetterSpacing(0.3f);
                    lat.setPadding(0, dp(1), 0, 0);
                    col.addView(lat);
                }
            }
            row.addView(col);
            content.addView(row);
        } else {
            TextView ht = Ui.bold(this, title, 26, Ui.TX);
            if (th.serifTitle) {
                ht.setTypeface(Typeface.SERIF, Typeface.BOLD);
                ht.setLetterSpacing(0.06f);
            }
            content.addView(ht);
        }
        TextView s = Ui.tv(this, sub, 12, Ui.MU);
        LinearLayout.LayoutParams p = Ui.lp(-2, -2);
        p.topMargin = dp(4);
        p.bottomMargin = dp(18);
        s.setLayoutParams(p);
        content.addView(s);
    }

    private View sectionTitle(String s) {
        Theme th = Theme.current(this);
        if (th.headingBar) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = Ui.lp(-2, -2);
            rp.topMargin = dp(24);
            rp.bottomMargin = dp(10);
            row.setLayoutParams(rp);
            View bar = new View(this);
            bar.setBackground(Ui.bg(this, Ui.AM, 0));
            bar.setLayoutParams(Ui.lp(dp(4), dp(15)));
            row.addView(bar);
            TextView ht = Ui.bold(this, s, 12, Ui.TX);
            ht.setLetterSpacing(0.06f);
            LinearLayout.LayoutParams hp = Ui.lp(-2, -2);
            hp.leftMargin = dp(9);
            ht.setLayoutParams(hp);
            row.addView(ht);
            View rule = new View(this);
            rule.setBackgroundColor(Ui.alpha(Ui.LINE, 0xCC));
            LinearLayout.LayoutParams lp2 = Ui.lp(dp(26), dp(1));
            lp2.leftMargin = dp(10);
            rule.setLayoutParams(lp2);
            row.addView(rule);
            return row;
        }
        String label = s;
        if (th.indexed) {
            label = String.format(java.util.Locale.US, "%02d", ++secIdx) + "  ·  " + s;
        }
        boolean wedge = th.motifs;
        TextView t = Ui.tv(this, th.sectionPrefix + label, 11, wedge ? Ui.INK : Ui.DIM);
        t.setLetterSpacing(wedge ? 0.14f : (th.capsLabels ? 0.1f : 0.16f));
        if (wedge) {
            t.setBackground(new Motif.Wedge(Ui.AM, dp(9)));
            t.setPadding(dp(9), dp(5), dp(15), dp(5));
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        LinearLayout.LayoutParams p = Ui.lp(-2, -2);
        p.topMargin = dp(24);
        p.bottomMargin = dp(8);
        t.setLayoutParams(p);
        return t;
    }

    private TextView hint(String s) {
        TextView t = Ui.tv(this, s, 12, Ui.MU);
        LinearLayout.LayoutParams p = Ui.lp(-2, -2);
        p.topMargin = dp(4);
        t.setLayoutParams(p);
        return t;
    }

    /* ---------------- 今天 ---------------- */

    private void renderToday() {
        Calendar now = Calendar.getInstance();
        int w = Planner.weekNo(this, now);
        String sub = (now.get(Calendar.MONTH) + 1) + "月" + now.get(Calendar.DAY_OF_MONTH)
                + "日 周" + Planner.DN[Planner.dow(now) - 1] + " · "
                + (w > 0 ? "第 " + w + " 教学周" : "学期外");
        addTitle("今天", sub);

        content.addView(alarmCard(true));
        content.addView(Ui.gap(this, 12));
        content.addView(alarmCard(false));

        content.addView(sectionTitle("接下来七天"));
        Calendar d = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            content.addView(dayRow(d));
            d.add(Calendar.DAY_OF_MONTH, 1);
        }

        AlarmScheduler.Next n = AlarmScheduler.next(this);
        TextView tail = Ui.tv(this, n == null
                ? "接下来三周没有课，闹钟不响"
                : "下一次响铃 " + Planner.fmt(n.at) + " · " + (n.morning ? "上午" : "下午")
                + " · " + n.course.name, 12, Ui.DIM);
        LinearLayout.LayoutParams tp = Ui.lp(-2, -2);
        tp.topMargin = dp(16);
        tail.setLayoutParams(tp);
        content.addView(tail);
    }

    private TextView bigText(String s, int color, float size) {
        TextView t = Ui.tv(this, s, size, color);
        t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        t.setLetterSpacing(-0.03f);
        LinearLayout.LayoutParams p = Ui.lp(-2, -2);
        p.topMargin = dp(8);
        t.setLayoutParams(p);
        return t;
    }

    private View alarmCard(boolean morning) {
        Store.Data d = Store.get(this);
        Store.Part part = morning ? d.am : d.pm;
        Calendar day = Calendar.getInstance();
        Store.Course c = Planner.firstOf(this, day, morning);
        Calendar a = Planner.alarmOf(this, day, morning);
        int accent = morning ? Ui.AM : Ui.PM;

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.cardDrawable(this));
        if (Theme.current(this).cardStyle == Theme.STYLE_SOFT) {
            card.setElevation(dp(3));
        }
        card.setPadding(dp(20), dp(17), dp(20), dp(18));

        LinearLayout head = Ui.row(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(Ui.circle(Ui.alpha(accent, 0xCC)));
        LinearLayout.LayoutParams dlp = Ui.lp(dp(6), dp(6));
        dlp.rightMargin = dp(9);
        dot.setLayoutParams(dlp);
        head.addView(dot);
        TextView lab = Ui.tv(this, Theme.current(this).capsLabels
                ? (morning ? "AM · 起床闹钟" : "PM · 出门闹钟")
                : (morning ? "上午 · 起床闹钟" : "下午 · 出门闹钟"), 11, Ui.MU);
        lab.setLetterSpacing(0.1f);
        head.addView(lab);
        if (Theme.current(this).capsLabels && !Theme.current(this).motifs) {
            TextView tagR = Ui.tv(this, morning ? "AM.LOG" : "PM.LOG", 9f, Ui.DIM);
            tagR.setLetterSpacing(0.2f);
            tagR.setGravity(Gravity.END);
            tagR.setLayoutParams(Ui.lp(0, -2, 1f));
            head.addView(tagR);
        }
        card.addView(head);

        if (!part.on) {
            card.addView(bigText("静默", Ui.DIM, 40));
            card.addView(Ui.tv(this, "这个时段的闹钟已关闭", 12.5f, Ui.MU));
        } else if (a == null) {
            card.addView(bigText("无课", Ui.DIM, 40));
            card.addView(Ui.tv(this, "今天" + (morning ? "上午" : "下午") + "没有课，闹钟跳过", 12.5f, Ui.MU));
        } else {
            Theme th = Theme.current(this);
            TextView t = Ui.tv(this, Planner.fmt(a), th.timeSize, accent);
            t.setTypeface(Typeface.create(th.timeFace, Typeface.NORMAL));
            t.setLetterSpacing(th.capsLabels ? 0.02f : -0.04f);
            LinearLayout.LayoutParams tp = Ui.lp(-2, -2);
            tp.topMargin = dp(4);
            t.setLayoutParams(tp);
            card.addView(t);

            TextView nm = Ui.tv(this, c.name, 14.5f, Ui.TX);
            nm.setMaxLines(2);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams np2 = Ui.lp(-2, -2);
            np2.topMargin = dp(12);
            nm.setLayoutParams(np2);
            card.addView(nm);

            LinearLayout foot = Ui.row(this);
            foot.setGravity(Gravity.CENTER_VERTICAL);
            foot.setPadding(0, dp(8), 0, 0);
            StringBuilder sb = new StringBuilder();
            sb.append(Planner.bellOf(this, c.from)).append(" 上课 · ").append(Planner.perLabel(c));
            if (c.place != null && !c.place.isEmpty()) {
                sb.append(" · ").append(c.place);
            }
            TextView meta = Ui.tv(this, sb.toString(), 11.5f, Ui.MU);
            meta.setLayoutParams(Ui.lp(0, -2, 1f));
            meta.setMaxLines(1);
            meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
            foot.addView(meta);
            TextView chip = Ui.tv(this, "提前 " + part.total() + " 分", 10.5f, accent);
            chip.setBackground(Ui.bg(this, Ui.alpha(accent, 0x1F), 20));
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            foot.addView(chip);
            card.addView(foot);
        }
        if (Theme.current(this).motifs) {
            FrameLayout fl = new FrameLayout(this);
            fl.addView(card, new FrameLayout.LayoutParams(-1, -2));
            if (a != null) {
                String hour = Planner.fmt(a).substring(0, 2);
                fl.addView(new Motif.Watermark(this, hour, Ui.alpha(accent, 0x16), 132),
                        new FrameLayout.LayoutParams(-1, -1));
            }
            FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(dp(54), dp(54),
                    Gravity.END | Gravity.TOP);
            rlp.rightMargin = dp(14);
            rlp.topMargin = dp(10);
            fl.addView(new Motif.TickRing(this, accent), rlp);
            return fl;
        }
        return card;
    }

    private View dayRow(Calendar d) {
        LinearLayout wrap = Ui.col(this);
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(13), 0, dp(13));

        String today = Planner.ymd(Calendar.getInstance());
        boolean isToday = Planner.ymd(d).equals(today);
        LinearLayout left = Ui.col(this);
        left.setLayoutParams(Ui.lp(0, -2, 0.72f));
        TextView lbl = Ui.bold(this, isToday ? "今天" : "周" + Planner.DN[Planner.dow(d) - 1],
                13.5f, isToday ? Ui.AM : Ui.TX);
        left.addView(lbl);
        TextView dt = Ui.tv(this, (d.get(Calendar.MONTH) + 1) + "." + d.get(Calendar.DAY_OF_MONTH),
                10.5f, Ui.DIM);
        dt.setLetterSpacing(0.04f);
        left.addView(dt);
        row.addView(left);

        row.addView(alarmCell(d, true));
        row.addView(alarmCell(d, false));
        wrap.addView(row);

        View line = new View(this);
        line.setBackgroundColor(Ui.alpha(Ui.LINE, 0xAA));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
        lp.leftMargin = dp(0);
        line.setLayoutParams(lp);
        wrap.addView(line);
        return wrap;
    }

    private View alarmCell(Calendar d, boolean morning) {
        LinearLayout col = Ui.col(this);
        col.setLayoutParams(Ui.lp(0, -2, 1.25f));
        Calendar a = Planner.alarmOf(this, d, morning);
        if (a == null) {
            TextView dash = Ui.tv(this, "—", 15, 0xFF39414E);
            dash.setLetterSpacing(0.1f);
            col.addView(dash);
        } else {
            Store.Course c = Planner.firstOf(this, d, morning);
            TextView t = Ui.tv(this, Planner.fmt(a), 18, morning ? Ui.AM : Ui.PM);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t.setLetterSpacing(-0.01f);
            col.addView(t);
            TextView n = Ui.tv(this, c.name, 10.5f, Ui.MU);
            n.setMaxLines(2);
            n.setEllipsize(android.text.TextUtils.TruncateAt.END);
            n.setPadding(0, dp(2), dp(6), 0);
            col.addView(n);
        }
        return col;
    }

    /* ---------------- 课表 ---------------- */

    private void renderWeek() {
        addTitle("课表", "点课程修改，长按删除");

        LinearLayout bar = Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = btn("‹", false);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (viewWeek > 1) {
                    viewWeek--;
                    render();
                }
            }
        });
        bar.addView(prev);
        TextView wk = Ui.bold(this, "第 " + viewWeek + " 周", 14, Ui.TX);
        wk.setGravity(Gravity.CENTER);
        wk.setLayoutParams(Ui.lp(0, dp(40), 1f));
        bar.addView(wk);
        Button next = btn("›", false);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (viewWeek < Store.get(MainActivity.this).termWeeks) {
                    viewWeek++;
                    render();
                }
            }
        });
        bar.addView(next);
        Button add = btn("+ 加课", true);
        LinearLayout.LayoutParams ap = Ui.lp(-2, dp(40));
        ap.leftMargin = dp(8);
        add.setLayoutParams(ap);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editCourse(null, Planner.dow(Calendar.getInstance()), 1);
            }
        });
        bar.addView(add);
        content.addView(bar);

        Store.Data d = Store.get(this);
        ArrayList<Store.Course> list = new ArrayList<Store.Course>();
        for (Store.Course c : d.courses) {
            if (Planner.weeksHas(c, viewWeek)) {
                list.add(c);
            }
        }
        Collections.sort(list, new Comparator<Store.Course>() {
            public int compare(Store.Course a, Store.Course b) {
                if (a.day != b.day) {
                    return a.day - b.day;
                }
                return a.from - b.from;
            }
        });

        content.addView(Ui.gap(this, 12));
        if (list.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.tv(this, "这周没有课", 13, Ui.MU));
            empty.addView(Ui.tv(this, "点右上角「+ 加课」，或从下面导入课表", 12, Ui.DIM));
            content.addView(empty);
        }
        for (Store.Course c : list) {
            content.addView(courseCard(c));
            content.addView(Ui.gap(this, 8));
        }

        content.addView(sectionTitle("从外面导入"));
        LinearLayout imp = Ui.row(this);
        Button txt = btn("文本文件", false);
        txt.setLayoutParams(Ui.lp(0, dp(42), 1f));
        txt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickText();
            }
        });
        imp.addView(txt);

        Button ics = btn(".ics 文件", false);
        LinearLayout.LayoutParams ip = Ui.lp(0, dp(42), 1f);
        ip.leftMargin = dp(8);
        ics.setLayoutParams(ip);
        ics.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickIcs();
            }
        });
        imp.addView(ics);

        Button img = btn("图片", false);
        LinearLayout.LayoutParams gp2 = Ui.lp(0, dp(42), 1f);
        gp2.leftMargin = dp(8);
        img.setLayoutParams(gp2);
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickImage();
            }
        });
        imp.addView(img);
        content.addView(imp);

        TextView tips = Ui.tv(this, "文本：一行一门课，字段顺序随意，"
                + "例如「高等数学,周一,1-2,1-16,教三201」或「周三 第5-6节 数据结构 机房A」。\n"
                + "图片：先收下截图，你说一句「识别课表」，我读出来写成课表文件放回手机，"
                + "下次打开 App 自动导入。", 11, Ui.DIM);
        LinearLayout.LayoutParams tp2 = Ui.lp(-2, -2);
        tp2.topMargin = dp(10);
        tips.setLayoutParams(tp2);
        tips.setLineSpacing(dp(4), 1f);
        content.addView(tips);
    }

    private View courseCard(final Store.Course c) {
        LinearLayout card = Ui.card(this);
        card.setElevation(dp(1));
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView day = Ui.bold(this, "周" + Planner.DN[c.day - 1], 11, Ui.AM);
        day.setBackground(Ui.bg(this, Ui.alpha(Ui.AM, 0x1F), 8));
        day.setGravity(Gravity.CENTER);
        day.setLayoutParams(Ui.lp(dp(42), dp(26)));
        top.addView(day);
        TextView name = Ui.bold(this, c.name, 15, Ui.TX);
        LinearLayout.LayoutParams np = Ui.lp(0, -2, 1f);
        np.leftMargin = dp(10);
        name.setLayoutParams(np);
        top.addView(name);
        card.addView(top);
        TextView sub = Ui.tv(this, Planner.perLabel(c) + " · 第 " + c.weeks + " 周 · "
                + (c.place == null || c.place.isEmpty() ? "地点待定" : c.place), 12, Ui.MU);
        LinearLayout.LayoutParams sp = Ui.lp(-2, -2);
        sp.topMargin = dp(6);
        sub.setLayoutParams(sp);
        card.addView(sub);
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editCourse(c, c.day, c.from);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                confirmDelete(c);
                return true;
            }
        });
        return card;
    }

    private void confirmDelete(final Store.Course c) {
        new AlertDialog.Builder(this)
                .setTitle("删除这门课？")
                .setMessage(c.name + " · 周" + Planner.DN[c.day - 1] + " " + Planner.perLabel(c))
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Store.get(MainActivity.this).courses.remove(c);
                        Store.save(MainActivity.this);
                        AlarmScheduler.reschedule(MainActivity.this);
                        render();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private Button btn(String text, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(13);
        if (primary) {
            b.setBackground(Ui.bg(this, Ui.AM, 10));
            b.setTextColor(0xFF1A1208);
            b.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            b.setBackground(Ui.bg(this, Ui.CARD2, 10, Ui.LINE));
            b.setTextColor(Ui.TX);
        }
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setMinimumWidth(0);
        b.setMinWidth(0);
        b.setMinimumHeight(dp(40));
        b.setMinHeight(0);
        return b;
    }

    /* ---------------- 设置 ---------------- */

    private void durationField(LinearLayout parent, String label, int value, final int kind) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, 0);
        TextView t = Ui.tv(this, label, 13, Ui.TX);
        t.setLayoutParams(Ui.lp(0, -2, 1f));
        row.addView(t);

        LinearLayout cell = Ui.row(this);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setBackground(Ui.bg(this, 0x0FFFFFFF, 9, 0x1FFFFFFF));
        cell.setPadding(dp(12), dp(8), dp(12), dp(8));
        cell.setLayoutParams(Ui.lp(dp(104), -2));
        final TextView numv = Ui.bold(this, String.valueOf(value), 13, Ui.TX);
        numv.setGravity(Gravity.CENTER);
        numv.setLayoutParams(Ui.lp(0, -2, 1f));
        cell.addView(numv);
        cell.addView(Ui.tv(this, "分钟", 10.5f, Ui.DIM));
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickDuration(kind, numv);
            }
        });
        row.addView(cell);
        parent.addView(row);
    }

    private void pickDuration(final int kind, final TextView numv) {
        int cur;
        try {
            cur = Integer.parseInt(numv.getText().toString());
        } catch (Exception e) {
            cur = 0;
        }
        LinearLayout box = Ui.row(this);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(12), 0, dp(4));

        final NumberPicker np = new NumberPicker(this);
        np.setMinValue(0);
        np.setMaxValue(30);
        np.setValue(clamp(cur, 0, 30));
        box.addView(np, Ui.lp(dp(96), dp(150)));

        TextView unit = Ui.tv(this, "分钟", 14, Ui.MU);
        LinearLayout.LayoutParams ulp = Ui.lp(-2, -2);
        ulp.leftMargin = dp(14);
        unit.setLayoutParams(ulp);
        box.addView(unit);

        new AlertDialog.Builder(this)
                .setView(box)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        int v = np.getValue();
                        numv.setText(String.valueOf(v));
                        applyNumber(kind, String.valueOf(v));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private EditText numField(LinearLayout parent, String label, int value, final int kind) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, 0);
        TextView t = Ui.tv(this, label, 13, Ui.TX);
        t.setLayoutParams(Ui.lp(0, -2, 1f));
        row.addView(t);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(value));
        e.setTextSize(13);
        e.setGravity(Gravity.CENTER);
        e.setBackground(Ui.bg(this, 0x0FFFFFFF, 9, 0x1FFFFFFF));
        e.setPadding(dp(6), dp(8), dp(6), dp(8));
        e.setLayoutParams(Ui.lp(dp(80), -2));
        e.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable s) {
                applyNumber(kind, s.toString());
            }
        });
        row.addView(e);
        parent.addView(row);
        return e;
    }

    private void applyNumber(int kind, String raw) {
        int v;
        try {
            v = Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            return;
        }
        Store.Data d = Store.get(this);
        switch (kind) {
            case 0:
                d.am.prep = clamp(v, 0, 30);
                break;
            case 1:
                d.am.commute = clamp(v, 0, 30);
                break;
            case 2:
                d.am.buffer = clamp(v, 0, 30);
                break;
            case 3:
                d.pm.prep = clamp(v, 0, 30);
                break;
            case 4:
                d.pm.commute = clamp(v, 0, 30);
                break;
            case 5:
                d.pm.buffer = clamp(v, 0, 30);
                break;
            case 6:
                d.termWeeks = clamp(v, 1, 30);
                break;
            case 7:
                d.split = clamp(v, 1, 10);
                break;
            default:
                return;
        }
        Store.save(this);
        AlarmScheduler.reschedule(this);
    }

    private int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private void renderSettings() {
        addTitle("设置", "闹钟时刻 = 首节课开始 −（洗漱 + 通勤 + 缓冲）");
        Store.Data d = Store.get(this);

        LinearLayout tCard = Ui.card(this);
        LinearLayout tRow = Ui.row(this);
        tRow.setGravity(Gravity.CENTER_VERTICAL);
        tRow.setPadding(0, dp(12), 0, dp(12));
        LinearLayout tLeft = Ui.col(this);
        tLeft.setLayoutParams(Ui.lp(0, -2, 1f));
        tLeft.addView(Ui.tv(this, "主题", 14, Ui.TX));
        tLeft.addView(Ui.tv(this, Theme.current(this).name, 11, Ui.DIM));
        tRow.addView(tLeft);
        tRow.addView(Ui.tv(this, "›", 20, Ui.AM));
        tRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                page = 3;
                render();
            }
        });
        tCard.addView(tRow);
        content.addView(tCard);
        content.addView(Ui.gap(this, 12));

        LinearLayout modeCard = Ui.card(this);
        modeCard.addView(Ui.bold(this, "闹钟方式", 15, Ui.TX));
        modeCard.addView(hint("用哪种方式把你叫醒"));
        String[][] modes = {
                {"app", "应用闹钟", "全屏响铃 + 震动，静音也会响（推荐）"},
                {"notify", "系统提醒", "走系统通知，用系统提示音；静音或勿扰会哑"},
                {"clock", "系统时钟", "写进手机时钟 App，由系统负责响；需要手动同步"}
        };
        for (final String[] md : modes) {
            final boolean on = Store.get(this).alarmMode.equals(md[0]);
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            View dot = new View(this);
            dot.setBackground(on ? Ui.circle(Ui.AM) : Ui.bg(this, 0x00000000, 20, Ui.DIM));
            LinearLayout.LayoutParams dlp = Ui.lp(dp(11), dp(11));
            dlp.rightMargin = dp(11);
            dot.setLayoutParams(dlp);
            row.addView(dot);
            LinearLayout mc = Ui.col(this);
            mc.setLayoutParams(Ui.lp(0, -2, 1f));
            mc.addView(Ui.tv(this, md[1], 14, on ? Ui.AM : Ui.TX));
            mc.addView(Ui.tv(this, md[2], 10.5f, Ui.DIM));
            row.addView(mc);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Store.get(MainActivity.this).alarmMode = md[0];
                    Store.save(MainActivity.this);
                    AlarmScheduler.reschedule(MainActivity.this);
                    render();
                    if ("clock".equals(md[0])) {
                        pushToSystemClock();
                    }
                }
            });
            modeCard.addView(row);
        }
        if ("clock".equals(Store.get(this).alarmMode)) {
            TextView warnLine = Ui.tv(this, "这个模式下 App 自己不再排闹钟。每次打开 App 会自动把「下一次」写进系统时钟，所以要每天开一次。", 10.5f, Ui.DIM);
            warnLine.setPadding(0, dp(6), 0, dp(6));
            modeCard.addView(warnLine);
            Button sync = btn("立即写入系统时钟", false);
            LinearLayout.LayoutParams sp = Ui.lp(-1, dp(42));
            sp.topMargin = dp(8);
            sync.setLayoutParams(sp);
            sync.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    pushToSystemClock();
                }
            });
            modeCard.addView(sync);
        }
        content.addView(modeCard);
        content.addView(Ui.gap(this, 12));

        tipA = pickTip(-1);
        tipB = pickTip(tipA);

        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.bold(this, "上午第一节课", 15, Ui.TX));
        c1.addView(hint(TIPS[tipA]));
        durationField(c1, "洗漱 / 起床", d.am.prep, 0);
        durationField(c1, "通勤", d.am.commute, 1);
        durationField(c1, "缓冲", d.am.buffer, 2);
        content.addView(c1);
        content.addView(Ui.gap(this, 12));

        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.bold(this, "下午第一节课", 15, Ui.TX));
        c2.addView(hint(TIPS[tipB]));
        durationField(c2, "洗漱 / 收拾", d.pm.prep, 3);
        durationField(c2, "通勤", d.pm.commute, 4);
        durationField(c2, "缓冲", d.pm.buffer, 5);
        content.addView(c2);

        LinearLayout c3 = Ui.card(this);
        LinearLayout.LayoutParams c3p = Ui.lp(-1, -2);
        c3p.topMargin = dp(12);
        c3.setLayoutParams(c3p);
        c3.addView(Ui.bold(this, "作息时间表", 15, Ui.TX));
        final TextView bellsHint = hint(d.evening
                ? "一天 8 节正课，第 9-10 节是晚自习；填错会整体偏移"
                : "填错会整体偏移");
        c3.addView(bellsHint);
        bellVals = new String[d.bells.size()];
        final LinearLayout eveningBox = Ui.col(this);
        for (int i = 0; i < d.bells.size(); i += 2) {
            LinearLayout row = Ui.row(this);
            row.setPadding(0, dp(6), 0, 0);
            for (int k = 0; k < 2 && i + k < d.bells.size(); k++) {
                LinearLayout cell = Ui.row(this);
                cell.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams clp = Ui.lp(0, -2, 1f);
                if (k == 0) {
                    clp.rightMargin = dp(16);
                }
                cell.setLayoutParams(clp);
                int no = i + k + 1;
                String label = no <= 8 ? ("第" + no + "节") : ("晚自习" + (no - 8));
                TextView n = Ui.tv(this, label, 11, no <= 8 ? Ui.MU : Ui.DIM);
                n.setLayoutParams(Ui.lp(0, -2, 1f));
                cell.addView(n);
                final int slot = i + k;
                final TextView tb = Ui.tv(this, d.bells.get(slot), 13, Ui.TX);
                tb.setGravity(Gravity.CENTER);
                tb.setBackground(Ui.bg(this, 0x0FFFFFFF, 9, 0x1FFFFFFF));
                tb.setPadding(dp(4), dp(8), dp(4), dp(8));
                tb.setLayoutParams(Ui.lp(dp(78), -2));
                bellVals[slot] = d.bells.get(slot);
                tb.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        pickBellTime(slot, tb);
                    }
                });
                cell.addView(tb);
                row.addView(cell);
            }
            if (i >= 8) {
                eveningBox.addView(row);
            } else {
                c3.addView(row);
            }
        }
        c3.addView(eveningBox);
        eveningBox.setVisibility(d.evening ? View.VISIBLE : View.GONE);
        content.addView(c3);

        LinearLayout evCard = Ui.card(this);
        final boolean evOn = Store.get(this).evening;
        LinearLayout evRow = Ui.row(this);
        evRow.setGravity(Gravity.CENTER_VERTICAL);
        evRow.setPadding(0, dp(2), 0, dp(2));
        TextView evLabel = Ui.tv(this, "晚自习提醒", 14, Ui.TX);
        evLabel.setLayoutParams(Ui.lp(0, -2, 1f));
        evRow.addView(evLabel);
        final TextView evPill = Ui.bold(this, evOn ? "开" : "关", 12, evOn ? Ui.INK : Ui.DIM);
        evPill.setGravity(Gravity.CENTER);
        evPill.setBackground(Ui.bg(this, evOn ? Ui.AM : Ui.alpha(Ui.LINE, 0x66), 20));
        evPill.setPadding(dp(16), dp(6), dp(16), dp(6));
        evRow.addView(evPill);
        evRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean now = !Store.get(MainActivity.this).evening;
                Store.get(MainActivity.this).evening = now;
                Store.save(MainActivity.this);
                AlarmScheduler.reschedule(MainActivity.this);
                animateEvening(eveningBox, now);
                bellsHint.setText(now
                        ? "一天 8 节正课，第 9-10 节是晚自习；填错会整体偏移"
                        : "填错会整体偏移");
                evPill.setText(now ? "开" : "关");
                evPill.setTextColor(now ? Ui.INK : Ui.DIM);
                evPill.setBackground(Ui.bg(MainActivity.this,
                        now ? Ui.AM : Ui.alpha(Ui.LINE, 0x66), 20));
            }
        });
        evCard.addView(evRow);
        content.addView(evCard);
        content.addView(Ui.gap(this, 12));


        LinearLayout c4 = Ui.card(this);
        LinearLayout.LayoutParams c4p = Ui.lp(-1, -2);
        c4p.topMargin = dp(12);
        c4.setLayoutParams(c4p);
        c4.addView(Ui.bold(this, "学期", 15, Ui.TX));
        c4.addView(hint("第一周的周一 + 总周数"));
        LinearLayout dr = Ui.row(this);
        dr.setGravity(Gravity.CENTER_VERTICAL);
        dr.setPadding(0, dp(7), 0, 0);
        TextView dl = Ui.tv(this, "第一周周一", 13, Ui.TX);
        dl.setLayoutParams(Ui.lp(0, -2, 1f));
        dr.addView(dl);
        Button db = btn(d.termStart, false);
        db.setLayoutParams(Ui.lp(dp(130), dp(40)));
        db.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickTermStart();
            }
        });
        dr.addView(db);
        c4.addView(dr);
        weekE = numField(c4, "学期总周数", d.termWeeks, 6);
        splitE = numField(c4, "上午 / 下午分界（第几节后算下午）", d.split, 7);
        content.addView(c4);

        LinearLayout c5 = Ui.card(this);
        LinearLayout.LayoutParams c5p = Ui.lp(-1, -2);
        c5p.topMargin = dp(12);
        c5.setLayoutParams(c5p);
        c5.addView(Ui.bold(this, "停课日", 15, Ui.TX));
        c5.addView(hint("辛苦了，好好休息吧"));
        for (final String s : new ArrayList<String>(d.skip)) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, 0);
            TextView t = Ui.tv(this, s, 13, Ui.TX);
            t.setLayoutParams(Ui.lp(0, -2, 1f));
            row.addView(t);
            TextView del = Ui.tv(this, "删除", 12, 0xFFE0705F);
            del.setPadding(dp(8), dp(4), dp(8), dp(4));
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Store.get(MainActivity.this).skip.remove(s);
                    Store.save(MainActivity.this);
                    AlarmScheduler.reschedule(MainActivity.this);
                    render();
                }
            });
            row.addView(del);
            c5.addView(row);
        }
        Button addSkip = btn("+ 添加停课日", false);
        LinearLayout.LayoutParams asp = Ui.lp(-1, dp(42));
        asp.topMargin = dp(10);
        addSkip.setLayoutParams(asp);
        addSkip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickSkipDay();
            }
        });
        c5.addView(addSkip);
        content.addView(c5);

        LinearLayout c6 = Ui.card(this);
        LinearLayout.LayoutParams c6p = Ui.lp(-1, -2);
        c6p.topMargin = dp(12);
        c6.setLayoutParams(c6p);
        c6.addView(Ui.bold(this, "权限", 15, Ui.TX));
        c6.addView(hint(permText()));
        Button go = btn("打开系统设置", false);
        LinearLayout.LayoutParams gp = Ui.lp(-1, dp(42));
        gp.topMargin = dp(10);
        go.setLayoutParams(gp);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openAppSettings();
            }
        });
        c6.addView(go);

        Button bat = btn("加入电池优化白名单", false);
        LinearLayout.LayoutParams bp = Ui.lp(-1, dp(42));
        bp.topMargin = dp(8);
        bat.setLayoutParams(bp);
        bat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestIgnoreBattery();
            }
        });
        c6.addView(bat);

        Button boot = btn("自启动 / 后台弹出", false);
        LinearLayout.LayoutParams btp = Ui.lp(-1, dp(42));
        btp.topMargin = dp(8);
        boot.setLayoutParams(btp);
        boot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openOemStartup();
            }
        });
        c6.addView(boot);
        content.addView(c6);

        LinearLayout c7 = Ui.card(this);
        LinearLayout.LayoutParams c7p = Ui.lp(-1, -2);
        c7p.topMargin = dp(12);
        c7.setLayoutParams(c7p);
        c7.addView(Ui.bold(this, "测试", 15, Ui.TX));
        c7.addView(hint("10 秒后按真实闹钟流程响一次，确认能响"));
        Button t = btn("10 秒后响", true);
        LinearLayout.LayoutParams tp = Ui.lp(-1, dp(44));
        tp.topMargin = dp(10);
        t.setLayoutParams(tp);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testAlarm();
            }
        });
        c7.addView(t);
        content.addView(c7);



        Button reset = btn("恢复示例课表", false);
        LinearLayout.LayoutParams rp = Ui.lp(-1, dp(44));
        rp.topMargin = dp(10);
        reset.setLayoutParams(rp);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Store.get(MainActivity.this).courses = Store.demoCourses();
                Store.save(MainActivity.this);
                AlarmScheduler.reschedule(MainActivity.this);
                toast("已恢复示例课表");
                render();
            }
        });
        content.addView(reset);

        Button wipe = btn("清空全部课程", false);
        LinearLayout.LayoutParams wp = Ui.lp(-1, dp(44));
        wp.topMargin = dp(8);
        wipe.setLayoutParams(wp);
        wipe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("清空全部课程？")
                        .setMessage("设置和停课日保留，只删课程。")
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                Store.get(MainActivity.this).courses.clear();
                                Store.save(MainActivity.this);
                                AlarmScheduler.reschedule(MainActivity.this);
                                toast("已清空");
                                render();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        content.addView(wipe);

        TextView foot = Ui.tv(this, "国产 ROM 普遍会清理后台闹钟类应用。把上面三项都设好，"
                + "再到系统里允许「自启动」与「后台弹出界面」；不同品牌叫法不一样。", 11, Ui.DIM);
        LinearLayout.LayoutParams fp = Ui.lp(-2, -2);
        fp.topMargin = dp(16);
        foot.setLayoutParams(fp);
        content.addView(foot);

        TextView disc = Ui.tv(this, "各主题为独立的视觉风格设计，未使用《明日方舟》《明日方舟：终末地》"
                + "《流浪地球》的任何官方素材，与上述作品的著作权人、商标权人无任何关联；"
                + "相关名称与商标归各自权利人所有。", 10.5f, Ui.DIM);
        LinearLayout.LayoutParams dp2 = Ui.lp(-2, -2);
        dp2.topMargin = dp(14);
        disc.setLayoutParams(dp2);
        content.addView(disc);

        settingsReady = true;
    }

    private void pushToSystemClock() {
        AlarmScheduler.Next n = AlarmScheduler.next(this);
        if (n == null) {
            toast("接下来二十一天没有闹钟");
            return;
        }
        try {
            Intent i = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            i.putExtra(android.provider.AlarmClock.EXTRA_HOUR, n.at.get(Calendar.HOUR_OF_DAY));
            i.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, n.at.get(Calendar.MINUTE));
            i.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true);
            i.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,
                    n.course == null ? "早八" : ("早八 · " + n.course.name));
            startActivity(i);
            Store.get(this).lastPushed = Planner.ymd(n.at) + " " + Planner.fmt(n.at);
            Store.save(this);
            toast("已交给系统时钟：" + Planner.fmt(n.at));
        } catch (Exception e) {
            toast("这台机器没有可用的系统时钟");
        }
    }

    private void renderThemePage() {
        LinearLayout head = Ui.row(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(2), 0, dp(12));
        TextView back = Ui.tv(this, "‹", 26, Ui.TX);
        back.setPadding(0, 0, dp(14), 0);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                page = 2;
                render();
            }
        });
        head.addView(back);
        LinearLayout ht = Ui.col(this);
        ht.addView(Ui.bold(this, "主题", 22, Ui.TX));
        head.addView(ht);
        content.addView(head);

        for (final Theme opt : Theme.ALL) {
            boolean on = Theme.current(this).id.equals(opt.id);
            LinearLayout card = Ui.col(this);
            card.setPadding(dp(9), dp(9), dp(9), dp(9));
            card.setBackground(Ui.bg(this, on ? Ui.alpha(opt.accent, 0x22) : opt.card, 12,
                    on ? Ui.alpha(opt.accent, 0x88) : Ui.LINE));
            ThemePreview pv = new ThemePreview(this, opt);
            pv.setLayoutParams(Ui.lp(-1, dp(94)));
            card.addView(pv);

            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(5), dp(10), dp(5), 0);
            TextView nm = Ui.tv(this, opt.name, 14, on ? Ui.AM : Ui.TX);
            if (on) {
                nm.setTypeface(Typeface.DEFAULT_BOLD);
            }
            nm.setLayoutParams(Ui.lp(0, -2, 1f));
            row.addView(nm);
            row.addView(Ui.tv(this, on ? "使用中" : "切换", 11.5f, on ? Ui.OK : Ui.DIM));
            card.addView(row);

            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Store.get(MainActivity.this).theme = opt.id;
                    Store.save(MainActivity.this);
                    Theme.select(MainActivity.this, opt.id);
                    Intent it = getIntent();
                    it.putExtra("page", 2);
                    it.putExtra("theme", opt.id);
                    recreate();
                }
            });
            content.addView(card);
            content.addView(Ui.gap(this, 10));
        }
    }

    private void askPerms() {
        AlarmReceiver.ensureChannel(this);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (am != null && !am.canScheduleExactAlarms()) {
                    Intent i = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private String permText() {
        StringBuilder sb = new StringBuilder();
        boolean notif = true;
        if (Build.VERSION.SDK_INT >= 33) {
            notif = checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED;
        }
        boolean exact = true;
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            exact = am != null && am.canScheduleExactAlarms();
        }
        boolean batt = true;
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            batt = pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        sb.append("通知：").append(notif ? "已允许" : "未允许（会收不到提示）").append("\n");
        sb.append("精确闹钟：").append(exact ? "已允许" : "未允许（可能晚几分钟）").append("\n");
        sb.append("电池优化白名单：").append(batt ? "已加入" : "未加入（可能被后台清理）");
        return sb.toString();
    }

    private void requestIgnoreBattery() {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                toast("已经在白名单里了");
                return;
            }
            Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e2) {
                toast("这台机器打不开电池优化设置");
                openAppSettings();
            }
        }
    }

    private void openOemStartup() {
        String[][] list = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"},
                {"com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"},
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
                {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"},
                {"com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings"}
        };
        for (String[] it : list) {
            try {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(it[0], it[1]));
                startActivity(i);
                return;
            } catch (Exception e) {
                // 下一个
            }
        }
        toast("没找到自启动设置页，打开应用详情");
        openAppSettings();
    }

    private void openAppSettings() {
        try {
            Intent i = new Intent();
            i.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            toast("打不开系统设置");
        }
    }

    private int num(EditText e, int def, int min, int max) {
        if (e == null) {
            return def;
        }
        try {
            int v = Integer.parseInt(e.getText().toString().trim());
            if (v < min) {
                v = min;
            }
            if (v > max) {
                v = max;
            }
            return v;
        } catch (Exception ex) {
            return def;
        }
    }

    private void commitSettings(boolean announce) {
        if (!settingsReady) {
            return;
        }
        Store.Data d = Store.get(this);

        d.termWeeks = num(weekE, 18, 1, 30);
        d.split = num(splitE, 4, 1, 10);
        if (bellVals != null) {
            for (int i = 0; i < bellVals.length && i < d.bells.size(); i++) {
                if (bellVals[i] != null && bellVals[i].matches("\\d{1,2}:\\d{2}")) {
                    d.bells.set(i, bellVals[i]);
                }
            }
        }
        Store.save(this);
        AlarmScheduler.reschedule(this);
        if (announce) {
            toast("已保存");
            render();
        }
    }

    private String pad2(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private void pickTermStart() {
        Calendar c = Planner.parseYmd(Store.get(this).termStart);
        if (c == null) {
            c = Calendar.getInstance();
        }
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker v, int y, int m, int d) {
                Calendar x = Calendar.getInstance();
                x.set(y, m, d, 0, 0, 0);
                int dow = Planner.dow(x);
                x.add(Calendar.DAY_OF_MONTH, -(dow - 1));
                Store.get(MainActivity.this).termStart = Planner.ymd(x);
                Store.save(MainActivity.this);
                AlarmScheduler.reschedule(MainActivity.this);
                render();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickSkipDay() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker v, int y, int m, int d) {
                Calendar x = Calendar.getInstance();
                x.set(y, m, d, 0, 0, 0);
                String s = Planner.ymd(x);
                if (!Store.get(MainActivity.this).skip.contains(s)) {
                    Store.get(MainActivity.this).skip.add(s);
                    Store.save(MainActivity.this);
                    AlarmScheduler.reschedule(MainActivity.this);
                    render();
                }
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void testAlarm() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(this, AlarmReceiver.class);
            i.putExtra("title", "出门 · 高等数学");
            i.putExtra("detail", "第1-2节 08:00 上课 @ 教三 201");
            i.putExtra("silent", getIntent().getBooleanExtra("silent", false));
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(this, 9001, i, flags);
            Intent show = new Intent(this, MainActivity.class);
            PendingIntent showPi = PendingIntent.getActivity(this, 9002, show, flags);
            long when = System.currentTimeMillis() + 10000;
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (exact) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            }
            toast("10 秒后响");
        } catch (Exception e) {
            toast("排不上：" + e.getMessage());
        }
    }

    /* ---------------- 课程编辑 ---------------- */

    private EditText rawField(LinearLayout parent, String label, String value, String hintText) {
        LinearLayout row = Ui.col(this);
        row.setPadding(0, dp(8), 0, 0);
        TextView t = Ui.tv(this, label, 11, Ui.MU);
        row.addView(t);
        EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(14);
        e.setHint(hintText);
        e.setSingleLine(true);
        e.setBackground(Ui.bg(this, 0x0FFFFFFF, 9, 0x1FFFFFFF));
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.addView(e);
        parent.addView(row);
        return e;
    }

    private void editCourse(final Store.Course src, int day, int period) {
        Store.Data d = Store.get(this);
        LinearLayout box = Ui.col(this);
        box.setPadding(dp(18), dp(2), dp(18), dp(2));
        final EditText name = rawField(box, "课程名", src == null ? "" : src.name, "高等数学");
        final EditText place = rawField(box, "地点", src == null ? "" : src.place, "教三 201");
        final EditText wd = rawField(box, "星期（1-7）", String.valueOf(src == null ? day : src.day), "");
        wd.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText from = rawField(box, "起始节", String.valueOf(src == null ? period : src.from), "");
        from.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText to = rawField(box, "结束节", String.valueOf(src == null ? period : src.to), "");
        to.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText weeks = rawField(box, "周次", src == null ? ("1-" + d.termWeeks) : src.weeks, "1-16 或 1-16/单");

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(src == null ? "添加课程" : "修改课程");
        b.setView(box);
        b.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int w) {
                String nm = name.getText().toString().trim();
                if (nm.isEmpty()) {
                    toast("先写个课程名");
                    return;
                }
                Store.Data dd = Store.get(MainActivity.this);
                Store.Course c = src == null ? new Store.Course() : src;
                c.name = nm;
                c.place = place.getText().toString().trim();
                int dayV = parseOr(wd, src == null ? 1 : src.day, 1, 7);
                int fromV = parseOr(from, src == null ? 1 : src.from, 1, dd.bells.size());
                int toV = parseOr(to, fromV, fromV, dd.bells.size());
                c.day = dayV;
                c.from = fromV;
                c.to = toV;
                String wv = weeks.getText().toString().trim();
                c.weeks = wv.isEmpty() ? ("1-" + dd.termWeeks) : wv;
                if (src == null) {
                    c.id = "c" + System.currentTimeMillis();
                    dd.courses.add(c);
                }
                Store.save(MainActivity.this);
                AlarmScheduler.reschedule(MainActivity.this);
                toast("已保存");
                render();
            }
        });
        b.setNegativeButton("取消", null);
        if (src != null) {
            b.setNeutralButton("删除", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int w) {
                    confirmDelete(src);
                }
            });
        }
        b.show();
    }

    private int parseOr(EditText e, int def, int min, int max) {
        try {
            int v = Integer.parseInt(e.getText().toString().trim());
            if (v < min) {
                v = min;
            }
            if (v > max) {
                v = max;
            }
            return v;
        } catch (Exception ex) {
            return def;
        }
    }

    /* ---------------- 导入 ---------------- */

    private void pickIcs() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_ICS);
        } catch (Exception e) {
            toast("打不开文件选择器");
        }
    }

    private void pickText() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_TEXT);
        } catch (Exception e) {
            toast("打不开文件选择器");
        }
    }

    private void pickImage() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, REQ_IMG);
        } catch (Exception e) {
            toast("打不开相册");
        }
    }

    private String readAll(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }

    private void handleImage(Uri uri) {
        try {
            java.io.File dir = getExternalFilesDir(null);
            if (dir == null) {
                dir = getFilesDir();
            }
            java.io.File out = new java.io.File(dir, "course-shot.png");
            InputStream is = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
            fos.close();
            is.close();
            new AlertDialog.Builder(this)
                    .setTitle("截图已收下")
                    .setMessage("已存到 App 自己的目录：\n" + out.getAbsolutePath()
                            + "\n\n下一步：在对话里说一句「识别课表」，我把它读成课程列表，"
                            + "写成课表文件放回手机，下次打开 App 会自动导入。")
                    .setPositiveButton("好", null)
                    .show();
        } catch (Exception e) {
            toast("存图片失败：" + e.getMessage());
        }
    }

    /** 我在电脑上识别完课表后，把文本推到 App 目录，这里自动吃进来 */
    private void autoImport() {
        try {
            java.io.File dir = getExternalFilesDir(null);
            if (dir == null) {
                return;
            }
            java.io.File f = new java.io.File(dir, "courses-import.txt");
            if (!f.exists()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new java.io.FileInputStream(f), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            android.util.Log.i("ZAOBA", "autoImport chars=" + sb.length() + " head="
                    + sb.toString().substring(0, Math.min(40, sb.length())));
            String txt = sb.toString().trim();
            ArrayList<Store.Course> got;
            if (txt.startsWith("{")) {
                got = new ArrayList<Store.Course>();
                org.json.JSONObject root = new org.json.JSONObject(txt);
                Store.Data dd = Store.get(this);
                if (root.has("termStart")) {
                    dd.termStart = root.optString("termStart", dd.termStart);
                }
                if (root.has("termWeeks")) {
                    dd.termWeeks = root.optInt("termWeeks", dd.termWeeks);
                }
                if (root.has("split")) {
                    dd.split = root.optInt("split", dd.split);
                }
                org.json.JSONArray bj = root.optJSONArray("bells");
                if (bj != null && bj.length() > 0) {
                    dd.bells.clear();
                    for (int i = 0; i < bj.length(); i++) {
                        dd.bells.add(bj.optString(i, "08:00"));
                    }
                }
                org.json.JSONArray arr = root.optJSONArray("courses");
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    Store.Course c = Store.Course.fromJson(arr.getJSONObject(i));
                    c.id = "x" + System.currentTimeMillis() + i;
                    got.add(c);
                }
                dd.courses.clear();
            } else {
                got = TextParse.parse(txt, Store.get(this));
            }
            f.delete();
            android.util.Log.i("ZAOBA", "autoImport parsed=" + got.size());
            if (got.isEmpty()) {
                toast("导入文件里没认出课程");
                return;
            }
            Store.get(this).courses.addAll(got);
            Store.save(this);
            AlarmScheduler.reschedule(this);
            viewWeek = Planner.weekNo(this, Calendar.getInstance());
            if (viewWeek <= 0) {
                viewWeek = 1;
            }
            toast("已自动导入 " + got.size() + " 门课");
            render();
        } catch (Exception e) {
            android.util.Log.e("ZAOBA", "autoImport fail", e);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (req == REQ_IMG) {
            handleImage(data.getData());
            return;
        }
        if (req != REQ_ICS && req != REQ_TEXT) {
            return;
        }
        try {
            String text = readAll(data.getData());
            ArrayList<Store.Course> got = (req == REQ_ICS)
                    ? Ics.parse(text, Store.get(this))
                    : TextParse.parse(text, Store.get(this));
            if (got.isEmpty()) {
                toast("没认出可导入的课程");
                return;
            }
            Store.get(this).courses.addAll(got);
            Store.save(this);
            AlarmScheduler.reschedule(this);
            viewWeek = Planner.weekNo(this, Calendar.getInstance());
            if (viewWeek <= 0) {
                viewWeek = 1;
            }
            toast("导入 " + got.size() + " 门课，请核对");
            render();
        } catch (Exception e) {
            toast("读取失败：" + e.getMessage());
        }
    }

    private void pasteCsv() {
        final EditText e = new EditText(this);
        e.setHint("高等数学,周一,1-2,1-16,教三201");
        e.setMinLines(5);
        e.setGravity(Gravity.TOP);
        e.setTextSize(13);
        e.setBackground(Ui.bg(this, 0x0FFFFFFF, 9, 0x1FFFFFFF));
        e.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout box = Ui.col(this);
        box.setPadding(dp(18), dp(4), dp(18), dp(4));
        box.addView(Ui.tv(this, "每行一门课：课程名,星期,节次,周次,地点", 11, Ui.MU));
        box.addView(e);
        new AlertDialog.Builder(this)
                .setTitle("粘贴课表")
                .setView(box)
                .setPositiveButton("导入", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        csvIn(e.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void csvIn(String txt) {
        String[] rows = txt.replace("\r\n", "\n").split("\n");
        int n = 0;
        Store.Data d = Store.get(this);
        for (String line : rows) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split("[,，\t]");
            if (p.length < 3) {
                continue;
            }
            String nm = p[0].trim();
            if (nm.isEmpty() || nm.equals("课程名") || nm.equals("课程")) {
                continue;
            }
            String ds = p[1].trim().replace("周", "").replace("星期", "");
            int day = 0;
            String[] map = {"一", "二", "三", "四", "五", "六", "日", "天"};
            for (int i = 0; i < map.length; i++) {
                if (map[i].equals(ds)) {
                    day = (i == 7) ? 7 : i + 1;
                    break;
                }
            }
            if (day == 0) {
                try {
                    day = Integer.parseInt(ds);
                } catch (Exception ex) {
                    continue;
                }
            }
            if (day < 1 || day > 7) {
                continue;
            }
            int from = 1, to = 1;
            String ps = p[2].trim();
            String[] pr = ps.split("[-~到]");
            try {
                from = Integer.parseInt(pr[0].trim());
                to = pr.length > 1 ? Integer.parseInt(pr[1].trim()) : from;
            } catch (Exception ex) {
                continue;
            }
            if (to < from) {
                to = from;
            }
            Store.Course c = new Store.Course();
            c.id = "s" + System.currentTimeMillis() + n;
            c.name = nm;
            c.place = p.length > 4 ? p[4].trim() : "";
            c.day = day;
            c.from = from;
            c.to = to;
            c.weeks = p.length > 3 && !p[3].trim().isEmpty() ? p[3].trim() : ("1-" + d.termWeeks);
            d.courses.add(c);
            n++;
        }
        if (n == 0) {
            toast("一行都没认出来");
            return;
        }
        Store.save(this);
        AlarmScheduler.reschedule(this);
        toast("导入 " + n + " 门课");
        render();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
