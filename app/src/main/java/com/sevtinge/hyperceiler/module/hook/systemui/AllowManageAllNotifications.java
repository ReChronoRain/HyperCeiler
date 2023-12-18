package com.sevtinge.hyperceiler.module.hook.systemui;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.HashSet;

import de.robv.android.xposed.XposedHelpers;

public class AllowManageAllNotifications extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        Class<?> NotifyManagerCls = findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager");
        XposedHelpers.setStaticBooleanField(NotifyManagerCls, "USE_WHITE_LISTS", false);
        findAndHookMethod("com.miui.systemui.NotificationCloudData$Companion", "getFloatBlacklist", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(new ArrayList<String>());
            }
        });

        hookAllMethods("miui.util.NotificationFilterHelper", "isNotificationForcedEnabled", MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "isNotificationForcedFor", Context.class, String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "canSystemNotificationBeBlocked", String.class, MethodHook.returnConstant(true));
        findAndHookMethod("miui.util.NotificationFilterHelper", "containNonBlockableChannel", String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "getNotificationForcedEnabledList", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(new HashSet<String>());
            }
        });

    }
}

