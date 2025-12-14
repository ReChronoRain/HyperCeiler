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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin

import android.content.ContextWrapper
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.rules.systemui.AutoSEffSwitchForSystemUi
import com.sevtinge.hyperceiler.hook.module.rules.systemui.AutoSEffSwitchForSystemUi.isSupportFW
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.DefaultPluginTheme
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.aod.AodBlurButton
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.CCGridForHyperOSKt
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.CustomCardTiles
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.DisableDeviceManagedNew
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.EnableVolumeBlur
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.HideCollpasedFootButton
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.HideEditButton
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.NewBrightnessPct
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.NewShowVolumePct
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.QSColor
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.QsTileSuperBlur
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.ShowDeviceName
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.StartCollpasedColumnPress
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.UnlockCarSicknessTile
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.VolumeOrQSBrightnessValue
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.v.FocusNotifLyric
import com.sevtinge.hyperceiler.hook.utils.api.PluginFactory
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreSmallVersion
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import java.lang.ref.WeakReference

object NewPluginHelperKt : BaseHook() {
    private val isStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_others_pct_style", 0)
    }

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
        loadClass($$"com.android.systemui.shared.plugins.PluginInstance$PluginFactory")
            .methodFinder().filterByName("createPluginContext")
            .first().createAfterHook { it ->
                runCatching {
                    val wrapper = it.result as ContextWrapper
                    onPluginLoaded(PluginFactory(it.thisObject).also { isLoad ->
                        isLoad.pluginCtxRef = WeakReference(wrapper)
                    })
                }.onFailure {
                    logE(TAG, lpparam.packageName, "Failed to create plugin context.")
                    return@createAfterHook
                }
            }
    }

    private fun onPluginLoaded(factory: PluginFactory) {
        val mCardStyleTiles = getTileList()
        val prefs = mPrefsMap

        val componentName = factory.mComponentName
        val pluginCtx = factory.pluginCtxRef.get()
        if (pluginCtx == null) {
            logE(TAG, lpparam.packageName, "plugin context is null for $componentName")
            return
        }
        val classLoader: ClassLoader = pluginCtx.classLoader

        when (componentName) {
            factory.componentNames(1, "miui.systemui.volume.VolumeDialogPlugin") -> {
                logD(TAG, lpparam.packageName, "Plugin for sysui volume loaded.")

                val enabledLoaders = ArrayList<Pair<String, (ClassLoader) -> Unit>>(6)

                if ((isHyperOSVersion(1f) || isStyle == 2) && prefs.getBoolean("system_cc_volume_showpct_title")) {
                    enabledLoaders.add(
                        Pair(
                            "NewShowVolumePct",
                            NewShowVolumePct::initLoader
                        )
                    )
                }

                if (prefs.getBoolean("system_ui_plugin_enable_volume_blur")) {
                    enabledLoaders.add(
                        Pair(
                            "EnableVolumeBlur",
                            EnableVolumeBlur::initEnableVolumeBlur
                        )
                    )
                }

                if (prefs.getBoolean("system_ui_volume_collpased_column_press")) {
                    enabledLoaders.add(
                        Pair(
                            "StartCollpasedColumnPress",
                            StartCollpasedColumnPress::initLoaderHook
                        )
                    )
                }

                if (prefs.getBoolean("system_ui_volume_hide_foot_button")) {
                    enabledLoaders.add(
                        Pair(
                            "StartCollpasedFootButton",
                            HideCollpasedFootButton::initLoaderHook
                        )
                    )
                }

                if (prefs.getBoolean("system_ui_other_default_plugin_theme")) {
                    enabledLoaders.add(
                        Pair(
                            "DefaultPluginTheme",
                            DefaultPluginTheme::initDefaultPluginTheme
                        )
                    )
                }

                loadClassLoaders(componentName.toString(), classLoader, enabledLoaders)
            }

            factory.componentNames(1, "miui.systemui.quicksettings.LocalMiuiQSTilePlugin"),
            factory.componentNames(1, "miui.systemui.controlcenter.MiuiControlCenter") -> {
                logD(TAG, lpparam.packageName, "Plugin for sysui qs tiles && control center loaded.")

                val enabledLoaders = ArrayList<Pair<String, (ClassLoader) -> Unit>>(10)

                if ((isStyle == 1) && (prefs.getBoolean("system_ui_control_center_qs_brightness_top_value_show") || prefs.getBoolean("system_ui_control_center_qs_volume_top_value_show"))) {
                    enabledLoaders.add(
                        Pair(
                            "VolumeOrQSBrightnessValue",
                            VolumeOrQSBrightnessValue::initVolumeOrQSBrightnessValue
                        )
                    )
                }

                if (prefs.getBoolean("misound_bluetooth") && !isSupportFW() && isHyperOSVersion(2f)) {
                    enabledLoaders.add(
                        Pair("AutoSEffSwitchForSystemUi",
                            AutoSEffSwitchForSystemUi::onNotSupportFW
                        )
                    )
                }

                if (prefs.getBoolean("systemui_plugin_card_tiles_enabled")) {
                    val tileStr = prefs.getString("systemui_plugin_card_tiles", "")
                    if (!tileStr.isNullOrEmpty()) {
                        enabledLoaders.add(
                            Pair("CustomCardTiles") { cl ->
                                CustomCardTiles.initCustomCardTiles(cl, mCardStyleTiles)
                            }
                        )
                    }
                }

                if (prefs.getBoolean("system_ui_control_center_hide_edit_botton")) {
                    enabledLoaders.add(Pair("HideEditButton", HideEditButton::initHideEditButton))
                }

                if (prefs.getBoolean("system_ui_control_center_tile_super_blur")) {
                    enabledLoaders.add(Pair("QsTileSuperBlur", QsTileSuperBlur::initQsTileSuperBlur))
                }

                if (prefs.getBoolean("system_ui_control_center_rounded_rect")) {
                    enabledLoaders.add(Pair("CCGridForHyperOS", CCGridForHyperOSKt::initCCGridForHyperOS))
                }

                if (prefs.getBoolean("system_ui_control_center_qs_open_color") || prefs.getBoolean("system_ui_control_center_qs_big_open_color")) {
                    enabledLoaders.add(Pair("QSColor", QSColor::pluginHook))
                }

                if ((isHyperOSVersion(1f) || isStyle == 2) && prefs.getBoolean("system_showpct_title")) {
                    enabledLoaders.add(Pair("NewBrightnessPct", NewBrightnessPct::initLoaderHook))
                }

                if (prefs.getBoolean("system_ui_control_center_disable_device_managed")) {
                    enabledLoaders.add(Pair("DisableDeviceManaged", DisableDeviceManagedNew::initDisableDeviceManaged))
                }

                if (prefs.getBoolean("security_center_unlock_car_sickness")) {
                    enabledLoaders.add(Pair("UnlockCarSicknessTile") { cl -> UnlockCarSicknessTile.initUnlockCarSicknessTile(cl) })
                }

                loadClassLoaders(componentName.toString(), classLoader, enabledLoaders)
            }

            factory.componentNames(1, "miui.systemui.notification.NotificationStatPluginImpl"),
            factory.componentNames(1, "miui.systemui.notification.FocusNotificationPluginImpl") -> {
                logD(TAG, lpparam.packageName, "Plugin for sysui NotificationStatPluginImpl loaded.")

                val enabledLoaders = ArrayList<Pair<String, (ClassLoader) -> Unit>>(1)
                if (prefs.getBoolean("system_ui_statusbar_music_switch") || prefs.getBoolean("system_ui_unlock_all_focus")) {
                    enabledLoaders.add(Pair("FocusNotifLyric", FocusNotifLyric::initLoader))
                }
                loadClassLoaders(componentName.toString(), classLoader, enabledLoaders)
            }

            factory.componentNames(0, "com.miui.keyguard.shortcuts.ShortcutPluginImpl") -> {
                logD(TAG, lpparam.packageName, "Plugin for aod ShortcutPluginImpl loaded.")

                val enabledLoaders = ArrayList<Pair<String, (ClassLoader) -> Unit>>(1)
                if (prefs.getBoolean("system_ui_lock_screen_blur_button") && isMoreSmallVersion(200, 2f)) {
                    enabledLoaders.add(Pair("AodBlurButton", AodBlurButton::initLoader))
                }
                loadClassLoaders(componentName.toString(), classLoader, enabledLoaders)
            }

            else -> {
                val enabledLoaders = ArrayList<Pair<String, (ClassLoader) -> Unit>>(1)
                if (prefs.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3 && isHyperOSVersion(1f)) {
                    enabledLoaders.add(Pair("ShowDeviceName", ShowDeviceName::initShowDeviceName))
                }
                loadClassLoaders(componentName.toString(), classLoader, enabledLoaders)

                // logD(TAG, lpparam.packageName, "Plugin is ${factory.mComponentName}")
                // 仅备份当前可用注入 ClassLoader
                // Plugin
                // miui.systemui.volume.VolumeDialogPlugin
                // miui.systemui.miplay.MiPlayPluginImpl
                // miui.systemui.quicksettings.LocalMiuiQSTilePlugin
                // miui.systemui.controlcenter.MiuiControlCenter
                // ↓
                // miui.systemui.notification.NotificationStatPluginImpl
                // miui.systemui.globalactions.GlobalActionsPlugin
                // miui.systemui.notification.FocusNotificationPluginImpl
                // miui.systemui.notification.unimportant.UnimportantSdkPluginImpl

                // Aod
                // com.miui.keyguard.shortcuts.ShortcutPluginImpl
                // com.miui.aod.doze.DozeServicePluginImpl
            }
        }
    }

    private fun loadClassLoaders(
        tag: String,
        classLoader: ClassLoader,
        loaders: List<Pair<String, (ClassLoader) -> Unit>>
    ) {
        for ((name, loader) in loaders) {
            runCatching {
                loader(classLoader)
                logD(TAG, lpparam.packageName, "$name is loaded success.")
            }.onFailure {
                logE(TAG, lpparam.packageName, "[$tag] $name is fail loaded, log: ${it.stackTraceToString()}" )
            }
        }
    }

    private fun getTileList(): List<String> {
        val raw = mPrefsMap.getString("systemui_plugin_card_tiles", "")?.removePrefix("List_") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split('|').filter { it.isNotEmpty() }
    }
}
