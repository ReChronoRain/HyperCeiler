/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.systemframework.input;

import android.content.IntentFilter;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * Source:
 * <a href="https://github.com/RC1844/MIUI_IME_Unlock/blob/main/app/src/main/java/com/xposed/miuiime/MainHook.kt">RC1844/MIUI_IME_Unlock</a>
 */
public class MiuiImeUnlockSystem extends BaseHook {

    @Override
    public void init() {
        startPermissionHook();
    }

    /**
     * Hook 获取应用列表权限，为所有输入法强制提供获取输入法列表的权限。
     * 用于修复部分输入法（搜狗输入法小米版等）缺少获取输入法列表权限，导致切换输入法功能不能显示其他输入法的问题。
     * 理论等效于在输入法的 AndroidManifest.xml 中添加:
     * <manifest>
     *     <queries>
     *         <intent>
     *             <action android:name="android.view.InputMethod" />
     *         </intent>
     *     </queries>
     * </manifest>
     * 当前实现可能影响开机速度，如需此修复需手动设置系统框架作用域。
     */
    private void startPermissionHook() {
        hookAllMethods("com.android.server.pm.AppsFilterUtils", "canQueryViaComponents", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (Boolean.TRUE.equals(param.getResult()) || param.getArgs().length < 2) {
                    return;
                }

                Object queryingPackage = param.getArgs()[0];
                Object targetPackage = param.getArgs()[1];
                String queryingPackageName = getAndroidPackageName(queryingPackage);
                if (!InputMethodConfig.isMiuiImeUnlockPackage(queryingPackageName)) {
                    return;
                }

                if (isIme(queryingPackage) && isIme(targetPackage)) {
                    param.setResult(true);
                }
            }
        });
    }

    private String getAndroidPackageName(Object androidPackage) {
        try {
            Object packageName = callMethod(androidPackage, "getPackageName");
            return packageName instanceof String ? (String) packageName : null;
        } catch (Throwable t) {
            XposedLog.w(TAG, "Read android package name failed: " + t);
            return null;
        }
    }

    private boolean isIme(Object androidPackage) {
        if (androidPackage == null) {
            return false;
        }

        try {
            Object services = callMethod(androidPackage, "getServices");
            if (!(services instanceof List<?> serviceList)) {
                return false;
            }

            for (Object service : serviceList) {
                Object exported = callMethod(service, "isExported");
                if (!(exported instanceof Boolean isExported) || !isExported) {
                    continue;
                }

                Object intents = callMethod(service, "getIntents");
                if (!(intents instanceof List<?> intentList)) {
                    continue;
                }

                for (Object intent : intentList) {
                    Object intentFilter = callMethod(intent, "getIntentFilter");
                    if (intentFilter instanceof IntentFilter filter &&
                        filter.matchAction("android.view.InputMethod")) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, "Check IME package failed: " + t);
        }

        return false;
    }
}
