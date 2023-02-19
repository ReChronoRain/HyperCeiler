package com.sevtinge.cemiuiler.module.weather;

import android.content.Context;
import android.os.Bundle;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class SetDeviceLevel extends BaseHook {
    Class<?> mUtil;

    @Override
    public void init() {

            XposedBridge.log("Cemiuiler: Start to hook package " + lpparam.packageName);
            mUtil = findClassIfExists("miuix.animation.utils.DeviceUtils");
            returnIntConstant(mUtil, "transDeviceLevel");

    }

    public static Bundle checkBundle(Context context, Bundle bundle) {
        if (context == null) {
            LogUtils.log("SetWeatherDeviceLevel" + "Context is null!");
            return null;
        }
        if (bundle == null) bundle = new Bundle();
        int order = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "weather_device_level", "0"));
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        int order = mPrefsMap.getStringAsInt("weather_device_level", 0);
        hookAllMethods(cls, methodName, XC_MethodReplacement.returnConstant(order));
    }
}


