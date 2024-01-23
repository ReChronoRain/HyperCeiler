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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import de.robv.android.xposed.XposedHelpers;

public class SelectiveHideIconForAlarmClock extends BaseHook {

    private boolean lastState = false;

    Class<?> mMiuiPhoneStatusBarPolicy;

    @Override
    public void init() {
        mMiuiPhoneStatusBarPolicy = findClassIfExists("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy");
        hookAllConstructors(mMiuiPhoneStatusBarPolicy, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mNextAlarmTime", getNextMIUIAlarmTime(mContext));
                ContentResolver resolver = mContext.getContentResolver();
                ContentObserver alarmObserver = new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (selfChange) return;
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mNextAlarmTime", getNextMIUIAlarmTime(mContext));
                        updateAlarmVisibility(param.thisObject, lastState);
                    }
                };
                resolver.registerContentObserver(Settings.System.getUriFor("next_alarm_clock_formatted"), false, alarmObserver);

                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_TICK");
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                filter.addAction("android.intent.action.LOCALE_CHANGED");
                final Object thisObject = param.thisObject;
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateAlarmVisibility(thisObject, lastState);
                    }
                }, filter);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", "updateAlarm", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                lastState = (boolean) XposedHelpers.getObjectField(param.thisObject, "mHasAlarm");
                updateAlarmVisibility(param.thisObject, lastState);
            }
        });

        findAndHookMethod(mMiuiPhoneStatusBarPolicy, "onMiuiAlarmChanged", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                lastState = (boolean) XposedHelpers.getObjectField(param.thisObject, "mHasAlarm");
                updateAlarmVisibility(param.thisObject, lastState);
                param.setResult(null);
            }
        });
    }

    private void updateAlarmVisibility(Object thisObject, boolean state) {
        try {
            Object mIconController = XposedHelpers.getObjectField(thisObject, "mIconController");
            if (!state) {
                XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", false);
                return;
            }

            Context mContext = (Context) XposedHelpers.getObjectField(thisObject, "mContext");
            long nowTime = java.lang.System.currentTimeMillis();
            long nextTime;
            try {
                nextTime = (long) XposedHelpers.getAdditionalInstanceField(thisObject, "mNextAlarmTime");
            } catch (Throwable t) {
                nextTime = getNextMIUIAlarmTime(mContext);
            }
            if (nextTime == 0) nextTime = getNextStockAlarmTime(mContext);

            long diffMSec = nextTime - nowTime;
            if (diffMSec < 0) diffMSec += 7 * 24 * 60 * 60 * 1000;
            float diffHours = (diffMSec - 59 * 1000) / (1000f * 60f * 60f);
            boolean vis = diffHours <= mPrefsMap.getInt("system_ui_status_bar_icon_alarm_clock_n", 0);
            XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", vis);
            mIconController = XposedHelpers.getObjectField(thisObject, "miuiDripLeftStatusBarIconController");
            XposedHelpers.callMethod(mIconController, "setIconVisibility", "alarm_clock", vis);
            logI(TAG, this.lpparam.packageName, "Now is " + diffHours + "min remain, show when " + vis + "min remain.");
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "updateAlarmVisibility failed", t);
        }
    }


    public long getNextMIUIAlarmTime(Context context) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), "next_alarm_clock_formatted");
        long nextTime = 0;
        if (!TextUtils.isEmpty(nextAlarm)) try {
            TimeZone timeZone = TimeZone.getTimeZone("UTC");
            SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context) ? "EHm" : "Ehma"), Locale.getDefault());
            dateFormat.setTimeZone(timeZone);
            long nextTimePart = Objects.requireNonNull(dateFormat.parse(nextAlarm)).getTime();

            Calendar cal = Calendar.getInstance(timeZone);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTimeInMillis(nextTimePart);
            int targetDay = cal.get(Calendar.DAY_OF_WEEK);
            int targetHour = cal.get(Calendar.HOUR_OF_DAY);
            int targetMinute = cal.get(Calendar.MINUTE);

            cal = Calendar.getInstance();
            int diff = targetDay - cal.get(Calendar.DAY_OF_WEEK);
            if (diff < 0) diff += 7;

            cal.add(Calendar.DAY_OF_MONTH, diff);
            cal.set(Calendar.HOUR_OF_DAY, targetHour);
            cal.set(Calendar.MINUTE, targetMinute);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            nextTime = cal.getTimeInMillis();
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, t);
        }
        return nextTime;
    }

    public long getNextStockAlarmTime(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) return 0;
        AlarmManager.AlarmClockInfo aci = alarmMgr.getNextAlarmClock();
        return aci == null ? 0 : aci.getTriggerTime();
    }
}
