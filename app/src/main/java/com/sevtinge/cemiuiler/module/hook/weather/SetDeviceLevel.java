package com.sevtinge.cemiuiler.module.hook.weather;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.XC_MethodReplacement;

public class SetDeviceLevel extends BaseHook {
    Class<?> mUtil;

    @Override
    public void init() {
        if (getPackageVersionCode(lpparam) < 15000000) mUtil = findClassIfExists("miuix.animation.utils.DeviceUtils") ; else mUtil = findClassIfExists("d7.a");
        returnIntConstant(mUtil);
    }

    public static Bundle checkBundle(Context context, Bundle bundle) {
        if (context == null) {
            XposedLogUtils.logI("SetWeatherDeviceLevel", "Context is null!");
            return null;
        }
        if (bundle == null) bundle = new Bundle();
        int order = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "weather_device_level", "0"));
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }

    private void returnIntConstant(Class<?> cls) {
        int order = mPrefsMap.getStringAsInt("weather_device_level", 0);
        if (getPackageVersionCode(lpparam) < 15000000) hookAllMethods(cls, "transDeviceLevel", XC_MethodReplacement.returnConstant(order)); else findAndHookMethod(cls, "j", XC_MethodReplacement.returnConstant(order));
    }
}


