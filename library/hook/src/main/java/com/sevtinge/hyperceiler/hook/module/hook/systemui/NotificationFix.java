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
package com.sevtinge.hyperceiler.hook.module.hook.systemui;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NotificationFix extends BaseHook {
    @Override
    public void init() {
        String className;
        String fieldName = "USE_WHITE_LISTS";

        if (isMoreAndroidVersion(35))
            className = "com.miui.systemui.notification.NotificationSettingsManager";
        else
            className = "com.android.systemui.statusbar.notification.NotificationSettingsManager";

        XposedHelpers.setStaticBooleanField(XposedHelpers.findClass(className, lpparam.classLoader), fieldName, false);
    }
}
