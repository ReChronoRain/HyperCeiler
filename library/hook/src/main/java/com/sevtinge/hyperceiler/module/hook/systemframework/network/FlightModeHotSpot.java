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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemframework.network;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FlightModeHotSpot extends BaseHook {
    XC_MethodHook.Unhook clHook;

    @Override
    public void init() {
        try {
            clHook = XposedHelpers.findAndHookMethod("com.android.server.SystemServiceManager", lpparam.classLoader,
                    "loadClassFromLoader", String.class, ClassLoader.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                String clzName = (String) param.args[0];
                                ClassLoader cl = (ClassLoader) param.args[1];
                                if (clzName.equals("com.android.server.wifi.WifiService")) {
                                    Class<?> cls = XposedHelpers.findClass("com.android.server.wifi.WifiServiceImpl$SoftApCallbackInternal", cl);
                                    XposedHelpers.findAndHookMethod("com.android.server.wifi.sap.MiuiWifiApManager", cl, "resetSoftApStateIfNeeded",
                                            cls, int.class, boolean.class, boolean.class,
                                            new XC_MethodHook() {
                                                @Override
                                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                    param.setResult(false);
                                                }
                                            });
                                    clHook.unhook();
                                }
                            } catch (Throwable t) {
                                logE(TAG, lpparam.packageName, "Hook MiuiWifiApManager Failed, " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            logE(TAG, lpparam.packageName, "Hook classloader defined by MIUI failed, " + t);
        }
    }
}
