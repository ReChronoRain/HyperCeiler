/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AppRestrict extends BaseHook {

    Class<?> mAppManageUtils;

    @Override
    public void init() {
        mAppManageUtils = findClassIfExists("com.miui.appmanager.AppManageUtils");

        Method[] mGetAppInfo = EzxHelpUtils.findMethodsByExactParameters(mAppManageUtils, ApplicationInfo.class, Object.class, PackageManager.class, String.class, int.class, int.class);

        if (mGetAppInfo.length == 0) {
            XposedLog.e(TAG, getPackageName(), "Cannot find getAppInfo method!");
        } else {
            hookMethod(mGetAppInfo[0], new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if ((int) param.getArgs()[3] == 128 && (int) param.getArgs()[4] == 0) {
                        ApplicationInfo appInfo = (ApplicationInfo) param.getResult();
                        appInfo.flags &= ~ApplicationInfo.FLAG_SYSTEM;
                        param.setResult(appInfo);
                    }
                }
            });
        }

        findAndHookMethod("com.miui.networkassistant.ui.fragment.ShowAppDetailFragment", "initFirewallData", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Object mAppInfo = EzxHelpUtils.getObjectField(param.getThisObject(), "mAppInfo");
                if (mAppInfo != null) EzxHelpUtils.setBooleanField(mAppInfo, "isSystemApp", false);
            }
        });

        hookAllMethods("com.miui.networkassistant.service.FirewallService", "setSystemAppWifiRuleAllow", doNothing());
    }
}
