package com.sevtinge.cemiuiler.module.systemui.plugin;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class PluginHelper extends BaseHook {

    private static ClassLoader pluginLoader = null;

    @Override
    public void init() {
        String pluginLoaderClass = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.shared.plugins.PluginInstance$Factory" : "com.android.systemui.shared.plugins.PluginManagerImpl";
        hookAllMethods(pluginLoaderClass, "getClassLoader", new MethodHook() {
            private boolean isHooked = false;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                    isHooked = true;
                    if (pluginLoader == null) {
                        pluginLoader = (ClassLoader) param.getResult();
                    }
                    if (mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"))
                        EnableVolumeBlur.initEnableVolumeBlur(pluginLoader);
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_smart_hub_entry", 0) != 0)
                        HideMiSmartHubEntry.initHideMiSmartHubEntry(pluginLoader);
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) != 0)
                        HideMiPlayEntry.initHideMiPlayEntry(pluginLoader);
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) != 0)
                        HideDeviceControlEntry.initHideDeviceControlEntry(pluginLoader);
                }
            }
        });
    }
}
