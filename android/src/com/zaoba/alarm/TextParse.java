package com.zaoba.alarm;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 宽松文本解析：一行一门课，字段顺序随意、分隔符随意。
 * 能吃下这些写法：
 *   高等数学,周一,1-2,1-16,教三201
 *   周三 第1-2节 概率论与数理统计 教二203 1-16周
 *   星期四 14:00-15:40 马克思主义基本原理 教一105
 *   大学物理 | 周二 | 3-4 | 1-16 | 教五101
 */
public class TextParse {

    private static final Pattern P_DAY = Pattern.compile("(?:周|星期)([一二三四五六日天1-7])");
    private static final Pattern P_DAY_EN = Pattern.compile("(?i)\\b(mon|tue|wed|thu|fri|sat|sun)\\b");
    private static final Pattern P_WEEKS = Pattern.compile("(\\d{1,2})\\s*[-~到]\\s*(\\d{1,2})\\s*周");
    private static final Pattern P_PER2 = Pattern.compile("(?:第)?\\s*(\\d{1,2})\\s*[-~到]\\s*(\\d{1,2})\\s*节");
    private static final Pattern P_PER1 = Pattern.compile("第\\s*(\\d{1,2})\\s*节");
    private static final Pattern P_TIME = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[-~到]\\s*(\\d{1,2}):(\\d{2})");
    private static final Pattern P_RANGE = Pattern.compile("^(\\d{1,2})\\s*[-~到]\\s*(\\d{1,2})$");
    private static final Pattern P_PARITY = Pattern.compile("[/／]\\s*(单|双)");

