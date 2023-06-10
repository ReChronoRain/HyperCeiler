package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.findClassOrNull
import com.sevtinge.cemiuiler.utils.setBooleanField

object DisableSafeModelTip : BaseHook() {
    override fun init() {
        val miuiSettingsCompatClass =
            loadClass("com.android.packageinstaller.compat.MiuiSettingsCompat")

        try {
            miuiSettingsCompatClass.methodFinder().filterByName("isPersonalizedAdEnabled")
                .filterByReturnType(Boolean::class.java).toList().createHooks {
                before {
                    it.result = false
                }
            }
        } catch (t: Throwable) {
            logE(t)
        }

        var letter = 'a'
        for (i in 0..25) {
            try {
                val classIfExists =
                    "com.miui.packageInstaller.ui.listcomponets.${letter}0".findClassOrNull()
                classIfExists?.let {
                    it.methodFinder().filterByName("a").first().createHook {
                        after { hookParam ->
                            hookParam.thisObject.setBooleanField("l", false)
                        }
                    }
                }
            } catch (t: Throwable) {
                letter++
            }
        }
    }
}
