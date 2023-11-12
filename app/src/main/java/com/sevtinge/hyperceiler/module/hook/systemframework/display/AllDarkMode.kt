package com.sevtinge.hyperceiler.module.hook.systemframework.display

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.IS_INTERNATIONAL_BUILD
import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild

//from SetoHook by SetoSkins
class AllDarkMode : BaseHook() {
    override fun init() {
        if (IS_INTERNATIONAL_BUILD) return
        val clazzForceDarkAppListManager =
            loadClass("com.android.server.ForceDarkAppListManager")
        clazzForceDarkAppListManager.methodFinder().filterByName("getDarkModeAppList").toList()
            .createHooks {
                before {
                    setStaticObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", true)
                }
                after {
                    setStaticObject(
                        clazzMiuiBuild,
                        "IS_INTERNATIONAL_BUILD",
                        IS_INTERNATIONAL_BUILD
                    )
                }
            }
        clazzForceDarkAppListManager.methodFinder().filterByName("shouldShowInSettings").toList()
            .createHooks {
                before { param ->
                    val info = param.args[0] as ApplicationInfo?
                    param.result =
                        !(info == null || (invokeMethodBestMatch(
                            info,
                            "isSystemApp"
                        ) as Boolean) || info.uid < 10000)
                }
            }
    }
}
