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
package com.sevtinge.hyperceiler.hook.module.rules.systemframework;

import static io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass;

import android.hardware.display.DisplayManager;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.lang.reflect.Field;
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
    private static Field captureSecureLayersField;

    private static final Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            logE("HookTool", "Failed to get deoptimizeMethod: " + t);
        }
        deoptimizeMethod = m;
    }

    public static void deoptimizeMethods(Class<?> c, String n) {
        for (Method m : c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals(n)) {
                try {
                    deoptimizeMethod.invoke(null, m);
                } catch (Throwable t) {
                    logE("HookTool", "Failed to deoptimize methods " + m + ": " + t);
                }
            }
        }
    }

    @Override
    public void init() {
        try {
            deoptimizeMethods(XposedHelpers.findClass("com.android.server.wm.WindowStateAnimator", lpparam.classLoader), "createSurfaceLocked");
            deoptimizeMethods(XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader), "relayoutWindow");

            for (int i = 0; i < 20; i++) {
                try {
                    var clazz = loadClass("com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda" + i, lpparam.classLoader);
                    if (BiConsumer.class.isAssignableFrom(clazz)) {
                        deoptimizeMethods(clazz, "accept");
                    }
                } catch (ClassNotFoundException ignored) {
                }
                try {
                    var clazz = loadClass("com.android.server.wm.DisplayContent$" + i, lpparam.classLoader);
                    if (BiPredicate.class.isAssignableFrom(clazz)) {
                        deoptimizeMethods(clazz, "test");
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "deoptimize system server failed", t);
        }

        // Screen record detection (V~Baklava)
        try {
            var windowManagerServiceClazz = XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
            var iScreenRecordingCallbackClazz = XposedHelpers.findClass("android.window.IScreenRecordingCallback", lpparam.classLoader);
            var method = windowManagerServiceClazz.getDeclaredMethod("registerScreenRecordingCallback", iScreenRecordingCallbackClazz);
            hookMethod(method, MethodHook.returnConstant(false));
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook WindowManagerService failed", t);
        }

        // Screenshot detection (U~Baklava)
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader,
                "registerScreenCaptureObserver",
                XposedHelpers.findClass("android.os.IBinder", lpparam.classLoader),
                XposedHelpers.findClass("android.app.IScreenCaptureObserver", lpparam.classLoader),
                XC_MethodReplacement.DO_NOTHING);
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook ActivityTaskManagerService failed", t);
        }

        // Xiaomi HyperOS (U~Baklava)
        // Baklava by OS2.0.250701.1.WOBCNXM.PRE
        try {
            hookAllMethods(
                "com.android.server.wm.WindowManagerServiceImpl", lpparam.classLoader,
                "notAllowCaptureDisplay",
                MethodHook.returnConstant(false));
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook HyperOS failed", t);
        }

        // ScreenCapture in WindowManagerService (S~Baklava)
        try {
            var captureArgsClazz = XposedHelpers.findClass("android.window.ScreenCapture$CaptureArgs", lpparam.classLoader);
            captureSecureLayersField = captureArgsClazz.getDeclaredField("mCaptureSecureLayers");
            captureSecureLayersField.setAccessible(true);
            hookAllMethods("android.window.ScreenCapture", lpparam.classLoader, "nativeCaptureDisplay",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        try {
                            captureSecureLayersField.set(param.args[0], true);
                        } catch (IllegalAccessException t) {
                            logE(TAG, "android", "ScreenCaptureHooker failed", t);
                        }
                    }
                });
            hookAllMethods("android.window.ScreenCapture", lpparam.classLoader, "nativeCaptureLayers",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        try {
                            captureSecureLayersField.set(param.args[0], true);
                        } catch (IllegalAccessException t) {
                            logE(TAG, "android", "ScreenCaptureHooker failed", t);
                        }
                    }
                });
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook ScreenCapture failed", t);
        }

        // WifiDisplay (S~Baklava) / OverlayDisplay (S~Baklava) / VirtualDisplay (U~Baklava)
        try {
            var displayControlClazz = XposedHelpers.findClass("com.android.server.display.DisplayControl", lpparam.classLoader);
            var method = displayControlClazz.getDeclaredMethod("createVirtualDisplay", String.class, boolean.class);
            hookMethod(method, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.args[1] = true;
                }
            });
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook DisplayControl failed", t);
        }

        // VirtualDisplay with MediaProjection (S~Baklava)
        try {
            hookAllMethods("com.android.server.display.VirtualDisplayAdapter", lpparam.classLoader,
                "createVirtualDisplayLocked",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        var caller = (int) param.args[2];
                        if (caller >= 10000 && param.args[1] == null) {
                            // not os and not media projection
                            return;
                        }
                        for (int i = 3; i < param.args.length; i++) {
                            var arg = param.args[i];
                            if (arg instanceof Integer) {
                                var flags = (int) arg;
                                flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
                                param.args[i] = flags;
                                return;
                            }
                        }
                        logE(TAG, "android", "flag not found in CreateVirtualDisplayLockedHooker");
                    }
                });
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook VirtualDisplayAdapter failed", t);
        }

        // secureLocked flag
        try {
            // Screenshot
            var windowStateClazz = XposedHelpers.findClass("com.android.server.wm.WindowState", lpparam.classLoader);
            var isSecureLockedMethod = windowStateClazz.getDeclaredMethod("isSecureLocked");
            hookMethod(isSecureLockedMethod, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    var walker = StackWalker.getInstance();
                    var match = walker.walk(frames -> frames
                        .map(StackWalker.StackFrame::getMethodName)
                        .limit(6)
                        .skip(2)
                        .anyMatch(s -> s.equals("setInitialSurfaceControlProperties") || s.equals("createSurfaceLocked")));
                    if (match) return;

                    param.setResult(false);
                }
            });
        } catch (Throwable t) {
            logE(TAG, this.lpparam.packageName, "hook WindowState failed", t);
        }
    }
}
