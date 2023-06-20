package com.sevtinge.cemiuiler.module.securitycenter.other

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit
import com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode

object FuckRiskPkg : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        val result = SecurityCenterDexKit.mSecurityCenterResultMap["FuckRiskPkg"]!!
        val appVersionCode = getPackageVersionCode(lpparam)
        if (appVersionCode >= 40000774) {
            for (descriptor in result) {
                try {
                    val mRiskPkg = descriptor.getMethodInstance(lpparam.classLoader)
                    mRiskPkg.createHook {
                        before {
                            it.result = null
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }
}
