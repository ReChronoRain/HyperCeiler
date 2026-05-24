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
package com.sevtinge.hyperceiler.libhook.rules.milink;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

public class DisablePcManagerClipboardRegionCheck extends BaseHook {
    @Override
    public void init() {
        IMethodHook regionCheckHook = returnConstant(true);
        IMethodHook regionValueHook = returnConstant("cn");

        hookRegionUtils("com.xiaomi.dist.universalclipboardservice.utils.LyraUtil", regionCheckHook, regionValueHook);
        hookRegionUtils("com.xiaomi.dist.file.service.utils.LyraUtil", regionCheckHook, regionValueHook);
    }

    private void hookRegionUtils(String className, IMethodHook regionCheckHook, IMethodHook regionValueHook) {
        try {
            findAndHookMethod(className, "isSameRegionWithLocal", Context.class, String.class, regionCheckHook);
            XposedLog.i(TAG, getPackageName(), "Hooked " + className + "#isSameRegionWithLocal");
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "Failed to hook " + className + "#isSameRegionWithLocal", t);
        }

        try {
            findAndHookMethod(className, "getDeviceRegion", Context.class, String.class, regionValueHook);
            XposedLog.i(TAG, getPackageName(), "Hooked " + className + "#getDeviceRegion");
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "Failed to hook " + className + "#getDeviceRegion", t);
        }
    }
}
