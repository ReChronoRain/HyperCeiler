package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import java.util.HashSet;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class AllowManageAllNotifications implements IXposedHookZygoteInit  {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws NoSuchMethodException {

        XposedHelpers.findAndHookMethod("android.app.NotificationChannel", startupParam.getClass().getClassLoader(), "isBlockable", HookUtils.MethodHook.returnConstant(true));

        XposedHelpers.findAndHookMethod("android.app.NotificationChannel", startupParam.getClass().getClassLoader(), "setBlockable", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

    }
}
