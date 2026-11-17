package com.zaoba.alarm;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Planner {

    public static String ymd(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static Calendar midnight(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x;
    }

    public static Calendar parseYmd(String s) {
        if (s == null || s.length() < 10) {
            return null;
        }
        try {
            int y = Integer.parseInt(s.substring(0, 4));
            int m = Integer.parseInt(s.substring(5, 7));
            int d = Integer.parseInt(s.substring(8, 10));
            Calendar c = Calendar.getInstance();
            c.set(y, m - 1, d, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /** 1 = 周一 … 7 = 周日 */
    public static int dow(Calendar c) {
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SUNDAY ? 7 : d - 1;
    }

    public static int weekNo(Context ctx, Calendar day) {
        Store.Data d = Store.get(ctx);
        Calendar st = parseYmd(d.termStart);
        if (st == null) {
            return 0;
        }
        long diff = (midnight(day).getTimeInMillis() - st.getTimeInMillis()) / 86400000L;
        if (diff < 0) {
            return 0;
        }
        int w = (int) (diff / 7) + 1;
        return w <= d.termWeeks ? w : 0;
    }

    /** "1-16/单" → 集合；返回 null 表示不限制 */
    public static ArrayList<Integer> parseWeeks(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String s = raw.trim().replace('，', ',').replace('、', ',').replace('；', ',');
        int parity = -1;
        if (s.contains("/单") || s.toLowerCase().contains("/odd")) {
            parity = 1;
            s = s.replace("/单", "").replace("/单周", "");
        }
        if (s.contains("/双") || s.toLowerCase().contains("/even")) {
            parity = 0;
            s = s.replace("/双", "").replace("/双周", "");
        }
        ArrayList<Integer> out = new ArrayList<Integer>();
        String[] parts = s.split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.isEmpty()) {
                continue;
            }
            int dash = p.indexOf('-');
            if (dash < 0) {
                dash = p.indexOf('~');
            }
            if (dash > 0) {
                try {
                    int a = Integer.parseInt(p.substring(0, dash).trim());
                    int b = Integer.parseInt(p.substring(dash + 1).trim());
                    for (int i = a; i <= b && i < 40; i++) {
                        if (!out.contains(i)) {
                            out.add(i);
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            } else {
                try {
                    int v = Integer.parseInt(p);
                    if (!out.contains(v)) {
                        out.add(v);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        if (parity >= 0) {
            ArrayList<Integer> f = new ArrayList<Integer>();
            for (Integer w : out) {
                if (w % 2 == parity) {
                    f.add(w);
                }
            }
            out = f;
        }
        return out;
    }

    public static boolean weeksHas(Store.Course c, int week) {
        ArrayList<Integer> list = parseWeeks(c.weeks);
        return list == null || list.contains(week);
    }

    public static boolean isSkip(Context ctx, String ymd) {
        return Store.get(ctx).skip.contains(ymd);
    }

    public static ArrayList<Store.Course> coursesOn(Context ctx, Calendar day) {
        ArrayList<Store.Course> out = new ArrayList<Store.Course>();
        int w = weekNo(ctx, day);
        if (w == 0) {
            return out;
        }
        if (isSkip(ctx, ymd(day))) {
            return out;
        }
        int d1 = dow(day);
        for (Store.Course c : Store.get(ctx).courses) {
            if (c.day == d1 && weeksHas(c, w)) {
                out.add(c);
            }
        }
        return out;
    }

    public static Store.Course firstOf(Context ctx, Calendar day, boolean morning) {
        Store.Data d = Store.get(ctx);
        Store.Course best = null;
        for (Store.Course c : coursesOn(ctx, day)) {
            if (!d.evening && c.from > 8) {
                continue;   // 晚自习不参与闹钟
            }
            boolean isAm = c.from <= d.split;
            if (isAm != morning) {
                continue;
            }
            if (best == null || c.from < best.from) {
                best = c;
            }
        }
        return best;
    }

    public static String bellOf(Context ctx, int period) {
        Store.Data d = Store.get(ctx);
        int i = period - 1;
        if (i < 0 || i >= d.bells.size()) {
            return "08:00";
        }
        return d.bells.get(i);
    }

    /** 把钟点映射到最近的节次，差超过 12 分钟认为对不上，返回 0 */
    public static int periodOfTime(Store.Data d, int hh, int mm) {
        int target = hh * 60 + mm;
        int best = 0, bestDiff = 9999;
        for (int i = 0; i < d.bells.size(); i++) {
            String s = d.bells.get(i);
            int c = s.indexOf(':');
            if (c < 0) {
                continue;
            }
            try {
                int v = Integer.parseInt(s.substring(0, c).trim()) * 60
                        + Integer.parseInt(s.substring(c + 1).trim());
                int diff = Math.abs(v - target);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = i + 1;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return bestDiff <= 12 ? best : 0;
    }

    /** 下课时间 → 最后一个已经开始过的节次，用来判定「到第几节」 */
    public static int periodForEnd(Store.Data d, int hh, int mm) {
        int target = hh * 60 + mm;
        int best = 0;
        for (int i = 0; i < d.bells.size(); i++) {
            String s = d.bells.get(i);
            int c = s.indexOf(':');
            if (c < 0) {
                continue;
            }
            try {
                int v = Integer.parseInt(s.substring(0, c).trim()) * 60
                        + Integer.parseInt(s.substring(c + 1).trim());
                if (v <= target + 5) {
                    best = i + 1;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return best;
    }

    public static Calendar alarmOf(Context ctx, Calendar day, boolean morning) {
        Store.Data d = Store.get(ctx);
        Store.Part part = morning ? d.am : d.pm;
        if (!part.on) {
            return null;
        }
        Store.Course c = firstOf(ctx, day, morning);
        if (c == null) {
            return null;
        }
        String t = bellOf(ctx, c.from);
        int hh = 8, mm = 0;
        try {
            int colon = t.indexOf(':');
            hh = Integer.parseInt(t.substring(0, colon).trim());
            mm = Integer.parseInt(t.substring(colon + 1).trim());
        } catch (Exception e) {
            // ignore
        }
        Calendar r = midnight(day);
        r.set(Calendar.HOUR_OF_DAY, hh);
        r.set(Calendar.MINUTE, mm);
        r.add(Calendar.MINUTE, -part.total());
        return r;
    }

    public static String perLabel(Store.Course c) {
        if (c.from == c.to) {
            return "第 " + c.from + " 节";
        }
        return "第 " + c.from + "–" + c.to + " 节";
    }

    public static String fmt(Calendar c) {
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static final String[] DN = {"一", "二", "三", "四", "五", "六", "日"};
}
