/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class SelectiveHideIconForAlarmClock extends BaseHook {

    private boolean lastAlarmState = false;

    @Override
    public void init() {
        Class<?> miuiPolicy = findClassIfExists("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy");
        Class<?> phonePolicy = findClassIfExists("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy$4");

        hookAllConstructors(miuiPolicy, new ConstructorHook());
        findAndHookMethod(phonePolicy, "onAlarmChanged", boolean.class, new AlarmChangedHook());
    }

    private class ConstructorHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            Context context = (Context) getObjectField(param.getThisObject(), "mContext");
            Object policy = param.getThisObject();

            initAlarmTime(policy, context);
            registerAlarmObserver(policy, context);
            registerTimeReceiver(policy, context);
        }

        private void initAlarmTime(Object policy, Context context) {
            EzxHelpUtils.setAdditionalInstanceField(policy, "mNextAlarmTime", getNextMIUIAlarmTime(context));
        }

        private void registerAlarmObserver(Object policy, Context context) {
            ContentObserver observer = new ContentObserver(new Handler(context.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (selfChange) return;
                    EzxHelpUtils.setAdditionalInstanceField(policy, "mNextAlarmTime", getNextMIUIAlarmTime(context));
                    updateAlarmVisibility(policy, lastAlarmState);
                }
            };
            context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor("next_alarm_clock_formatted"), false, observer);
        }

        private void registerTimeReceiver(Object policy, Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction("android.intent.action.LOCALE_CHANGED");
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    updateAlarmVisibility(policy, lastAlarmState);
                }
            }, filter);
        }
    }

    private class AlarmChangedHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            Object policy = getObjectField(param.getThisObject(), "this$0");
            lastAlarmState = (boolean) getObjectField(policy, "mHasAlarm");
            updateAlarmVisibility(policy, lastAlarmState);
            param.setResult(null);
        }
    }

    private void updateAlarmVisibility(Object policy, boolean hasAlarm) {
        try {
            Object iconController = getObjectField(policy, "mIconController");

            if (!hasAlarm) {
                callMethod(iconController, "setIconVisibility", "alarm_clock", false);
                return;
            }

            Context context = (Context) getObjectField(policy, "mContext");
            long nextAlarmTime = getNextAlarmTime(policy, context);
            boolean shouldShow = shouldShowAlarmIcon(nextAlarmTime);

            callMethod(iconController, "setIconVisibility", "alarm_clock", shouldShow);
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "updateAlarmVisibility failed", t);
        }
    }

    private long getNextAlarmTime(Object policy, Context context) {
        try {
            return (long) EzxHelpUtils.getAdditionalInstanceField(policy, "mNextAlarmTime");
        } catch (Throwable t) {
            long miuiTime = getNextMIUIAlarmTime(context);
            return miuiTime != 0 ? miuiTime : getNextStockAlarmTime(context);
        }
    }

    private boolean shouldShowAlarmIcon(long nextAlarmTime) {
        if (nextAlarmTime == 0) return false;

        long diffMSec = nextAlarmTime - System.currentTimeMillis();
        if (diffMSec < 0) diffMSec += 7 * 24 * 60 * 60 * 1000L;

        float diffHours = (diffMSec - 59 * 1000) / (1000f * 60f * 60f);
        int thresholdHours = PrefsBridge.getInt("system_ui_status_bar_icon_alarm_clock_n", 0);

        return diffHours <= thresholdHours;
    }

    private long getNextMIUIAlarmTime(Context context) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), "next_alarm_clock_formatted");
        if (TextUtils.isEmpty(nextAlarm)) return 0;

        try {
            return parseMIUIAlarmString(context, nextAlarm);
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Parse MIUI alarm failed", t);
            return 0;
        }
    }

    private long parseMIUIAlarmString(Context context, String alarmStr) throws Exception {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
            DateFormat.is24HourFormat(context) ? "EHm" : "Ehma");

        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        dateFormat.setTimeZone(utc);

        long nextTimePart = Objects.requireNonNull(dateFormat.parse(alarmStr)).getTime();

        Calendar utcCal = Calendar.getInstance(utc);
        utcCal.setFirstDayOfWeek(Calendar.MONDAY);
        utcCal.setTimeInMillis(nextTimePart);

        int targetDay = utcCal.get(Calendar.DAY_OF_WEEK);
        int targetHour = utcCal.get(Calendar.HOUR_OF_DAY);
        int targetMinute = utcCal.get(Calendar.MINUTE);

        Calendar localCal = Calendar.getInstance();
        int dayDiff = targetDay - localCal.get(Calendar.DAY_OF_WEEK);
        if (dayDiff < 0) dayDiff += 7;

        localCal.add(Calendar.DAY_OF_MONTH, dayDiff);
        localCal.set(Calendar.HOUR_OF_DAY, targetHour);
        localCal.set(Calendar.MINUTE, targetMinute);
        localCal.clear(Calendar.SECOND);
        localCal.clear(Calendar.MILLISECOND);

        return localCal.getTimeInMillis();
    }

    private long getNextStockAlarmTime(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) return 0;

        AlarmManager.AlarmClockInfo alarmInfo = alarmMgr.getNextAlarmClock();
        return alarmInfo == null ? 0 : alarmInfo.getTriggerTime();
    }
}

