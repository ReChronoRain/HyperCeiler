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
package com.sevtinge.hyperceiler.libhook.rules.incallui;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;


public class HideCrbt extends BaseHook {
    Class<?> loadClass;

    public void init() {
        loadClass = findClassIfExists("com.android.incallui.Call");
        try {
            hookAllMethods(loadClass, "getVideoCall", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });
            findAndHookMethod(loadClass, "setPlayingVideoCrbt", int.class, boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.getArgs()[0] = 0;
                    param.getArgs()[1] = Boolean.FALSE;
                }
            });
        } catch (Exception e) {
            XposedLog.e(TAG, getPackageName(), "method hooked failed! " + e);
        }
    }
}
