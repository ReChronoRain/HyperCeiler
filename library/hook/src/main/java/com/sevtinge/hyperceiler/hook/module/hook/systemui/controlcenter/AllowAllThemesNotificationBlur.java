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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class AllowAllThemesNotificationBlur extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.shade.MiuiNotificationPanelViewController$MiuiConfigurationListener", "onMiBlurChanged", boolean.class,new MethodHook(){
            XC_MethodHook.Unhook isDefaultLockScreenThemeHook;

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(findClass("com.miui.utils.MiuiThemeUtils"), "sDefaultSysUiTheme", true);
                isDefaultLockScreenThemeHook = findAndHookMethodUseUnhook("com.miui.systemui.util.CommonUtil", lpparam.classLoader, "isDefaultLockScreenTheme", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (isDefaultLockScreenThemeHook != null) isDefaultLockScreenThemeHook.unhook();
                isDefaultLockScreenThemeHook = null;
            }
        });
        findAndHookMethod("com.miui.systemui.util.MiBlurCompat","getBackgroundBlurOpened", Context.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        MethodHook blurHook = new MethodHook(){
            XC_MethodHook.Unhook isDefaultLockScreenThemeHook;

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(findClass("com.miui.utils.MiuiThemeUtils"), "sDefaultSysUiTheme", true);
                isDefaultLockScreenThemeHook = findAndHookMethodUseUnhook("com.miui.systemui.util.CommonUtil", lpparam.classLoader, "isDefaultLockScreenTheme", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (isDefaultLockScreenThemeHook != null) isDefaultLockScreenThemeHook.unhook();
                isDefaultLockScreenThemeHook = null;
            }
        };

        if (isMoreHyperOSVersion(2f)) {
            findAndHookMethod("com.miui.systemui.notification.MiuiBaseNotifUtil", "isBackgroundBlurOpened", Context.class, blurHook);
        } else {
            findAndHookMethod("com.android.systemui.statusbar.notification.NotificationUtil", "isBackgroundBlurOpened", Context.class, blurHook);
        }
    }
}
