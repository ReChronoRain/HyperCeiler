package com.sevtinge.cemiuiler.module.systemui.plugin;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class HideMiPlayEntry {
    public static void initHideMiPlayEntry(ClassLoader classLoader) {
        if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) == 1) {
            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.external.MiPlayEntryController", classLoader, "available", boolean.class, XC_MethodReplacement.returnConstant(true));
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) == 2) {
            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.external.MiPlayEntryController", classLoader, "available", boolean.class, XC_MethodReplacement.returnConstant(false));
        }
    }
}
