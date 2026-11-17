package com.zaoba.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class AlarmScheduler {

    public static final int REQ_FIRE = 200;
    public static final int REQ_SHOW = 201;

    public static class Next {
        public Calendar at;
        public boolean morning;
        public Store.Course course;
    }

    private static int piFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    public static Next next(Context c) {
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < 21; i++) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_MONTH, i);
            for (int k = 0; k < 2; k++) {
                boolean morning = (k == 0);
                Calendar a = Planner.alarmOf(c, day, morning);
                if (a == null) {
                    continue;
                }
                if (a.after(now)) {
                    Next n = new Next();
                    n.at = a;
                    n.morning = morning;
                    n.course = Planner.firstOf(c, day, morning);
                    return n;
                }
            }
        }
        return null;
    }

    public static void reschedule(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            PendingIntent cancel = PendingIntent.getBroadcast(c, REQ_FIRE,
                    new Intent(c, AlarmReceiver.class), piFlags());
            am.cancel(cancel);
            if ("clock".equals(Store.get(c).alarmMode)) {
                return;   // 交给系统时钟，本 App 不再自己排
            }
            Next n = next(c);
            if (n == null || n.course == null) {
                return;
            }
            String go = (n.morning ? "起床" : "出门") + " · " + n.course.name;
            String detail = Planner.perLabel(n.course) + " " + Planner.bellOf(c, n.course.from) + " 上课"
                    + (n.course.place == null || n.course.place.isEmpty() ? "" : " @ " + n.course.place);
            Intent fire = new Intent(c, AlarmReceiver.class);
            fire.putExtra("title", go);
            fire.putExtra("detail", detail);
            fire.putExtra("time", Planner.fmt(n.at));
            PendingIntent op = PendingIntent.getBroadcast(c, REQ_FIRE, fire, piFlags());
            Intent show = new Intent(c, MainActivity.class);
            PendingIntent showPi = PendingIntent.getActivity(c, REQ_SHOW, show, piFlags());
            long at = n.at.getTimeInMillis();
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) {
                exact = am.canScheduleExactAlarms();
            }
            if (exact) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showPi), op);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op);
            }
        } catch (Exception e) {
            // 权限缺失时静默失败，主界面会提示
        }
    }
}
