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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.MathUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

public class AutoBrightness extends BaseHook {

    private static float mMaximumBacklight;
    private static float mMinimumBacklight;
    private static int backlightMaxLevel;

    private static float constrainValue(float val) {
        if (val < 0) val = 0;
        if (val > 1) val = 1;

        boolean limit_min = mPrefsMap.getBoolean("system_control_center_auto_brightness_min");
        boolean limit_max = mPrefsMap.getBoolean("system_control_center_auto_brightness_max");
        int min_pct = mPrefsMap.getInt("system_ui_auto_brightness_min", 25);
        int max_pct = mPrefsMap.getInt("system_ui_auto_brightness_max", 75);

        float min, max;
        min = MathUtils.convertGammaToLinearFloat(min_pct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight);
        max = MathUtils.convertGammaToLinearFloat(max_pct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight);

        if (limit_min && val < min) val = min;
        if (limit_max && val > max) val = max;
        return val;
    }

    @Override
    public void init() {
        findAndHookMethod("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader,
                "clampScreenBrightness", float.class, new MethodHook() {
                    @Override
                    protected void after(final MethodHookParam param) throws Throwable {
                        float val = (float) param.getResult();
                        if (val >= 0) {
                            float res = constrainValue(val);
                            param.setResult(res);
                        }
                    }
                });

        hookAllConstructors("com.android.server.display.AutomaticBrightnessController",
                lpparam.classLoader, new MethodHook() {
                    @Override
                    protected void after(final MethodHookParam param) throws Throwable {
                        XposedHelpers.setLongField(param.thisObject, "mBrighteningLightDebounceConfig", 1000L);
                        XposedHelpers.setLongField(param.thisObject, "mDarkeningLightDebounceConfig", 1200L);
                    }
                });

        findAndHookMethod("com.android.server.display.DisplayPowerController", lpparam.classLoader,
                "clampScreenBrightness", float.class, new MethodHook() {
                    @Override
                    protected void after(final MethodHookParam param) throws Throwable {
                        float val = (float) param.getResult();
                        if (val >= 0) {
                            float res = constrainValue(val);
                            param.setResult(res);
                        }
                    }
                });

        hookAllConstructors("com.android.server.display.DisplayPowerController",
                lpparam.classLoader, new MethodHook() {
                    @Override
                    @SuppressLint("DiscouragedApi")
                    protected void before(final MethodHookParam param) throws Throwable {
                        Resources res = Resources.getSystem();
                        int minBrightnessLevel = res.getInteger(res.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android"));
                        int maxBrightnessLevel = res.getInteger(res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android"));
                        int backlightBit = res.getInteger(res.getIdentifier("config_backlightBit", "integer", "android.miui"));
                        backlightMaxLevel = (1 << backlightBit) - 1;
                        mMinimumBacklight = (minBrightnessLevel - 1) * 1.0f / (backlightMaxLevel - 1);
                        mMaximumBacklight = (maxBrightnessLevel - 1) * 1.0f / (backlightMaxLevel - 1);
                    }

                    @Override
                    protected void after(final MethodHookParam param) throws Throwable {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                        new PrefsChangeObserver(mContext, mHandler) {
                            @Override
                            public void onChange(PrefType type, Uri uri, String name, Object def) {
                                try {
                                    switch (type) {
                                        case PrefType.Integer -> {
                                            int defVal = "pref_key_system_control_center_min_brightness".equals(name) ? 25 : 75;
                                            mPrefsMap.put(name, PrefsUtils.getSharedIntPrefs(mContext, name, defVal));
                                        }
                                        case PrefType.Boolean ->
                                                mPrefsMap.put(name, PrefsUtils.getSharedBoolPrefs(mContext, name, false));
                                    }
                                } catch (Throwable t) {
                                    AndroidLogUtils.logD(TAG, "onChange", t);
                                }
                            }
                        };
                    }
                });
    }
}
