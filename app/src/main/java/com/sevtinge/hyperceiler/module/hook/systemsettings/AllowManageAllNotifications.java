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
package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import java.util.HashSet;

import de.robv.android.xposed.XC_MethodReplacement;

public class AllowManageAllNotifications extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        findAndHookMethod("com.android.settings.notification.AppNotificationSettings", "setupBlock", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.ChannelNotificationSettings", "setupBlock", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.app.AppNotificationSettings", "setupBlock", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.app.ChannelNotificationSettings", "setupBlock", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("android.app.NotificationChannel", "isBlockable", MethodHook.returnConstant(true));

        findAndHookMethod("android.app.NotificationChannel", "setBlockable", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

        hookAllMethods("miui.util.NotificationFilterHelper", "isNotificationForcedEnabled", MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "isNotificationForcedFor", Context.class, String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "canSystemNotificationBeBlocked", String.class, MethodHook.returnConstant(true));
        findAndHookMethod("miui.util.NotificationFilterHelper", "containNonBlockableChannel", String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "getNotificationForcedEnabledList", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(new HashSet<String>());
            }
        });

    }
}
