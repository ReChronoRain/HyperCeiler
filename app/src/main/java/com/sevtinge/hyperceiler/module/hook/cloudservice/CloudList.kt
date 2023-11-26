package com.sevtinge.hyperceiler.module.hook.cloudservice

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

// <string name="cloud_list">应用数据云同步补全</string>
// <string name="cloud_list_summary">手机可以同步小米创作\n国内国际版小米浏览器可以同时同步</string>
// <string name="cloud_creation_summary">小米创作的首次同步必须从小米创作的设置中启用授权\n否则会提示网络不可用或系统忙请稍后再试</string>
object CloudList : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("support_google_csp_sync")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader).createHook {
            returnConstant(null)
        }
    }
}
