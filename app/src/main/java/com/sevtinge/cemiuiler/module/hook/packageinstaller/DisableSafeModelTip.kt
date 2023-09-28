package com.sevtinge.cemiuiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.packageinstaller.PackageInstallerDexKit.mPackageInstallerResultMethodsMap
import com.sevtinge.cemiuiler.utils.findClassOrNull
import com.sevtinge.cemiuiler.utils.setBooleanField
import java.util.Objects

object DisableSafeModelTip : BaseHook() {
    override fun init() {
        val result = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["DisableSafeModelTip"]
        )
        for (descriptor in result) {
            val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
            mDisableSafeModelTip.createHook {
                returnConstant(false)
            }
            // val miuiSettingsCompatClass = loadClass("com.android.packageinstaller.compat.MiuiSettingsCompat")
            /*miuiSettingsCompatClass.methodFinder().filterByName("isPersonalizedAdEnabled")
                .filterByReturnType(Boolean::class.java).toList().createHooks {
                    before {
                        it.result = false
                    }
                }*/
        }

        // 屏蔽每 30 天提示开启安全守护的弹窗（已知问题：完成和打开按钮无反应）
        /*val result2 = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["Disable30DaysDialog"]
        )

        for (descriptor in result2) {
            val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
            mDisableSafeModelTip.createHook {
                returnConstant(null)
            }
        }*/

        var letter = 'a'
        for (i in 0..25) {
            try {
                val classIfExists =
                    "com.miui.packageInstaller.ui.listcomponets.${letter}0".findClassOrNull()
                classIfExists?.let {
                    it.methodFinder().filterByName("a").first().createHook {
                        after { hookParam ->
                            try {
                                hookParam.thisObject.setBooleanField("m", false)
                            } catch (t: Throwable) {
                                hookParam.thisObject.setBooleanField("l", false)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                letter++
            }
        }
    }
}
