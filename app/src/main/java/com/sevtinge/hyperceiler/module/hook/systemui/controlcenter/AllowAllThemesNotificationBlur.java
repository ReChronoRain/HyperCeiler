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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AllowAllThemesNotificationBlur extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.systemui.statusbar.phone.ConfigurationControllerImpl$onMiuiThemeChanged$1", boolean.class,boolean.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true; //isDefaultLockScreenTheme
                param.args[1] = true; //sDefaultSysUiTheme
            }
        });
        findAndHookMethod("com.android.systemui.shade.MiuiNotificationPanelViewController$MiuiConfigurationListener", "onMiBlurChanged", boolean.class,new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(findClass("com.miui.utils.MiuiThemeUtils"), "sDefaultSysUiTheme", true);
                findAndHookMethod("com.miui.systemui.util.CommonUtil", "isDefaultLockScreenTheme", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });
        findAndHookMethod("com.miui.systemui.util.MiBlurCompat","getBackgroundBlurOpened",Context.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.notification.NotificationUtil", "isBackgroundBlurOpened",Context.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(findClass("com.miui.utils.MiuiThemeUtils"), "sDefaultSysUiTheme", true);
                findAndHookMethod("com.miui.systemui.util.CommonUtil", "isDefaultLockScreenTheme", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });
    }
}
