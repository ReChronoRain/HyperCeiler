package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class SuperVolume {
    // Referenced from StarVoyager
    public static void initSuperVolume(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("miui.systemui.util.CommonUtils", classLoader, "supportSuperVolume", XC_MethodReplacement.returnConstant(true));
        XposedHelpers.findAndHookMethod("miui.systemui.util.CommonUtils", classLoader, "voiceSupportSuperVolume", XC_MethodReplacement.returnConstant(true));
    }
}
