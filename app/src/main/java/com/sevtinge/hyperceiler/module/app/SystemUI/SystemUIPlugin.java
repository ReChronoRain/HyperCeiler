package com.sevtinge.hyperceiler.module.app.SystemUI;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemui.other.DefaultPluginTheme;
import com.sevtinge.hyperceiler.module.hook.systemui.plugin.FlashLightNotificationColor;

@HookBase(targetPackage = "miui.systemui.plugin")
public class SystemUIPlugin extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(FlashLightNotificationColor.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_opt_notification_element_background_color"));

        initHook(DefaultPluginTheme.INSTANCE, mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"));
    }
}
