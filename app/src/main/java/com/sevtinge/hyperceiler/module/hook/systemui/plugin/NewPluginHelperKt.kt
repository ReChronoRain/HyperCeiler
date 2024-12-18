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
import com.sevtinge.hyperceiler.utils.api.PluginFactory
import com.sevtinge.hyperceiler.utils.log.*
import java.lang.ref.*

object NewPluginHelperKt : BaseHook() {
    override fun init() {
        // from hyperstar2.0
        /*loadClass("com.android.systemui.shared.plugins.PluginActionManager\$PluginContextWrapper")
            .constructors.first().createAfterHook {
                val classLoader = it.thisObject.getObjectFieldAs<ClassLoader>("mClassLoader")
                runCatching {
                    onPluginLoadedAll(classLoader)
                }.onFailure {
                    logE(TAG, lpparam.packageName, "Failed to create plugin context.")
                    return@createAfterHook
                }
            }*/

        // https://github.com/buffcow/Hyper5GSwitch/blob/master/app/src/main/kotlin/cn/buffcow/hyper5g/hooker/PluginLoader.kt
        loadClass("com.android.systemui.shared.plugins.PluginInstance\$PluginFactory")
            .methodFinder().filterByName("createPluginContext")
            .first().createAfterHook {
                runCatching {
                    val wrapper = it.result as ContextWrapper
                    onPluginLoaded(PluginFactory(it.thisObject).also {
                        it.pluginCtxRef = WeakReference(wrapper)
                    })
                }.onFailure {
                    logE(TAG, lpparam.packageName, "Failed to create plugin context.")
                    return@createAfterHook
                }
            }
    }

    private fun onPluginLoaded(factory: PluginFactory) {
        val mCardStyleTiles = getTileList()

        when (factory.mComponentName) {
            factory.componentNames("miui.systemui.volume.VolumeDialogPlugin") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui volume loaded.")

                try {
                    if (mPrefsMap.getBoolean("miui.systemui.plugin_enable_volume_blur"))
                        EnableVolumeBlur.initEnableVolumeBlur(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("EnableVolumeBlur", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_cc_volume_showpct_title"))
                        NewShowVolumePct.initLoader(classLoader) // 声音百分比
                } catch (e: Throwable) {
                    XposedLogUtils.logE("NewShowVolumePct", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_unlock_super_volume"))
                        NewSuperVolume.initSuperVolume(classLoader) // 超大音量
                } catch (e: Throwable) {
                    XposedLogUtils.logE("NewSuperVolume", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_framework_volume_separate_control") &&
                        mPrefsMap.getBoolean("system_framework_volume_separate_slider")
                    )
                        NotificationVolumeSeparateSlider.initHideDeviceControlEntry(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("NotificationVolumeSeparateSlider", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("DefaultPluginTheme", "miui.systemui.plugin", "Hook Failed: $e")
                }

            }

            factory.componentNames("miui.systemui.quicksettings.LocalMiuiQSTilePlugin"),
            factory.componentNames("miui.systemui.controlcenter.MiuiControlCenter") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui qs tiles && control center loaded.")

                try {
                    if (mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                        mPrefsMap.getString("systemui_plugin_card_tiles", "").isNotEmpty()
                    ) {
                        CustomCardTiles.initCustomCardTiles(classLoader, mCardStyleTiles)
                    }
                } catch (e: Throwable) {
                    XposedLogUtils.logE("CustomCardTiles", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_control_center_hide_edit_botton"))
                        HideEditButton.initHideEditButton(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("HideEditButton", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect"))
                        CCGridForHyperOS.initCCGridForHyperOS(classLoader) // 控制中心磁贴圆角
                } catch (e: Throwable) {
                    XposedLogUtils.logE("CCGridForHyperOS", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                        mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color")
                    ) {
                        QSColor.pluginHook(classLoader)
                    }
                } catch (e: Throwable) {
                    XposedLogUtils.logE("QSColor", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_showpct_title"))
                        NewBrightnessPct.initLoaderHook(classLoader) // 亮度百分比
                } catch (e: Throwable) {
                    XposedLogUtils.logE("NewBrightnessPct", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("DefaultPluginTheme", "miui.systemui.plugin", "Hook Failed: $e")
                }

            }

            factory.componentNames("miui.systemui.notification.NotificationStatPluginImpl") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui NotificationStatPluginImpl loaded.")

                try {
                    if (mPrefsMap.getBoolean("system_ui_statusbar_music_switch"))
                        FocusNotifLyric.initLoader(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("FocusNotifLyric", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"))
                        DefaultPluginTheme.initDefaultPluginTheme(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("DefaultPluginTheme", "miui.systemui.plugin", "Hook Failed: $e")
                }

            }

            else -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                try {
                    if (mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3)
                        ShowDeviceName.initShowDeviceName(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("ShowDeviceName", "miui.systemui.plugin", "Hook Failed: $e")
                }

                try {
                    if (mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"))
                        DisableDeviceManaged.initDisableDeviceManaged(classLoader)
                } catch (e: Throwable) {
                    XposedLogUtils.logE("DisableDeviceManaged", "miui.systemui.plugin", "Hook Failed: $e")
                }


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
    }

    private fun getTileList(): List<String> {
        val cardTiles =
            mPrefsMap.getString("systemui_plugin_card_tiles", "").replace("List_", "")

        return if (TextUtils.isEmpty(cardTiles.replace("List_", ""))) ArrayList()
        else listOf(*cardTiles.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }
}