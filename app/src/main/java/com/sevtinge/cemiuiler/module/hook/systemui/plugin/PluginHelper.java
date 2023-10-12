package com.sevtinge.cemiuiler.module.hook.systemui.plugin;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidT;

import android.content.pm.ApplicationInfo;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.systemui.controlcenter.BluetoothTileStyle;

public class PluginHelper extends BaseHook {

    private static ClassLoader pluginLoader = null;

    @Override
    public void init() {
        String pluginLoaderClass = isAndroidT()
            ? "com.android.systemui.shared.plugins.PluginInstance$Factory"
            : "com.android.systemui.shared.plugins.PluginManagerImpl";
        hookAllMethods(pluginLoaderClass, "getClassLoader", new MethodHook() {
            private boolean isHooked = false;

            @Override
            protected void after(MethodHookParam param) {
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
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1) > 1)
                        BluetoothTileStyle.initHideDeviceControlEntry(pluginLoader);
                }
            }
        });
    }
}
