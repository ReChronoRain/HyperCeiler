package com.sevtinge.cemiuiler.module.hook.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AllRotations extends BaseHook {
    @Override
    public void init() {
        hookAllConstructors("com.android.server.wm.DisplayRotation", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setIntField(param.thisObject, "mAllowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations") ? 1 : 0);
            }
        });
    }

    /*public static void initZygote() {
        XposedInit.mResourcesHook.setObjectReplacement("android", "bool", "config_allowAllRotations", mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
    }*/
}
