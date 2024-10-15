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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.HashSet;

import de.robv.android.xposed.XposedHelpers;

public class AllowManageAllNotifications extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> mNotifyManagerCls = isMoreAndroidVersion(35) ?
                findClassIfExists("com.miui.systemui.notification.NotificationSettingsManager", lpparam.classLoader) :
                findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(mNotifyManagerCls, "USE_WHITE_LISTS", false);

        try {
            // Android 14 中期开始变化为 getCloudDataString 方法
            findAndHookMethod("com.miui.systemui.CloudDataManager$Companion", "getCloudDataString", Context.class, String.class, String.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult((new ArrayList<String>()).toString());
                }
            });
        } catch (Throwable t) {
            findAndHookMethod("com.miui.systemui.NotificationCloudData$Companion", "getFloatBlacklist", Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(new ArrayList<String>());
                }
            });
        }

        // miui-framework.jar
        hookAllMethods("miui.util.NotificationFilterHelper", "isNotificationForcedEnabled", MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "isNotificationForcedFor", Context.class, String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "canSystemNotificationBeBlocked", String.class, MethodHook.returnConstant(true));
        findAndHookMethod("miui.util.NotificationFilterHelper", "containNonBlockableChannel", String.class, MethodHook.returnConstant(false));
        findAndHookMethod("miui.util.NotificationFilterHelper", "getNotificationForcedEnabledList", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(new HashSet<String>());
            }
        });

    }
}
