package com.sevtinge.cemiuiler.module.wini.hooks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.sevtinge.cemiuiler.module.wini.blur.BlurPersonalAssistant
import com.sevtinge.cemiuiler.module.wini.blur.BlurSecurity
import com.sevtinge.cemiuiler.module.wini.blur.BlurSystemUI
import com.sevtinge.cemiuiler.module.wini.blur.BlurWhenShowShortcutMenu
import com.sevtinge.cemiuiler.module.wini.model.ConfigModel
import com.sevtinge.cemiuiler.utils.HookUtils
import com.sevtinge.cemiuiler.utils.Storage
import com.sevtinge.cemiuiler.BuildConfig

class WiniMainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val otherHooks = OtherHooks(lpparam.classLoader)
        when (lpparam.packageName) {
            // 系统桌面
            "com.miui.home" -> {
                val config = getConfig()
                val miuiHomeHooks = BlurWhenShowShortcutMenu(lpparam.classLoader, config)
                if (config.BlurWhenShowShortcutMenu.enableShortcutBackgroundBlur) {
                    miuiHomeHooks.addBlurEffectToShortcutLayer()
                }

                otherHooks.deviceLevelHook()
                miuiHomeHooks.addBlurEffectToFolderIcon()
                miuiHomeHooks.addBlurEffectToAlphaIcon()
                miuiHomeHooks.hideBlurIconWhenEnterRecents()
            }
            // 系统界面
            "com.android.systemui" -> {
                val config = getConfig()
                val systemUIHooks = BlurSystemUI(lpparam.classLoader, config)
                if (config.BlurSystemUI.notification.enable) {
                    systemUIHooks.addBlurEffectToNotificationView()
                }
                if (config.BlurSystemUI.quickSetting.hideMiPlayEntry) {
                    systemUIHooks.hideControlsPlugin()
                }
                if (config.BlurSystemUI.quickSetting.controlDetailBackgroundAlpha != 255) {
                    systemUIHooks.setQSControlDetailBackgroundAlpha()
                }
                systemUIHooks.enableBlurForMTK()
                systemUIHooks.addBlurEffectToLockScreen()
            }
            // 个人助理 负一屏
            "com.miui.personalassistant" -> {
                val config = getConfig()
                if (config.BlurPersonalAssistant.background.enable) {
                    val personalAssistantHooks = BlurPersonalAssistant(lpparam.classLoader, config)
                    personalAssistantHooks.addBlurEffectToPersonalAssistant()
                }
            }
            // 安全中心
            "com.miui.securitycenter" -> {
                val config = getConfig()
                if (config.BlurSecurity.dockBackground.enable) {
                    val securityCenterHooks = BlurSecurity(lpparam.classLoader, config)
                    securityCenterHooks.addBlurEffectToDock()
                }
            }
            BuildConfig.APPLICATION_ID -> {
                getConfig(true)
                otherHooks.enableModule()
            }
            else -> {
                return
            }
        }
    }
    private fun getConfig(showLog: Boolean = false): ConfigModel {
        val xSharedPreferences =
            XSharedPreferences(BuildConfig.APPLICATION_ID, Storage.DATA_FILENAME)
        xSharedPreferences.makeWorldReadable()
        val configJsonString = xSharedPreferences.getString(Storage.CONFIG_JSON, "")
        if (configJsonString != null && configJsonString != "") {
            if (showLog) {
                HookUtils.log(configJsonString)
            }
            return Storage.getConfig(configJsonString)
        }
        return ConfigModel()
    }
}