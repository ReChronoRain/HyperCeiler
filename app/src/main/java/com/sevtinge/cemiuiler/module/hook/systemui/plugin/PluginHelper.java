package com.sevtinge.cemiuiler.module.hook.systemui.plugin;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidT;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidU;

import android.content.pm.ApplicationInfo;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.systemui.NotificationVolumeSeparateSlider;
import com.sevtinge.cemiuiler.module.hook.systemui.controlcenter.BluetoothTileStyle;

public class PluginHelper extends BaseHook {

    private static ClassLoader pluginLoader = null;

    private static ApplicationInfo appInfo = null;

    @Override
    public void init() {
        if (!isAndroidU()) {
            String pluginLoaderClass = isAndroidT()
                ? "com.android.systemui.shared.plugins.PluginInstance$Factory"
                : "com.android.systemui.shared.plugins.PluginManagerImpl";
            hookAllMethods(pluginLoaderClass, "getClassLoader", new MethodHook() {
                    private boolean isHooked = false;

                    @Override
                    protected void after(MethodHookParam param) {
                        appInfo = (ApplicationInfo) param.args[0];
                        if (appInfo != null) {
                            if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                                isHooked = true;
                                if (pluginLoader == null) {
                                    pluginLoader = (ClassLoader) param.getResult();
                                }
                                // logD("pluginLoader: " + pluginLoader);
                                setClassLoader(pluginLoader);
                            } else {
                                if (!isHooked)
                                    logD("appInfo is not miui.systemui.plugin is: " + appInfo.packageName + " isHooked: " + isHooked);
                            }
                        } else {
                            logE("appInfo is null");
                        }
                    }
                }
            );
        } else {
            hookAllMethods("com.android.systemui.shared.plugins.PluginInstance$Factory",
                "create", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        appInfo = (ApplicationInfo) param.args[1];
                    }
                }
            );

            findAndHookMethod("com.android.systemui.shared.plugins.PluginInstance$Factory$$ExternalSyntheticLambda0",
                "get", new MethodHook() {
                    private boolean isHooked = false;

                    @Override
                    protected void after(MethodHookParam param) {
                        Object pathClassLoader = param.getResult();
                        if (appInfo != null) {
                            if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                                isHooked = true;
                                if (pluginLoader == null) {
                                    pluginLoader = (ClassLoader) pathClassLoader;
                                }
                                // logD("AU pluginLoader: " + pluginLoader);
                                setClassLoader(pluginLoader);
                            } else {
                                if (!isHooked)
                                    logD("AU appInfo is not miui.systemui.plugin is: " + appInfo.packageName + " isHooked: " + isHooked);
                            }
                        } else {
                            logE("AU appInfo is null");
                        }
                    }
                }
            );
        }
    }

    public void setClassLoader(ClassLoader classLoader) {
        if (mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"))
            EnableVolumeBlur.initEnableVolumeBlur(classLoader);
        if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_smart_hub_entry", 0) != 0)
            HideMiSmartHubEntry.initHideMiSmartHubEntry(classLoader);
        if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) != 0)
            HideMiPlayEntry.initHideMiPlayEntry(classLoader);
        if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) != 0)
            HideDeviceControlEntry.initHideDeviceControlEntry(classLoader);
        if (mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1) > 1)
            BluetoothTileStyle.initHideDeviceControlEntry(classLoader);
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control") && mPrefsMap.getBoolean("system_framework_volume_separate_slider"))
            NotificationVolumeSeparateSlider.initHideDeviceControlEntry(classLoader);
    }
}
