package com.sevtinge.cemiuiler.module.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit
import com.sevtinge.cemiuiler.utils.Helpers
import com.sevtinge.cemiuiler.utils.api.isPad
import java.util.Objects

object BeautyPrivacy : BaseHook() {
    override fun init() {
        try {
            val appVersionCode = Helpers.getPackageVersionCode(lpparam)
            val result =
                Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultClassMap["BeautyLight"])
            for (descriptor in result) {
                val beautyPrivacyUtils = descriptor.getClassInstance(lpparam.classLoader)
                beautyPrivacyUtils.methodFinder().first {
                    if (!isPad()) {
                        when {
                            appVersionCode in 40000749..40000750 -> name == "X"
                            appVersionCode in 40000754..40000756 -> name == "Q"
                            appVersionCode in 40000761..40000762 -> name == "R"
                            appVersionCode in 40000771..40000772 -> name == "X"
                            appVersionCode >= 40000774 -> name == "Y"// 手机端截止到 7.8.5-20230515 版本
                            else -> name == "M" // 未混淆分类
                        }
                    } else {
                        when {
                            appVersionCode == 40010749 -> name == "f"
                            appVersionCode == 40010750 -> name == "l"
                            appVersionCode >= 40010771 -> name == "f"// 平板端截止到 7.8.5-20230511 版本
                            else -> name == "l" // 未混淆分类
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
