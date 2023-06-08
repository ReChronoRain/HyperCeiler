package com.sevtinge.cemiuiler.module.securitycenter.app;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class AppRestrict extends BaseHook {

    Class<?> mAppManageUtils;

    @Override
    public void init() {
        mAppManageUtils = findClassIfExists("com.miui.appmanager.AppManageUtils");

        Method[] mGetAppInfo = XposedHelpers.findMethodsByExactParameters(mAppManageUtils, ApplicationInfo.class, Object.class, PackageManager.class, String.class, int.class, int.class);

        if (mGetAppInfo.length == 0) {
            LogUtils.logXp(TAG, "Cannot find getAppInfo method!");
        } else {
            hookMethod(mGetAppInfo[0], new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    if ((int) param.args[3] == 128 && (int) param.args[4] == 0) {
                        ApplicationInfo appInfo = (ApplicationInfo) param.getResult();
                        appInfo.flags &= ~ApplicationInfo.FLAG_SYSTEM;
                        param.setResult(appInfo);
                    }
                }
            });
        }

        findAndHookMethod("com.miui.networkassistant.ui.fragment.ShowAppDetailFragment", "initFirewallData", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object mAppInfo = XposedHelpers.getObjectField(param.thisObject, "mAppInfo");
                if (mAppInfo != null) XposedHelpers.setBooleanField(mAppInfo, "isSystemApp", false);
            }
        });

        hookAllMethods("com.miui.networkassistant.service.FirewallService", "setSystemAppWifiRuleAllow", XC_MethodReplacement.DO_NOTHING);
    }
}
