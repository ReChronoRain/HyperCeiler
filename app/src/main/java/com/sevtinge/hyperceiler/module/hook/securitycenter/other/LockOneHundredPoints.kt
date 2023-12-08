package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object LockOneHundredPoints : BaseHook() {
    private val score by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingString("getMinusPredictScore", StringMatchType.Contains)
            }
        }.single().getMethodInstance(safeClassLoader)
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").methodFinder()
            .filterByName("onClick")
            .filterByParamTypes(View::class.java)
            .first().createHook {
                before {
                    it.result = null
                }
            }

        score.createHook {
            returnConstant(0)
        }
    }
}