    public static ArrayList<Store.Course> parse(String raw, Store.Data d) {
        ArrayList<Store.Course> out = new ArrayList<Store.Course>();
        if (raw == null) {
            return out;
        }
        String[] lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        int n = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            Store.Course c = one(line, d, n);
            if (c != null) {
                out.add(c);
                n++;
            }
        }
        return out;
    }

    private static String cut(String src, Matcher m) {
        return src.substring(0, m.start()) + " " + src.substring(m.end());
    }

    private static int dayOf(String s) {
        String[] cn = {"一", "二", "三", "四", "五", "六", "日", "天"};
        for (int i = 0; i < cn.length; i++) {
            if (cn[i].equals(s)) {
                return i == 7 ? 7 : i + 1;
            }
        }
        try {
            int v = Integer.parseInt(s);
            return v >= 1 && v <= 7 ? v : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean looksPlace(String s) {
        if (s == null || s.isEmpty() || s.length() > 14) {
            return false;
        }
        String[] keys = {"教", "楼", "室", "馆", "场", "机房", "实验", "中心", "号", "校区"};
        for (String k : keys) {
            if (s.indexOf(k) >= 0) {
                return true;
            }
        }
        return s.matches("(?i)^[a-z]?\\d{2,4}$");
    }

    private static Store.Course one(String line, Store.Data d, int idx) {
        String rest = line;
        int day = 0;

        Matcher md = P_DAY.matcher(rest);
        if (md.find()) {
            day = dayOf(md.group(1));
            rest = cut(rest, md);
        } else {
            Matcher me = P_DAY_EN.matcher(rest);
            if (me.find()) {
                String g = me.group(1).toLowerCase();
                if (g.startsWith("mo")) {
                    day = 1;
                } else if (g.startsWith("tu")) {
                    day = 2;
                } else if (g.startsWith("we")) {
                    day = 3;
                } else if (g.startsWith("th")) {
                    day = 4;
                } else if (g.startsWith("fr")) {
                    day = 5;
                } else if (g.startsWith("sa")) {
                    day = 6;
                } else {
                    day = 7;
                }
                rest = cut(rest, me);
            }
        }

        String weekStr = "";
        Matcher mw = P_WEEKS.matcher(rest);
        if (mw.find()) {
            weekStr = mw.group(1) + "-" + mw.group(2);
            rest = cut(rest, mw);
        }

        int from = 0, to = 0;
        Matcher mp = P_PER2.matcher(rest);
        if (mp.find()) {
            from = Integer.parseInt(mp.group(1));
            to = Integer.parseInt(mp.group(2));
            rest = cut(rest, mp);
        } else {
            Matcher m1 = P_PER1.matcher(rest);
            if (m1.find()) {
                from = Integer.parseInt(m1.group(1));
                to = from;
                rest = cut(rest, m1);
            }
        }
        if (from == 0) {
            Matcher mt = P_TIME.matcher(rest);
            if (mt.find()) {
                from = Planner.periodOfTime(d, Integer.parseInt(mt.group(1)), Integer.parseInt(mt.group(2)));
                to = Planner.periodForEnd(d, Integer.parseInt(mt.group(3)), Integer.parseInt(mt.group(4)));
                if (to < from) {
                    to = from;
                }
                rest = cut(rest, mt);
            }
        }

        String parity = "";
        Matcher mpa = P_PARITY.matcher(rest);
        if (mpa.find()) {
            parity = "/" + mpa.group(1);
            rest = cut(rest, mpa);
        }

        boolean comma = rest.indexOf(',') >= 0 || rest.indexOf('，') >= 0
                || rest.indexOf('\t') >= 0 || rest.indexOf('|') >= 0
                || rest.indexOf('｜') >= 0 || rest.indexOf(';') >= 0
                || rest.indexOf('；') >= 0 || rest.indexOf('·') >= 0;
        String[] fields;
        if (comma) {
            fields = rest.replace('，', ',').replace('\t', ',').replace('|', ',')
                    .replace('｜', ',').replace(';', ',').replace('；', ',')
                    .replace('·', ',').split(",");
        } else {
            fields = rest.split("\\s+");
        }

        ArrayList<String> nameParts = new ArrayList<String>();
        String place = "";
        for (String raw2 : fields) {
            String f = raw2.trim();
            if (f.isEmpty() || f.equals("周") || f.equals("节") || f.equals("-")) {
                continue;
            }
            String num = f.replace("周", "").replace("节", "").trim();
            Matcher mr = P_RANGE.matcher(num);
            if (mr.find()) {
                if (from == 0) {
                    from = Integer.parseInt(mr.group(1));
                    to = Integer.parseInt(mr.group(2));
                } else if (weekStr.isEmpty()) {
                    weekStr = mr.group(1) + "-" + mr.group(2);
                }
                continue;
            }
            if (f.matches("^\\d{1,2}$")) {
                if (from == 0) {
                    from = Integer.parseInt(f);
                    to = from;
                } else if (weekStr.isEmpty() && nameParts.isEmpty()) {
                    weekStr = f + "-" + f;
                }
                continue;
            }
            if (place.isEmpty() && looksPlace(f) && !nameParts.isEmpty()) {
                place = f;
                continue;
            }
            nameParts.add(f);
        }
        if (nameParts.isEmpty()) {
            // 全是数字/地点，退一步：把地点当名字
            if (!place.isEmpty()) {
                nameParts.add(place);
                place = "";
            } else {
                return null;
            }
        }
        String name = join(nameParts);
        if (name.isEmpty() || name.length() > 40) {
            return null;
        }
        if (weekStr.isEmpty()) {
            weekStr = "1-" + d.termWeeks;
        }
        Store.Course c = new Store.Course();
        c.id = "t" + System.currentTimeMillis() + idx;
        c.name = name;
        c.place = place;
        c.day = day > 0 ? day : 1;
        c.from = from > 0 ? Math.min(from, d.bells.size()) : 1;
        c.to = to >= c.from ? Math.min(to, d.bells.size()) : c.from;
        c.weeks = weekStr + parity;
        return c;
    }

    private static String join(ArrayList<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s);
        }
        return sb.toString().replaceAll("\\s{2,}", " ").trim();
    }
}
