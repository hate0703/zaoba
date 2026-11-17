package com.zaoba.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CH = "zaoba_alarm";
    public static final int NID = 8801;

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CH) == null) {
                    NotificationChannel ch = new NotificationChannel(CH, "上课闹钟",
                            NotificationManager.IMPORTANCE_HIGH);
                    ch.setDescription("按课表自动排的上课闹钟");
                    ch.setBypassDnd(true);
                    ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    ch.enableVibration(true);
                    nm.createNotificationChannel(ch);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void onReceive(Context c, Intent i) {
        ensureChannel(c);
        boolean notifyOnly = "notify".equals(Store.get(c).alarmMode);
        String title = i == null ? null : i.getStringExtra("title");
        String detail = i == null ? null : i.getStringExtra("detail");
        if (title == null) {
            title = "起床";
        }
        if (detail == null) {
            detail = "该上课了";
        }

        Intent ai = new Intent(c, AlarmActivity.class);
        ai.putExtra("title", title);
        ai.putExtra("detail", detail);
        if (i != null) {
            ai.putExtra("silent", i.getBooleanExtra("silent", false));
        }
        ai.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent fsi = PendingIntent.getActivity(c, 300, ai, flags);

        try {
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(c, CH);
            } else {
                b = new Notification.Builder(c);
                b.setPriority(Notification.PRIORITY_MAX);
            }
            b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(detail)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(fsi);
            if (!notifyOnly) {
                b.setFullScreenIntent(fsi, true);
            }
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NID, b.build());
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            if (!notifyOnly) {
                c.startActivity(ai);
            }
        } catch (Exception e) {
            // 后台启动被限制时靠全屏通知
        }

        AlarmScheduler.reschedule(c);
    }
}
