package com.sevtinge.cemiuiler.module.packageinstaller;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class DisableCountChecking extends BaseHook {
    /*override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        hasEnable("pkg_installer_count_checking") {
            findMethod("com.miui.packageInstaller.model.RiskControlRules") {
                name == "getCurrentLevel"
            }.hookBefore { param ->
                    XposedBridge.log("Hooked getCurrentLevel, param result = ${param.result}")
                param.result = 0
            }
        }
    }*/
    Class<?> mRiskCtrlRules;

    @Override
    public void init() {

        hookAllMethods("com.xiaomi.accountsdk.utils.ParcelableAttackGuardian", "safeCheck", XC_MethodReplacement.returnConstant(true));

        mRiskCtrlRules = findClassIfExists("com.miui.packageInstaller.model.RiskControlRules");
        returnIntConstant(mRiskCtrlRules, "getCurrentLevel");

    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        findAndHookMethod(cls, methodName, XC_MethodReplacement.returnConstant(0));
    }
}




