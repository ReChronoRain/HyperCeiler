package com.sevtinge.cemiuiler.module.packageinstaller;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class MiuiPackageInstallModify extends BaseHook {
    @Override
    public void init() {

        Class<?> mCloudParams = findClassIfExists("com.miui.packageInstaller.model.CloudParams");

        findAndHookConstructor(mCloudParams, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "installNotAllow", false);
                XposedHelpers.setBooleanField(param.thisObject, "showSafeModeTip", false);
                XposedHelpers.setBooleanField(param.thisObject, "showAdsAfter", false);
                XposedHelpers.setBooleanField(param.thisObject, "showAdsBefore", false);
                XposedHelpers.setBooleanField(param.thisObject, "singletonAuthShowAdsAfter", true);
                XposedHelpers.setBooleanField(param.thisObject, "singletonAuthShowAdsBefore", false);
                XposedHelpers.setBooleanField(param.thisObject, "useSystemAppRules", true);
                XposedHelpers.setBooleanField(param.thisObject, "skipInstallConfirm", true);
                XposedHelpers.setBooleanField(param.thisObject, "allowHighLight", true);
                XposedHelpers.setBooleanField(param.thisObject, "openButton", true);
                XposedHelpers.setObjectField(param.thisObject, "safeType", "1");
            }
        });

        // 隐藏开启纯净模式提示
        // SafeModeTipViewObject safeModeTipViewObject = new SafeModeTipViewObject(h10, pureModeElderTipViewObject.f5884m, null, null, 12, null);
        // safeModeTipViewObject.a();  a方法里的调用
        findAndHookMethod("com.miui.packageInstaller.ui.listcomponets.g0", "a", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "l", false);
            }
        });
    }

}
