package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.packageinstaller.PackageInstallerDexKit.mPackageInstallerResultMethodsMap
import java.util.Objects

object DisableSafeModelTip : BaseHook() {
    override fun init() {
        runCatching {
            val result = Objects.requireNonNull(
                mPackageInstallerResultMethodsMap!!["SecureVerifyEnable"]
            )
            for (descriptor in result) {
                val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
                mDisableSafeModelTip.createHook {
                    returnConstant(​false​)
                }
            }
        }
        
        runCatching {
            val result2 = Objects.requireNonNull(
                mPackageInstallerResultMethodsMap!!["isInstallRiskEnabled"]
            )
            for (descriptor in result2) {
                val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
                mDisableSafeModelTip.createHook {
                    returnConstant(​false​)
                }
            }
        }
    
        /*runCatching {
            val result3 = Objects.requireNonNull(
                mPackageInstallerResultMethodsMap!!["DisableSafeModelTip"]
            )
            for (descriptor in result3) {
                val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
                mDisableSafeModelTip.createHook {
                    returnConstant(​false​)
                }
            }
        }*/
    }
}