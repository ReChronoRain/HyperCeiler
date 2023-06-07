package com.sevtinge.cemiuiler.module.systemui.plugin;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class EnableVolumeBlur {
    public static void initEnableVolumeBlur(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.android.systemui.miui.volume.Util", classLoader, "isSupportBlurS", XC_MethodReplacement.returnConstant(true));
    }
}
