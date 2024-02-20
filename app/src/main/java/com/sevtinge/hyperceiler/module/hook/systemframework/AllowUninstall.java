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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AllowUninstall implements IXposedHookZygoteInit {

    private final String SecurityManagerServiceName = "com.miui.server.SecurityManagerService$1";

    private PathClassLoader servicesClassLoader = null;

    private Class<?> SecurityManagerServiceClazz = null;

    private java.util.Set<XC_MethodHook.Unhook> pathClassLoaderHook = null;

    @Override
    public void initZygote(StartupParam startupParam) {
        // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: hook all PathClassLoader Constructors");
        pathClassLoaderHook =
            XposedBridge.hookAllConstructors(PathClassLoader.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String path = param.args[0].toString();
                    if (path.contains("/system/framework/services.jar")) {
                        // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: find services.jar ClassLoader");
                        try {
                            servicesClassLoader = (PathClassLoader) param.thisObject;
                            SecurityManagerServiceClazz = XposedHelpers.findClass(
                                SecurityManagerServiceName,
                                servicesClassLoader);
                            // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: findClass SecurityManagerService$1");
                            XposedHelpers.findAndHookMethod(SecurityManagerServiceClazz,
                                "run", new XC_MethodReplacement() {
                                    @Override
                                    protected Object replaceHookedMethod(MethodHookParam unused) {
                                        // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: hooked checkSystemSelfProtection invoke");
                                        return null;
                                    }
                                });
                            // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: hook method 'SecurityManagerService$1.run()'");
                        } catch (Exception e) {
                            XposedBridge.log("[HyperCeiler][E][android][AllowUninstall]: AllowUninstall Exception! " + e);
                            // e.printStackTrace();
                        } finally {
                            for (Unhook hook : pathClassLoaderHook) {
                                hook.unhook();
                            }
                            // XposedBridge.log("[HyperCeiler][I][android][AllowUninstall]: unhook all PathClassLoader Constructors");
                        }
                    }
                }
            });
    }


}
