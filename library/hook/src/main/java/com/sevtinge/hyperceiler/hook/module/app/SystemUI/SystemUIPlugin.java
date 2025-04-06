package com.sevtinge.hyperceiler.hook.module.app.SystemUI;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.FlashLightNotificationColor;

@HookBase(targetPackage = "miui.systemui.plugin")
public class SystemUIPlugin extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(FlashLightNotificationColor.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_opt_notification_element_background_color"));
    }
}
