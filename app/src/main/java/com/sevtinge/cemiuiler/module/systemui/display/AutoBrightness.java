package com.sevtinge.cemiuiler.module.systemui.display;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

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
        min = Helpers.convertGammaToLinearFloat(min_pct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight);
        max = Helpers.convertGammaToLinearFloat(max_pct / 100f * backlightMaxLevel, backlightMaxLevel, mMinimumBacklight, mMaximumBacklight);

        if (limit_min && val < min) val = min;
        if (limit_max && val > max) val = max;
        return val;
    }

    @Override
    public void init() {
        Helpers.findAndHookMethod("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader, "clampScreenBrightness", float.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                float val = (float) param.getResult();
                if (val >= 0) {
                    float res = constrainValue(val);
                    param.setResult(res);
                }
            }
        });

        Helpers.hookAllConstructors("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader, new Helpers.MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                XposedHelpers.setLongField(param.thisObject, "mBrighteningLightDebounceConfig", 1000L);
                XposedHelpers.setLongField(param.thisObject, "mDarkeningLightDebounceConfig", 1200L);
            }
        });

        Helpers.findAndHookMethod("com.android.server.display.DisplayPowerController", lpparam.classLoader, "clampScreenBrightness", float.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                float val = (float) param.getResult();
                if (val >= 0) {
                    float res = constrainValue(val);
                    param.setResult(res);
                }
            }
        });

        Helpers.hookAllConstructors("com.android.server.display.DisplayPowerController", lpparam.classLoader, new Helpers.MethodHook() {
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
                new Helpers.SharedPrefObserver(mContext, mHandler) {
                    @Override
                    public void onChange(Uri uri) {
                        try {
                            String type = uri.getPathSegments().get(1);
                            String key = uri.getPathSegments().get(2);
                            switch (type) {
                                case "integer" -> {
                                    int defVal = "pref_key_system_control_center_min_brightness".equals(key) ? 25 : 75;
                                    mPrefsMap.put(key, Helpers.getSharedIntPref(mContext, key, defVal));
                                }
                                case "boolean" -> mPrefsMap.put(key, Helpers.getSharedBoolPref(mContext, key, false));
                            }
                        } catch (Throwable t) {
                            Helpers.log(t);
                        }
                    }
                };
            }
        });
    }
}
