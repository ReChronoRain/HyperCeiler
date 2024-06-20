package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Handler;

import java.util.concurrent.Executor;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class DisableDeviceManaged {
    public static void initDisableDeviceManaged(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(DevicePolicyManager.class, "isDeviceManaged", XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.policy.SecurityController", classLoader, "isDeviceManaged", XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.policy.SecurityController", classLoader, "hasCACertInCurrentUser", XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.policy.SecurityController", classLoader, "hasCACertInWorkProfile", XC_MethodReplacement.returnConstant(false));
        XposedHelpers.findAndHookConstructor("miui.systemui.controlcenter.policy.SecurityController", classLoader, Context.class, Handler.class, Executor.class, "miui.systemui.boardcast.BroadcastDispatcher", "miui.systemui.util.SystemUIResourcesHelper", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "hasCACerts", null);
            }
        });
    }
}
