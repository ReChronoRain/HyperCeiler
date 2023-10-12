package com.sevtinge.cemiuiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object InstallRiskDisable : BaseHook() {
    override fun init() {
        /*val result = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["SecureVerifyEnable"]
        )
        for (descriptor in result) {
            val mSecureVerifyEnable = descriptor.getMethodInstance(lpparam.classLoader)
            mSecureVerifyEnable.createHook {
                returnConstant(false)
            }
        }

        val result2 = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["isInstallRiskEnabled"]
        )
        for (descriptor2 in result2) {
            val isInstallRiskEnabled = descriptor2.getMethodInstance(lpparam.classLoader)
            isInstallRiskEnabled.createHook {
                returnConstant(false)
            }
        }

        val result3 = Objects.requireNonNull(
            mPackageInstallerResultMethodsMap!!["DisableSafeModelTip"]
        )
          for (descriptor in result3) {
            val mDisableSafeModelTip = descriptor.getMethodInstance(lpparam.classLoader)
            mDisableSafeModelTip.createHook {
                 returnConstant(false)
            }
        }*/

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("secure_verify_enable")
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("installerOpenSafetyModel")
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.provider.MiuiSettings\$Ad")
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }
    }
}
