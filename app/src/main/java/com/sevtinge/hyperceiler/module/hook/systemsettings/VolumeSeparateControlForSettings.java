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
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class VolumeSeparateControlForSettings extends BaseHook {

    Class<?> mVsbCls;
    Class<?> mPreferenceGroupCls;
    Class<?> mPreferenceCls;

    public static int mSystemResId;
    public static int mCallsResId;
    public static int mNotificationVolumeResId;

    @Override
    public void init() {

        mVsbCls = findClassIfExists("com.android.settings.sound.VolumeSeekBarPreference");
        mPreferenceGroupCls = findClassIfExists("androidx.preference.PreferenceGroup");
        mPreferenceCls = findClassIfExists("androidx.preference.Preference");

        findAndHookMethod("com.android.settings.MiuiSoundSettings", "onCreate", Bundle.class, new MethodHook() {
            @SuppressLint("DiscouragedApi")
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Object fragment = param.thisObject;
                Context context = (Context) XposedHelpers.callMethod(fragment, "getActivity");
                Resources modRes = getModuleRes(context);
                int order = 6;

                Method[] initSeekBar;
                String addPreference = "addPreference";

                try {
                    initSeekBar = XposedHelpers.findMethodsByExactParameters(fragment.getClass(), void.class, String.class, int.class, int.class);
                    if (mVsbCls == null || initSeekBar.length == 0) {
                        logE(TAG, VolumeSeparateControlForSettings.this.lpparam.packageName, "Unable to find class/method in Settings to hook");
                        return;
                    } else {
                        initSeekBar[0].setAccessible(true);
                    }

                    Method[] methods = XposedHelpers.findMethodsByExactParameters(mPreferenceGroupCls, void.class, mPreferenceCls);
                    for (Method method : methods) {
                        if (Modifier.isPublic(method.getModifiers())) {
                            addPreference = method.getName();
                            break;
                        }
                    }
                } catch (Throwable t) {
                    logE(TAG, VolumeSeparateControlForSettings.this.lpparam.packageName, "Unable to find class/method in Settings to hook", t);
                    return;
                }

                Object media = XposedHelpers.callMethod(fragment, "findPreference", "media_volume");
                if (media != null) order = (int) XposedHelpers.callMethod(media, "getOrder");

                Object prefScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen");
                Object pref = XposedHelpers.newInstance(mVsbCls, context);

                XposedHelpers.callMethod(pref, "setKey", "system_volume");
                XposedHelpers.callMethod(pref, "setTitle", modRes.getString(R.string.system_volume));
                XposedHelpers.callMethod(pref, "setPersistent", true);
                XposedHelpers.callMethod(prefScreen, addPreference, pref);
                initSeekBar[0].invoke(fragment, "system_volume", 1, context.getResources().getIdentifier("ic_audio_vol", "drawable", context.getPackageName()));
                XposedHelpers.callMethod(pref, "setOrder", order);

                pref = XposedHelpers.newInstance(mVsbCls, context);
                XposedHelpers.callMethod(pref, "setKey", "notification_volume");
                XposedHelpers.callMethod(pref, "setTitle", modRes.getString(R.string.notification_volume));
                XposedHelpers.callMethod(pref, "setPersistent", true);
                XposedHelpers.callMethod(prefScreen, addPreference, pref);
                initSeekBar[0].invoke(fragment, "notification_volume", 5, context.getResources().getIdentifier("ic_audio_ring_notif", "drawable", context.getPackageName()));
                XposedHelpers.callMethod(pref, "setOrder", order);

                Object mRingVolume = XposedHelpers.callMethod(param.thisObject, "findPreference", "ring_volume");
                XposedHelpers.callMethod(mRingVolume, "setTitle", mCallsResId);
            }
        });
    }


    public static void initRes() {
        mSystemResId = R.drawable.ic_audio_system;
        mNotificationVolumeResId = R.drawable.ic_miui_volume_notification;
        /*mCallsResId = XposedInit.mResHook.addResource("ring_volume_option_newtitle", R.string.calls);*/
    }
}
