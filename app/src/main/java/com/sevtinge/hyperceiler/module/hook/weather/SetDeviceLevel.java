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
package com.sevtinge.hyperceiler.module.hook.weather;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.lang.reflect.Method;

public class SetDeviceLevel extends BaseHook {
    Class<?> mUtil = null;

    @Override
    public void init() {
        if ((mUtil = findClassIfExists("miuix.animation.utils.DeviceUtils")) == null) {
            // 看不懂
            mUtil = findClassIfExists("d7.a");
        }
        returnIntConstant(mUtil);
    }

    public static Bundle checkBundle(Context context, Bundle bundle) {
        if (context == null) {
            logE("SetWeatherDeviceLevel", "com.miui.weather2", "Context is null!");
            return null;
        }
        if (bundle == null) bundle = new Bundle();
        int order = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "weather_device_level", "0"));
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }

    private void returnIntConstant(Class<?> cls) {
        if (cls == null) {
            logE(TAG, "class is null");
            return;
        }
        int order = mPrefsMap.getStringAsInt("weather_device_level", 0);
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getName().equals("transDeviceLevel")) {
                if (method.getReturnType().equals(int.class)
                    // && (method.getParameterTypes().length == 1 &&
                    //     method.getParameterTypes()[0].equals(int.class))
                ) {
                    hookMethod(method, MethodHook.returnConstant(order));
                }
            }
        }
        // if (getPackageVersionCode(lpparam) < 15000000)
        //     hookAllMethods(cls, "transDeviceLevel", MethodHook.returnConstant(order));
        // else
        //     findAndHookMethod(cls, "transDeviceLevel", int.class, XC_MethodReplacement.returnConstant(order));
    }
}
