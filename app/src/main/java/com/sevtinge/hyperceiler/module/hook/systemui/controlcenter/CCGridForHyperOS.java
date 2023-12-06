package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.module.base.BaseXposedInit.mPrefsMap;

import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class CCGridForHyperOS {
    public static void initCCGridForHyperOS(ClassLoader classLoader) {
        Class<?> clazz = XposedHelpers.findClass("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader);
        XposedHelpers.findAndHookMethod(clazz, "getCornerRadius", new HookUtils.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) {
                param.setResult((float) mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72));
            }
        });
    }
}
