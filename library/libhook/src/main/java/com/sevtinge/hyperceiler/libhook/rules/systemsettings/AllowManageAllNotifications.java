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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.HashSet;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AllowManageAllNotifications extends BaseHook {
    @Override
    public void init() {

        findAndHookMethod("com.android.settings.notification.AppNotificationSettings", "setupBlock", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.ChannelNotificationSettings", "setupBlock", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.app.AppNotificationSettings", "setupBlock", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("com.android.settings.notification.app.ChannelNotificationSettings", "setupBlock", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                findAndHookMethod("androidx.preference.Preference", "setEnabled", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                        param.setResult(true);
                    }
                });
            }
        });

        findAndHookMethod("android.app.NotificationChannel", "isBlockable", returnConstant(true));

        findAndHookMethod("android.app.NotificationChannel", "setBlockable", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        hookAllMethods("miui.util.NotificationFilterHelper", "isNotificationForcedEnabled", returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "isNotificationForcedFor", Context.class, String.class, returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "canSystemNotificationBeBlocked", String.class, returnConstant(true));
        findAndHookMethod("miui.util.NotificationFilterHelper", "containNonBlockableChannel", String.class, returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "getNotificationForcedEnabledList", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(new HashSet<String>());
            }
        });

    }
}
