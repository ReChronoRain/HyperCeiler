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

package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;

public class AntiQues extends BaseHook {
    XposedInterface.MethodUnhooker<?> clHook;

    @Override
    public void init() {
        clHook = findAndHookMethod("com.android.server.SystemServiceManager",
            "loadClassFromLoader", String.class, ClassLoader.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    try {
                        String clzName = (String) param.getArgs()[0];
                        ClassLoader cl = (ClassLoader) param.getArgs()[1];
                        if (clzName.equals("com.android.server.wifi.WifiService")) {
                            EzxHelpUtils.findAndHookMethod("com.android.server.wifi.Utils", cl, "checkDeviceNameIsIllegalSync", Context.class, int.class, String.class,
                                new IMethodHook() {
                                    @Override
                                    public void before(BeforeHookParam param) {
                                        param.setResult(false);
                                    }
                                });
                            clHook.unhook();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
    }
}
