package com.sevtinge.cemiuiler.module.systemframework;

import android.content.Context;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class ScreenRotation extends BaseHook {

    @Override
    public void init() {

        findAndHookMethod("com.android.internal.view.RotationPolicy", "areAllRotationsAllowed", Context.class, XC_MethodReplacement.returnConstant(mPrefsMap.getBoolean("system_framework_screen_all_rotations")));

        hookAllConstructors("com.android.server.wm.DisplayRotation", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setIntField(param.thisObject, "mAllowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations") ? 1 : 0);
            }
        });
    }

    public static void initRes() {
        XposedInit.mResHook.setObjectReplacement("android", "bool", "config_allowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
    }
}
