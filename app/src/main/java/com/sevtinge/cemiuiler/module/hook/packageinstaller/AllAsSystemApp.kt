package com.sevtinge.cemiuiler.module.hook.packageinstaller

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object AllAsSystemApp : BaseHook() {
    private val systemMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                paramTypes = listOf("android.content.pm.ApplicationInfo")
                returnType = "boolean"
            }
        }.map { it.getMethodInstance(EzXHelper.safeClassLoader) }
    }

    override fun init() {
        /*dexKitBridge.findMethod {
            matcher {
                methodParamTypes = arrayOf("Landroid/content/pm/ApplicationInfo;")
                methodReturnType = "boolean"
            }
        }.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                before { param ->
                    (param.args[0] as ApplicationInfo).flags =
                        (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
                }
            }
        }*/
        systemMethod.createHooks {
            before { param ->
                (param.args[0] as ApplicationInfo).flags =
                    (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
            }
        }
    }
}
