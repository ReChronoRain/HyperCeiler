package com.sevtinge.cemiuiler.module.packageinstaller

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.safeDexKitBridge

class AllAsSystemApp : BaseHook() {
    override fun init() {
        initDexKit(lpparam)
        safeDexKitBridge.findMethod {
            methodParamTypes = arrayOf("Landroid/content/pm/ApplicationInfo;")
            methodReturnType = "boolean"
        }.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                before { param ->
                    (param.args[0] as ApplicationInfo).flags =
                        (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
                }
            }
        }
        closeDexKit()
    }
}
