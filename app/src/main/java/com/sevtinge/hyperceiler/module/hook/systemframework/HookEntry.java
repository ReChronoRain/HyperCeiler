package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HookEntry extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllMethods(
            "com.android.server.MiuiBatteryIntelligence$BatteryNotificationListernerService",
            "isNavigationStatus",
            MethodHook.returnConstant(true)
        );
    }
}
