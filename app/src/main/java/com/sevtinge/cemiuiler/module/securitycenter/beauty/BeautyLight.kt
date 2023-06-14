package com.sevtinge.cemiuiler.module.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.api.isPad
import java.util.Objects


object BeautyLight : BaseHook() {
    override fun init() {
        try {
            val appVersionCode = Helpers.getPackageVersionCode(lpparam)
            val result =
                Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultClassMap["BeautyLight"])
            for (descriptor in result) {
                val beautyUtils = descriptor.getClassInstance(lpparam.classLoader)
                beautyUtils.methodFinder().first {
                    if (!isPad()) {
                        when {
                            appVersionCode in 40000749..40000750 -> name == "G"
                            appVersionCode in 40000754..40000756 -> name == "j"
                            appVersionCode in 40000761..40000762 -> name == "k"
                            appVersionCode in 40000771..40000772 -> name == "G"
                            appVersionCode >= 40000774 -> name == "H"// 手机端截止到 7.8.5-20230515 版本
                            else -> name == "c" // 未混淆分类
                        }
                    } else {
                        when {
                            appVersionCode == 40010749 -> name == "G"
                            appVersionCode == 40010750 -> name == "j"
                            appVersionCode in 40010771..40010772 -> name == "G"
                            appVersionCode >= 40010774 -> name == "H"// 平板端截止到 7.8.5-20230511 版本
                            else -> name == "j" // 未混淆分类
                        }
                    }
                }.createHook {
                    returnConstant(true)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
