/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.content.*
import android.text.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.*
import com.sevtinge.hyperceiler.module.hook.systemui.other.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v.*
import com.sevtinge.hyperceiler.utils.api.*
import java.lang.ref.*

// https://github.com/buffcow/Hyper5GSwitch/blob/master/app/src/main/kotlin/cn/buffcow/hyper5g/hooker/PluginLoader.kt
object NewPluginHelperKt : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.shared.plugins.PluginInstance\$PluginFactory")
            .methodFinder().filterByName("createPluginContext")
            .first().createAfterHook {
                runCatching {
                    val wrapper = it.result as ContextWrapper
                    onPluginLoaded(PluginFactory(it.thisObject).also { it.pluginCtxRef = WeakReference(wrapper) })
                }.onFailure {
                    logE(TAG, lpparam.packageName, "Failed to create plugin context.")
                    return@createAfterHook
                }
            }
    }

    private fun onPluginLoaded(factory: PluginFactory) {
        val mCardStyleTiles = getTileList()
        val mIsDefaultMode = mPrefsMap.getStringAsInt("system_ui_plugin_tiles_load_way", 0) == 0 || mPrefsMap.getStringAsInt("system_ui_plugin_tiles_load_way", 0) == 1
        val mIsCompatibilityMode = mPrefsMap.getStringAsInt("system_ui_plugin_tiles_load_way", 0) == 0 || mPrefsMap.getStringAsInt("system_ui_plugin_tiles_load_way", 0) == 2
        try {
            when (factory.mComponentName) {
                factory.componentNames("miui.systemui.volume.VolumeDialogPlugin") -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                    logD(TAG, lpparam.packageName, "Plugin for sysui volume loaded.")

                    if (mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"))
                        EnableVolumeBlur.initEnableVolumeBlur(classLoader)
                    if (mPrefsMap.getBoolean("system_cc_volume_showpct_title"))
                        NewShowVolumePct.initLoader(classLoader) // 声音百分比
                    if (mPrefsMap.getBoolean("system_ui_unlock_super_volume"))
                        NewSuperVolume.initSuperVolume(classLoader) // 超大音量
                    if (mPrefsMap.getBoolean("system_framework_volume_separate_control") &&
                        mPrefsMap.getBoolean("system_framework_volume_separate_slider"))
                        NotificationVolumeSeparateSlider.initHideDeviceControlEntry(classLoader)
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                }

                factory.componentNames("miui.systemui.miplay.MiPlayPluginImpl") -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                    logD(TAG, lpparam.packageName, "Plugin for sysui mipay loaded.")

                    if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) != 0)
                        HideMiPlayEntry.initHideMiPlayEntry(classLoader)
                }

                factory.componentNames("miui.systemui.quicksettings.LocalMiuiQSTilePlugin") -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                    logD(TAG, lpparam.packageName, "Plugin for sysui qs tiles loaded.")

                    if (mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                        mPrefsMap.getString("systemui_plugin_card_tiles", "").isNotEmpty() &&
                        mIsCompatibilityMode
                    ) {
                        CustomCardTiles.initCustomCardTiles(classLoader, mCardStyleTiles)
                    }
                    if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect") && mIsDefaultMode)
                        CCGridForHyperOS.initCCGridForHyperOS(classLoader) // 控制中心磁贴圆角         //A
                    if (mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                        mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color")
                    ) {
                        QSColor.pluginHook(classLoader)
                    }
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                }

                factory.componentNames("miui.systemui.controlcenter.MiuiControlCenter") -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                    logD(TAG, lpparam.packageName, "Plugin for sysui control center loaded.")

                    if (mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                        mPrefsMap.getString("systemui_plugin_card_tiles", "").isNotEmpty() &&
                        mIsDefaultMode
                    ) {
                        CustomCardTiles.initCustomCardTiles(classLoader, mCardStyleTiles)            //A
                    }
                    if (mPrefsMap.getBoolean("system_ui_control_center_hide_edit_botton"))
                        HideEditButton.initHideEditButton(classLoader)
                    if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect") && mIsCompatibilityMode)
                        CCGridForHyperOS.initCCGridForHyperOS(classLoader) // 控制中心磁贴圆角
                    if (mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                        mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color")
                    ) {
                        QSColor.pluginHook(classLoader)
                    }
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                }

                factory.componentNames("miui.systemui.notification.NotificationStatPluginImpl") -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                    logD(TAG, lpparam.packageName, "Plugin for sysui NotificationStatPluginImpl loaded.")

                    if (mPrefsMap.getBoolean("system_ui_statusbar_music_switch"))
                        FocusNotifLyric.initLoader(classLoader);
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                }

                else -> {
                    val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader

                    if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_smart_hub_entry", 0) != 0)
                        HideMiSmartHubEntry.initHideMiSmartHubEntry(classLoader)
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) != 0)
                        HideDeviceControlEntry.initHideDeviceControlEntry(classLoader)
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1) > 1)
                        BluetoothTileStyle.initHideDeviceControlEntry(classLoader)
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3)
                        ShowDeviceName.initShowDeviceName(classLoader)
                    if (mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"))
                        DisableDeviceManaged.initDisableDeviceManaged(classLoader)
                    // logD(TAG, lpparam.packageName, "Plugin is ${factory.mComponentName}")
                    // 仅备份当前可用注入 ClassLoader
                    // miui.systemui.volume.VolumeDialogPlugin
                    // miui.systemui.miplay.MiPlayPluginImpl
                    // miui.systemui.quicksettings.LocalMiuiQSTilePlugin
                    // miui.systemui.controlcenter.MiuiControlCenter
                    // ↓
                    // miui.systemui.notification.NotificationStatPluginImpl
                    // miui.systemui.globalactions.GlobalActionsPlugin
                    // miui.systemui.notification.FocusNotificationPluginImpl
                    // miui.systemui.notification.unimportant.UnimportantSdkPluginImpl
                }
            }
        } catch (_: Exception) {}
    }

    private fun getTileList(): List<String> {
        val cardTiles =
            mPrefsMap.getString("systemui_plugin_card_tiles", "").replace("List_", "")

        return if (TextUtils.isEmpty(cardTiles.replace("List_", ""))) ArrayList()
        else listOf(*cardTiles.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }
}