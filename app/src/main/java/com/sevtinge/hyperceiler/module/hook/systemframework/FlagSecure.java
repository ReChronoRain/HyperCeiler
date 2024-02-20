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

import android.os.Build;
import android.util.Log;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FlagSecure extends BaseHook {

    /*@Override
    public void init() {
        findAndHookMethod("com.android.server.wm.WindowState", "isSecureLocked", XC_MethodReplacement.returnConstant(false));
    }*/

    private final static Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Class<?> c, String n) throws InvocationTargetException, IllegalAccessException {
        for (Method m : c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals(n)) {
                deoptimizeMethod.invoke(null, m);
                Log.d("DisableFlagSecure", "Deoptimized " + m);
            }
        }
    }

    @Override
    public void init() {
        if (lpparam.packageName.equals("android")) {
            try {
                Class<?> windowsState = XposedHelpers.findClass("com.android.server.wm.WindowState", lpparam.classLoader);
                Class<?> windowsManagerServiceImpl = XposedHelpers.findClassIfExists("com.android.server.wm.WindowManagerServiceImpl", lpparam.classLoader);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    XposedHelpers.findAndHookMethod(
                        windowsState,
                        "isSecureLocked",
                        XC_MethodReplacement.returnConstant(false));
                } else {
                    XposedHelpers.findAndHookMethod(
                        "com.android.server.wm.WindowManagerService",
                        lpparam.classLoader,
                        "isSecureLocked",
                        windowsState,
                        XC_MethodReplacement.returnConstant(false));
                }
                hookAllMethods(windowsManagerServiceImpl, "notAllowCaptureDisplay", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            } catch (Throwable t) {
                logE(TAG, this.lpparam.packageName, t);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    XposedHelpers.findAndHookMethod(
                        "com.android.server.wm.ActivityTaskManagerService",
                        lpparam.classLoader,
                        "registerScreenCaptureObserver",
                        "android.os.IBinder",
                        "android.app.IScreenCaptureObserver",
                        XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable t) {
                    logE(TAG, this.lpparam.packageName, t);
                }
            }

            try {
                deoptimizeMethod(XposedHelpers.findClass("com.android.server.wm.WindowStateAnimator", lpparam.classLoader), "createSurfaceLocked");
                var c = XposedHelpers.findClass("com.android.server.display.DisplayManagerService", lpparam.classLoader);
                deoptimizeMethod(c, "setUserPreferredModeForDisplayLocked");
                deoptimizeMethod(c, "setUserPreferredDisplayModeInternal");
                c = XposedHelpers.findClass("com.android.server.wm.InsetsPolicy$InsetsPolicyAnimationControlListener", lpparam.classLoader);
                for (var m : c.getDeclaredConstructors()) {
                    deoptimizeMethod.invoke(null, m);
                }
                c = XposedHelpers.findClass("com.android.server.wm.InsetsPolicy", lpparam.classLoader);
                deoptimizeMethod(c, "startAnimation");
                deoptimizeMethod(c, "controlAnimationUnchecked");
                for (int i = 0; i < 20; i++) {
                    c = XposedHelpers.findClassIfExists("com.android.server.wm.DisplayContent$$ExternalSyntheticLambda" + i, lpparam.classLoader);
                    if (c != null && BiPredicate.class.isAssignableFrom(c)) {
                        deoptimizeMethod(c, "test");
                    }
                }
                c = XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
                deoptimizeMethod(c, "relayoutWindow");
                for (int i = 0; i < 20; i++) {
                    c = XposedHelpers.findClassIfExists("com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda" + i, lpparam.classLoader);
                    if (c != null && BiConsumer.class.isAssignableFrom(c)) {
                        deoptimizeMethod(c, "accept");
                    }
                }
            } catch (Throwable t) {
                logE(TAG, this.lpparam.packageName, t);
            }
        }
    }
}
