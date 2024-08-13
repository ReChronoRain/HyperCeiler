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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class AntiQues extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        XposedHelpers.findAndHookMethod("com.android.server.SystemServiceManager", lpparam.classLoader,
                "loadClassFromLoader", String.class, ClassLoader.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String clzName = (String) param.args[0];
                            ClassLoader cl = (ClassLoader) param.args[1];
                            if (clzName.equals("com.android.server.wifi.WifiService")) {
                                XposedHelpers.findAndHookMethod("com.android.server.wifi.Utils", cl, "checkDeviceNameIsIllegalSync", Context.class, int.class, String.class,
                                        new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                param.setResult(false);
                                            }
                                        });
                            }
                        } catch (Throwable t) {
                        }
                    }
                });
    }
}
