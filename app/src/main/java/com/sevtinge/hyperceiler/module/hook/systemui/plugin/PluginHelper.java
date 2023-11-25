package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.pm.ApplicationInfo;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.hook.systemui.NotificationVolumeSeparateSlider;
import com.sevtinge.hyperceiler.module.hook.systemui.ShowVolumePct;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.BluetoothTileStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CCGridForHyperOS;


public class PluginHelper extends BaseHook {

    private static ClassLoader pluginLoader = null;

    private static ApplicationInfo appInfo = null;

    @Override
    public void init() {
        if (!isAndroidVersion(34) || !isMoreHyperOSVersion(1f)) {
            String pluginLoaderClass = isAndroidVersion(33)
                ? "com.android.systemui.shared.plugins.PluginInstance$Factory"
                : "com.android.systemui.shared.plugins.PluginManagerImpl";
            hookAllMethods(pluginLoaderClass, "getClassLoader", new MethodHook() {
                    private boolean isHooked = false;
                    // private boolean run = false;

                    @Override
                    protected void after(MethodHookParam param) {
                        appInfo = (ApplicationInfo) param.args[0];
                        if (appInfo != null) {
                            if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                                if (pluginLoader == null) {
                                    pluginLoader = (ClassLoader) param.getResult();
                                }
                                isHooked = true;
                                setClassLoader(pluginLoader);
                                logW(TAG, "im get ClassLoader: " + pluginLoader);
                                // logD("pluginLoader: " + pluginLoader);
                                // setClassLoader(pluginLoader);
                                // logE("PluginHelper", "im get ClassLoader: " + pluginLoader);
                            } else {
                                if (!isHooked)
                                    logW(TAG, "get classloader miui.systemui.plugin error");
                            }
                        } else {
                            logE(TAG, "appInfo is null");
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
                        if (pluginLoader == null) {
                            pluginLoader = (ClassLoader) pathClassLoader;
                        }
                        if (!isHooked) {
                            if (appInfo != null) {
                                if ("miui.systemui.plugin".equals(appInfo.packageName)) {
                                    isHooked = true;
                                    setClassLoader(pluginLoader);
                                    logI(TAG, "im get ClassLoader: " + pluginLoader);
                                    // logD("AU pluginLoader: " + pluginLoader);
                                } else {
                                    logW(TAG, "Au get classloader miui.systemui.plugin error: " + pluginLoader);
                                }
                            } else {
                                if (pluginLoader.toString().contains("MIUISystemUIPlugin") ||
                                    pluginLoader.toString().contains("miui.systemui.plugin")) {
                                    isHooked = true;
                                    setClassLoader(pluginLoader);
                                } else {
                                    logW(TAG, "Au get classloader miui.systemui.plugin error & appInfo is null");
                                }
                            }
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
        if (isMoreHyperOSVersion(1f) && mPrefsMap.getBoolean("system_ui_control_center_rounded_rect"))
            CCGridForHyperOS.initCCGridForHyperOS(classLoader);
        if (mPrefsMap.getBoolean("system_cc_volume_showpct_title"))
            ShowVolumePct.init(classLoader);
    }
}
