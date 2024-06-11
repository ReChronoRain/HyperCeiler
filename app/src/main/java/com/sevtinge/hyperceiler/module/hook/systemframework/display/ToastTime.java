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
package com.sevtinge.hyperceiler.module.hook.systemframework.display;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class ToastTime extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.notification.NotificationManagerService", lpparam.classLoader, "showNextToastLocked",
                new MethodHook() {
                    @Override
                    @SuppressWarnings("unchecked")
                    protected void after(MethodHookParam param) {
                        Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                        Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                        ArrayList<Object> mToastQueue = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mToastQueue");
                        if (mContext == null || mHandler == null || mToastQueue == null || mToastQueue.isEmpty())
                            return;
                        int mod = (PrefsUtils.getSharedIntPrefs(mContext, "pref_key_system_ui_display_toast_times", 0) - 4) * 1000;
                        for (Object record : mToastQueue)
                            if (record != null && mHandler.hasMessages(2, record)) {
                                mHandler.removeCallbacksAndMessages(record);
                                int duration = XposedHelpers.getIntField(record, "duration");
                                int delay = Math.max(1000, (duration == 1 ? 3500 : 2000) + mod);
                                mHandler.sendMessageDelayed(Message.obtain(mHandler, 2, record), delay);
                            }
                    }
                });

        findAndHookMethod("com.android.server.policy.PhoneWindowManager", lpparam.classLoader, "systemReady",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                        new PrefsChangeObserver(mContext, mHandler, true, PrefType.Integer,
                                "pref_key_system_ui_display_toast_times", 0);
                    }
                });

        String windowClass = "com.android.server.wm.DisplayPolicy";
        hookAllMethods(windowClass, lpparam.classLoader, "adjustWindowParamsLw", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                Object lp = param.args.length == 1 ? param.args[0] : param.args[1];
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mPrevHideTimeout", XposedHelpers.getLongField(lp, "hideTimeoutMilliseconds"));
            }

            @Override
            protected void after(MethodHookParam param) {
                Object lp = param.args.length == 1 ? param.args[0] : param.args[1];
                long mPrevHideTimeout = (long) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mPrevHideTimeout");
                long mHideTimeout = XposedHelpers.getLongField(lp, "hideTimeoutMilliseconds");
                if (mPrevHideTimeout == -1 || mHideTimeout == -1) return;

                long dur = 0;
                if (mPrevHideTimeout == 1000 || mPrevHideTimeout == 4000 || mPrevHideTimeout == 5000 || mPrevHideTimeout == 7000 || mPrevHideTimeout != mHideTimeout)
                    dur = Math.max(1000, 3500 + (mPrefsMap.getInt("system_ui_display_toast_times", 0) - 4) * 1000);
                if (dur != 0) XposedHelpers.setLongField(lp, "hideTimeoutMilliseconds", dur);
            }
        });
    }
}
