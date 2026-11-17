package com.zaoba.alarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public class AlarmActivity extends Activity {

    private Ringtone ring;
    private Vibrator vib;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        String title = getIntent().getStringExtra("title");
        String detail = getIntent().getStringExtra("detail");
        if (title == null) {
            title = "起床";
        }
        if (detail == null) {
            detail = "";
        }

        Calendar now = Calendar.getInstance();
        String hhmm = String.format(Locale.US, "%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.INK);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 28), Ui.dp(this, 40), Ui.dp(this, 28), Ui.dp(this, 40));

        TextView tag = Ui.tv(this, "上课闹钟", 12, Ui.MU);
        tag.setLetterSpacing(0.22f);
        root.addView(tag);

        TextView time = Ui.tv(this, hhmm, 76, Ui.AM);
        time.setLetterSpacing(-0.03f);
        LinearLayout.LayoutParams tp = Ui.lp(-2, -2);
        tp.topMargin = Ui.dp(this, 14);
        time.setLayoutParams(tp);
        root.addView(time);

        TextView t2 = Ui.bold(this, title, 21, Ui.TX);
        LinearLayout.LayoutParams t2p = Ui.lp(-2, -2);
        t2p.topMargin = Ui.dp(this, 18);
        t2.setLayoutParams(t2p);
        t2.setGravity(Gravity.CENTER);
        root.addView(t2);

        TextView d2 = Ui.tv(this, detail, 14, Ui.MU);
        LinearLayout.LayoutParams d2p = Ui.lp(-2, -2);
        d2p.topMargin = Ui.dp(this, 8);
        d2.setLayoutParams(d2p);
        d2.setGravity(Gravity.CENTER);
        root.addView(d2);

        TextView stop = Ui.bold(this, "停 止", 17, 0xFF1A1208);
        stop.setGravity(Gravity.CENTER);
        stop.setBackground(Ui.bg(this, Ui.AM, 14));
        LinearLayout.LayoutParams sp = Ui.lp(Ui.dp(this, 190), Ui.dp(this, 56));
        sp.topMargin = Ui.dp(this, 46);
        stop.setLayoutParams(sp);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopAll();
            }
        });
        root.addView(stop);

        TextView hint = Ui.tv(this, "点击停止，或按音量键静音", 11, Ui.DIM);
        LinearLayout.LayoutParams hp = Ui.lp(-2, -2);
        hp.topMargin = Ui.dp(this, 18);
        hint.setLayoutParams(hp);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);

        setContentView(root);
        if (!getIntent().getBooleanExtra("silent", false)) {
            playAlarm();
        }
    }

    private void playAlarm() {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) {
                u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (u == null) {
                u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ring = RingtoneManager.getRingtone(this, u);
            if (ring != null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    ring.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    ring.setLooping(true);
                }
                ring.play();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 700, 500};
            if (vib != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vib.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void stopAll() {
        try {
            if (ring != null) {
                ring.stop();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (vib != null) {
                vib.cancel();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(AlarmReceiver.NID);
            }
        } catch (Exception e) {
            // ignore
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        stopAll();
    }

    @Override
    protected void onDestroy() {
        try {
            if (ring != null) {
                ring.stop();
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            if (vib != null) {
                vib.cancel();
            }
        } catch (Exception e) {
            // ignore
        }
        super.onDestroy();
    }
}
