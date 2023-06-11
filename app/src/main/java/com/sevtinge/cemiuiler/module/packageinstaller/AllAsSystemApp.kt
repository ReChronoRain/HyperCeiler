package com.sevtinge.cemiuiler.module.packageinstaller

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.yife.DexKit
import com.sevtinge.cemiuiler.utils.yife.DexKit.dexKitBridge

class AllAsSystemApp : BaseHook() {
    override fun init() {
        DexKit.hostDir = lpparam.appInfo.sourceDir
        DexKit.loadDexKit()
        dexKitBridge.findMethod {
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
        DexKit.closeDexKit()
    }
}
