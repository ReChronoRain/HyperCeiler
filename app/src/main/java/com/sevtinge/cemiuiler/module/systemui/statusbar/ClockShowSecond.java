package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ClockShowSecond extends BaseHook {

    Class<?> mMiuiClock;

    @Override
    public void init() {

        mMiuiClock = findClassIfExists("com.android.systemui.statusbar.views.MiuiClock");

        MethodHook ScheduleHook = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                initSecondTimer(param.thisObject);
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter timeSetIntent = new IntentFilter();
                timeSetIntent.addAction("android.intent.action.TIME_SET");
                BroadcastReceiver mUpdateTimeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        initSecondTimer(param.thisObject);
                    }
                };
                mContext.registerReceiver(mUpdateTimeReceiver, timeSetIntent);
            }
        };

        hookAllConstructors("com.android.systemui.statusbar.policy.MiuiStatusBarClockController", ScheduleHook);

        findAndHookMethod(mMiuiClock, "updateTime", new MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void before(MethodHookParam param) {
                TextView clock = (TextView)param.thisObject;
                Context mContext = clock.getContext();
                String clockName = (String) XposedHelpers.getAdditionalInstanceField(clock, "clockName");
                Object mMiuiStatusBarClockController = XposedHelpers.getObjectField(clock, "mMiuiStatusBarClockController");
                Object mCalendar = XposedHelpers.callMethod(mMiuiStatusBarClockController, "getCalendar");
                String timeFmt;
                if ("clock".equals(clockName)) {
                    String fmt;
                    int mAmPmStyle = (int) XposedHelpers.getObjectField(clock, "mAmPmStyle");
                    boolean is24 = (boolean) XposedHelpers.callMethod(mMiuiStatusBarClockController, "getIs24");
                    if (is24) {
                        fmt = "fmt_time_24hour_minute";
                    }
                    else {
                        if (mAmPmStyle == 0) {
                            fmt = "fmt_time_12hour_minute_pm";
                        }
                        else {
                            fmt = "fmt_time_12hour_minute";
                        }
                    }
                    @SuppressLint("DiscouragedApi")
                    int fmtResId = mContext.getResources().getIdentifier(fmt, "string", "com.android.systemui");
                    timeFmt = mContext.getString(fmtResId);
                    timeFmt = timeFmt.replaceFirst(":mm", ":mm:ss");
                    String hourStr = "h";
                    if (is24) {
                        hourStr = "H";
                    }
                    timeFmt = timeFmt.replaceFirst("h+:", hourStr + ":");

                    if (timeFmt != null) {
                        StringBuilder formatSb = new StringBuilder(timeFmt);
                        StringBuilder textSb = new StringBuilder();
                        XposedHelpers.callMethod(mCalendar, "format", mContext, textSb, formatSb);
                        clock.setText(textSb.toString());
                        param.setResult(null);
                    }
                }
            }
        });
    }



    private static void initSecondTimer(Object clockController) {
        boolean sbShowSeconds = mPrefsMap.getBoolean("system_ui_statusbar_clock_show_second");
        Context mContext = (Context) XposedHelpers.getObjectField(clockController, "mContext");
        Timer scheduleTimer = (Timer) XposedHelpers.getAdditionalInstanceField(clockController, "scheduleTimer");
        if (scheduleTimer != null) {
            scheduleTimer.cancel();
        }
        if (sbShowSeconds) {
            final Handler mClockHandler = new Handler(mContext.getMainLooper());
            long delay = 1000 - SystemClock.elapsedRealtime() % 1000;
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mClockHandler.post(() -> {
                        Object mCalendar = XposedHelpers.getObjectField(clockController, "mCalendar");
                        XposedHelpers.callMethod(mCalendar, "setTimeInMillis", System.currentTimeMillis());
                        XposedHelpers.setObjectField(clockController, "mIs24", DateFormat.is24HourFormat(mContext));
                        ArrayList<Object> mClockListeners = (ArrayList<Object>) XposedHelpers.getObjectField(clockController, "mClockListeners");
                        for (Object clock : mClockListeners) {
                            String clockName = (String) XposedHelpers.getAdditionalInstanceField(clock, "clockName");
                            if (clock != null) {
                                if ("ccClock".equals(clockName)) {
                                    XposedHelpers.callMethod(clock, "onTimeChange");
                                } else if ("clock".equals(clockName) && sbShowSeconds) {
                                    XposedHelpers.callMethod(clock, "onTimeChange");
                                }
                            }
                        }
                    });
                }
            }, delay, 1000);
            XposedHelpers.setAdditionalInstanceField(clockController, "scheduleTimer", timer);
        }
    }
}
