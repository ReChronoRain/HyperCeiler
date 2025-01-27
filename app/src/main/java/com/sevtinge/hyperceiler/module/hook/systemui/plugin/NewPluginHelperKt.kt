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
package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.content.ContextWrapper
import android.text.TextUtils
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CCGridForHyperOSKt
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CustomCardTiles
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSColor
import com.sevtinge.hyperceiler.module.hook.systemui.other.DefaultPluginTheme
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v.FocusNotifLyric
import com.sevtinge.hyperceiler.utils.api.PluginFactory
import com.sevtinge.hyperceiler.utils.log.LogManager.logLevel
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

                val loaders = listOf(
                    Triple(
                        "VolumeTimerValuesHook",
                        mPrefsMap.getBoolean("system_ui_volume_timer"),
                        VolumeTimerValuesHook::initVolumeTimerValuesHook
                    ),
                    Triple(
                        "NewShowVolumePct",
                        (isStyle == 2) && mPrefsMap.getBoolean("system_cc_volume_showpct_title"),
                        NewShowVolumePct::initLoader
                    ),
                    Triple(
                        "EnableVolumeBlur",
                        mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"),
                        EnableVolumeBlur::initEnableVolumeBlur
                    ),
                    Triple(
                        "StartCollpasedColumnPress",
                        mPrefsMap.getBoolean("system_ui_volume_collpased_column_press"),
                        StartCollpasedColumnPress::initLoaderHook
                    ),
                    Triple(
                        "StartCollpasedFootButton",
                        mPrefsMap.getBoolean("system_ui_volume_hide_foot_button"),
                        HideCollpasedFootButton::initLoaderHook
                    ),
                    Triple(
                        "DefaultPluginTheme",
                        mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"),
                        DefaultPluginTheme::initDefaultPluginTheme
                    ),
                )
                loadClassLoaders(factory.mComponentName.toString(), classLoader, loaders)
            }

            factory.componentNames("miui.systemui.quicksettings.LocalMiuiQSTilePlugin"),
            factory.componentNames("miui.systemui.controlcenter.MiuiControlCenter") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui qs tiles && control center loaded.")

                val loaders = listOf(
                    Triple(
                        "VolumeOrQSBrightnessValue",
                        (isStyle == 1) && (mPrefsMap.getBoolean("system_ui_control_center_qs_brightness_top_value_show") || mPrefsMap.getBoolean("system_ui_control_center_qs_volume_top_value_show")),
                        VolumeOrQSBrightnessValue::initVolumeOrQSBrightnessValue
                    ),
                    Triple(
                        "CustomCardTiles",
                        mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                                mPrefsMap.getString("systemui_plugin_card_tiles", "").isNotEmpty()
                    ) { cl -> CustomCardTiles.initCustomCardTiles(cl, mCardStyleTiles) },
                    Triple(
                        "HideEditButton",
                        mPrefsMap.getBoolean("system_ui_control_center_hide_edit_botton"),
                        HideEditButton::initHideEditButton
                    ),
                    Triple(
                        "CCGridForHyperOS",
                        mPrefsMap.getBoolean("system_ui_control_center_rounded_rect"),
                        CCGridForHyperOSKt::initCCGridForHyperOS
                    ),
                    Triple(
                        "QSColor",
                        mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                                mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"),
                        QSColor::pluginHook
                    ),
                    Triple(
                        "NewBrightnessPct",
                        (isStyle == 2) && mPrefsMap.getBoolean("system_showpct_title"),
                        NewBrightnessPct::initLoaderHook
                    ),
                    Triple(
                        "DisableDeviceManaged",
                        mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"),
                        DisableDeviceManagedNew::initDisableDeviceManaged
                    ),
                    Triple(
                        "DefaultPluginTheme",
                        mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"),
                        DefaultPluginTheme::initDefaultPluginTheme
                    ),
                )
                loadClassLoaders(factory.mComponentName.toString(), classLoader, loaders)
            }

            factory.componentNames("miui.systemui.notification.NotificationStatPluginImpl") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui NotificationStatPluginImpl loaded.")

                val loaders = listOf(
                    Triple(
                        "FocusNotifLyric",
                        mPrefsMap.getBoolean("system_ui_statusbar_music_switch"),
                        FocusNotifLyric::initLoader
                    ),
                    Triple(
                        "DefaultPluginTheme",
                        mPrefsMap.getBoolean("system_ui_other_default_plugin_theme"),
                        DefaultPluginTheme::initDefaultPluginTheme
                    ),
                )
                loadClassLoaders(factory.mComponentName.toString(), classLoader, loaders)
            }

            else -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                val loaders = listOf(
                    Triple(
                        "ShowDeviceName",
                        mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3,
                        ShowDeviceName::initShowDeviceName
                    ),
                )
                loadClassLoaders(factory.mComponentName.toString(), classLoader, loaders)

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

    private fun loadClassLoaders(
        name: String,
        classLoader: ClassLoader,
        loaders: List<Triple<String, Boolean, (ClassLoader) -> Unit>>
    ) {
        loaders.forEach { (tag, prefKey, loader) ->
            runCatching {
                if (prefKey) {
                    loader(classLoader)
                }
                if (logLevel >= 3) logI(TAG, lpparam.packageName, "$name is loaded success.")
            }.onFailure {
                if (logLevel >= 1) logE(TAG, lpparam.packageName, "[$tag] $name is fail loaded, log: $it")
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
