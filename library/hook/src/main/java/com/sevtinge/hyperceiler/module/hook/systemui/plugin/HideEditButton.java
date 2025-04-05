package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;

import de.robv.android.xposed.XposedHelpers;

public class HideEditButton {
    public static void initHideEditButton(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.qs.EditButtonController", classLoader, "available", boolean.class, returnConstant(false));
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.qs.EditButtonController", classLoader, "available", boolean.class, "miui.systemui.controlcenter.panel.main.MainPanelModeController$MainPanelMode", returnConstant(false));
    }
}
