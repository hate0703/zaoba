package com.zaoba.alarm;

import java.util.ArrayList;
import java.util.Calendar;

public class Ics {

    public static ArrayList<Store.Course> parse(String text, Store.Data d) {
        ArrayList<Store.Course> out = new ArrayList<Store.Course>();
        if (text == null) {
            return out;
        }
        String[] raw = text.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        StringBuilder sb = new StringBuilder();
        for (String ln : raw) {
            if ((ln.startsWith(" ") || ln.startsWith("\t")) && sb.length() > 0) {
                sb.append(ln.substring(1));
            } else {
                sb.append('\n').append(ln);
            }
        }
        String[] lines = sb.toString().split("\n");
        boolean in = false;
        String dtstart = null, dtend = null, rrule = null, summary = null, location = null;
        for (String ln : lines) {
            String u = ln.trim();
            if (u.equalsIgnoreCase("BEGIN:VEVENT")) {
                in = true;
                dtstart = dtend = rrule = summary = location = null;
                continue;
            }
            if (u.equalsIgnoreCase("END:VEVENT")) {
                if (in && dtstart != null) {
                    Store.Course c = build(d, dtstart, dtend, rrule, summary, location);
                    if (c != null) {
                        out.add(c);
                    }
                }
                in = false;
                continue;
            }
            if (!in) {
                continue;
            }
            int c1 = ln.indexOf(':');
            if (c1 < 0) {
                continue;
            }
            String k = ln.substring(0, c1).toUpperCase();
            String v = ln.substring(c1 + 1);
            if (k.startsWith("DTSTART")) {
                dtstart = v;
            } else if (k.startsWith("DTEND")) {
                dtend = v;
            } else if (k.startsWith("RRULE")) {
                rrule = v;
            } else if (k.startsWith("SUMMARY")) {
                summary = v;
            } else if (k.startsWith("LOCATION")) {
                location = v;
            }
        }
        return out;
    }

    private static int[] stamp(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        int t = s.indexOf('T');
        if (t < 8) {
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(s.substring(0, 4)),
                    Integer.parseInt(s.substring(4, 6)),
                    Integer.parseInt(s.substring(6, 8)),
                    Integer.parseInt(s.substring(t + 1, t + 3)),
                    Integer.parseInt(s.substring(t + 3, t + 5))
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static int periodOf(Store.Data d, int hh, int mm) {
        int best = 0, bestDiff = 9999;
        int target = hh * 60 + mm;
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

    private static int byDay(String rrule) {
        String up = rrule.toUpperCase();
        int i = up.indexOf("BYDAY=");
        if (i < 0) {
            return 0;
        }
        String s = up.substring(i + 6);
        int end = s.indexOf(';');
        if (end < 0) {
            end = s.indexOf(',');
        }
        if (end > 0) {
            s = s.substring(0, end);
        }
        s = s.trim();
        if (s.length() < 2) {
            return 0;
        }
        String two = s.substring(0, 2);
        if (two.equals("MO")) return 1;
        if (two.equals("TU")) return 2;
        if (two.equals("WE")) return 3;
        if (two.equals("TH")) return 4;
        if (two.equals("FR")) return 5;
        if (two.equals("SA")) return 6;
        if (two.equals("SU")) return 7;
        return 0;
    }

    private static Store.Course build(Store.Data d, String dtstart, String dtend,
                                      String rrule, String summary, String location) {
        int[] a = stamp(dtstart);
        if (a == null) {
            return null;
        }
        int from = periodOf(d, a[3], a[4]);
        if (from <= 0) {
            return null;
        }
        int to = from;
        int[] b = dtend == null ? null : stamp(dtend);
        if (b != null) {
            int p = periodOf(d, b[3], b[4]);
            if (p >= from) {
                to = p;
            }
        }
        Calendar c0 = Calendar.getInstance();
        c0.set(a[0], a[1] - 1, a[2], a[3], a[4], 0);
        int day = rrule == null ? 0 : byDay(rrule);
        if (day <= 0) {
            day = Planner.dow(c0);
        }
        String weeks = "1-" + d.termWeeks;
        if (rrule != null) {
            String up = rrule.toUpperCase();
            int iu = up.indexOf("UNTIL=");
            int ic = up.indexOf("COUNT=");
            if (iu >= 0) {
                String s = up.substring(iu + 6);
                if (s.length() >= 8) {
                    try {
                        int y = Integer.parseInt(s.substring(0, 4));
                        int m = Integer.parseInt(s.substring(4, 6));
                        int dd = Integer.parseInt(s.substring(6, 8));
                        Calendar end = Calendar.getInstance();
                        end.set(y, m - 1, dd, 0, 0, 0);
                        Calendar st = Planner.parseYmd(d.termStart);
                        if (st != null) {
                            long diff = (Planner.midnight(end).getTimeInMillis()
                                    - st.getTimeInMillis()) / 86400000L;
                            int w = (int) (diff / 7) + 1;
                            if (w >= 1) {
                                weeks = "1-" + Math.min(w, 40);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } else if (ic >= 0) {
                try {
                    String s = up.substring(ic + 6).split("[;,]")[0].trim();
                    int n = Integer.parseInt(s);
                    weeks = "1-" + Math.min(n, d.termWeeks);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        Store.Course c = new Store.Course();
        c.id = "i" + System.currentTimeMillis() + (int) (Math.random() * 9999);
        c.name = summary == null || summary.trim().isEmpty() ? "未命名课程" : summary.trim();
        c.place = location == null ? "" : location.trim();
        c.day = day;
        c.from = from;
        c.to = to;
        c.weeks = weeks;
        return c;
    }
}
