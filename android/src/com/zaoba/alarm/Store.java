package com.zaoba.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class Store {

    public static class Course {
        public String id = "";
        public String name = "";
        public String place = "";
        public String weeks = "1-16";
        public int day = 1;
        public int from = 1;
        public int to = 2;

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("place", place);
                o.put("day", day);
                o.put("from", from);
                o.put("to", to);
                o.put("weeks", weeks);
            } catch (Exception e) {
                // ignore
            }
            return o;
        }

        public static Course fromJson(JSONObject o) {
            Course c = new Course();
            c.id = o.optString("id", "");
            c.name = o.optString("name", "");
            c.place = o.optString("place", "");
            c.weeks = o.optString("weeks", "1-16");
            c.day = o.optInt("day", 1);
            c.from = o.optInt("from", 1);
            c.to = o.optInt("to", 2);
            return c;
        }
    }

    public static class Part {
        public boolean on = true;
        public int prep = 20;
        public int commute = 30;
        public int buffer = 10;

        public int total() {
            return prep + commute + buffer;
        }
    }

    public static class Data {
        public String theme = "dawn";
        public String alarmMode = "app";
        public String lastPushed = "";
        public boolean evening = false;   // 是否启用晚自习提醒
        public String termStart = "";
        public int termWeeks = 18;
        public int split = 4;
        public ArrayList<String> bells = new ArrayList<String>();
        public ArrayList<Course> courses = new ArrayList<Course>();
        public ArrayList<String> skip = new ArrayList<String>();
        public Part am = new Part();
        public Part pm = new Part();
    }

    private static Data d;
    private static SharedPreferences sp;
    private static Context appCtx;

    public static Data get(Context c) {
        if (d != null) {
            return d;
        }
        appCtx = c.getApplicationContext();
        sp = appCtx.getSharedPreferences("zaoba", Context.MODE_PRIVATE);
        String raw = sp.getString("data", null);
        if (raw != null) {
            try {
                d = fromJson(new JSONObject(raw));
            } catch (Exception e) {
                d = null;
            }
        }
        if (d == null) {
            d = makeDefault();
            d.courses = demoCourses();
            saveNow();
        }
        return d;
    }

    public static void save(Context c) {
        get(c);
        saveNow();
    }

    public static void saveNow() {
        try {
            sp.edit().putString("data", toJson(d).toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private static String mondayOfToday() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int back = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        c.add(Calendar.DAY_OF_MONTH, -back);
        return Planner.ymd(c);
    }

    public static Data makeDefault() {
        Data n = new Data();
        n.termStart = mondayOfToday();
        n.termWeeks = 18;
        n.split = 4;
        String[] b = {"08:00", "08:55", "10:00", "10:55", "14:00", "14:55",
                "16:00", "16:55", "19:00", "19:55", "20:45", "21:40"};
        for (String s : b) {
            n.bells.add(s);
        }
        n.am.prep = 20;
        n.am.commute = 30;
        n.am.buffer = 10;
        n.pm.prep = 10;
        n.pm.commute = 30;
        n.pm.buffer = 5;
        return n;
    }

    public static ArrayList<Course> demoCourses() {
        ArrayList<Course> list = new ArrayList<Course>();
        list.add(mk("d1", "高等数学", "教三 201", 1, 1, 2, "1-16"));
        list.add(mk("d2", "大学英语", "外语楼 305", 1, 3, 4, "1-16"));
        list.add(mk("d3", "线性代数", "教二 104", 2, 1, 2, "1-8"));
        list.add(mk("d4", "数据结构", "机房 A", 3, 5, 6, "1-16"));
        list.add(mk("d5", "体育（篮球）", "南操场", 4, 3, 4, "1-16"));
        list.add(mk("d6", "信号与系统", "电院 210", 5, 1, 2, "1-16"));
        list.add(mk("d7", "毛概", "教一 201", 5, 5, 6, "1-12"));
        return list;
    }

    private static Course mk(String id, String name, String place, int day, int from, int to, String weeks) {
        Course c = new Course();
        c.id = id;
        c.name = name;
        c.place = place;
        c.day = day;
        c.from = from;
        c.to = to;
        c.weeks = weeks;
        return c;
    }

    public static JSONObject toJson(Data d) throws Exception {
        JSONObject o = new JSONObject();
        o.put("theme", d.theme);
        o.put("alarmMode", d.alarmMode);
        o.put("lastPushed", d.lastPushed);
        o.put("evening", d.evening);
        o.put("termStart", d.termStart);
        o.put("termWeeks", d.termWeeks);
        o.put("split", d.split);
        JSONArray bells = new JSONArray();
        for (String s : d.bells) {
            bells.put(s);
        }
        o.put("bells", bells);
        JSONArray arr = new JSONArray();
        for (Course c : d.courses) {
            arr.put(c.toJson());
        }
        o.put("courses", arr);
        JSONArray sk = new JSONArray();
        for (String s : d.skip) {
            sk.put(s);
        }
        o.put("skip", sk);
        o.put("am", partJson(d.am));
        o.put("pm", partJson(d.pm));
        return o;
    }

    private static JSONObject partJson(Part p) throws Exception {
        JSONObject o = new JSONObject();
        o.put("on", p.on);
        o.put("prep", p.prep);
        o.put("commute", p.commute);
        o.put("buffer", p.buffer);
        return o;
    }

    private static Part partFrom(JSONObject o) {
        Part p = new Part();
        if (o == null) {
            return p;
        }
        p.on = o.optBoolean("on", true);
        p.prep = o.optInt("prep", 20);
        p.commute = o.optInt("commute", 30);
        p.buffer = o.optInt("buffer", 10);
        return p;
    }

    public static Data fromJson(JSONObject o) throws Exception {
        Data d = new Data();
        d.theme = o.optString("theme", "dawn");
        d.alarmMode = o.optString("alarmMode", "app");
        d.lastPushed = o.optString("lastPushed", "");
        d.evening = o.optBoolean("evening", false);
        d.termStart = o.optString("termStart", mondayOfToday());
        d.termWeeks = o.optInt("termWeeks", 18);
        d.split = o.optInt("split", 4);
        JSONArray bells = o.optJSONArray("bells");
        if (bells != null) {
            for (int i = 0; i < bells.length(); i++) {
                d.bells.add(bells.optString(i, "08:00"));
            }
        }
        if (d.bells.isEmpty()) {
            d.bells = makeDefault().bells;
        }
        JSONArray arr = o.optJSONArray("courses");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject co = arr.optJSONObject(i);
                if (co != null) {
                    d.courses.add(Course.fromJson(co));
                }
            }
        }
        JSONArray sk = o.optJSONArray("skip");
        if (sk != null) {
            for (int i = 0; i < sk.length(); i++) {
                d.skip.add(sk.optString(i, ""));
            }
        }
        d.am = partFrom(o.optJSONObject("am"));
        d.pm = partFrom(o.optJSONObject("pm"));
        return d;
    }
}
