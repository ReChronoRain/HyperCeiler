package com.sevtinge.cemiuiler.module.securitycenter

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

import com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode
import com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionName

object FuckRiskPkg : BaseHook() {
    @SuppressLint("SuspiciousIndentation")
    override fun init() {
        val result = SecurityCenterDexKit.mSecurityCenterResultMap["FuckRiskPkg"]!!
        val appVersionName = getPackageVersionName(lpparam)
        val appVersionCode = getPackageVersionCode(lpparam)
        if (appVersionName.split(".").last() == "1") {
            if (appVersionCode >= 40000774) {
                for (descriptor in result) {
                    try {
                        val mRiskPkg = descriptor.getMethodInstance(lpparam.classLoader)
                        mRiskPkg.hookBefore {
                            it.result = null
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        } else {
            log("Your Security Version is $appVersionName (The version suffix is ${appVersionName.split(".").last()}), and does not meet the usage requirements")
        }
    }
}