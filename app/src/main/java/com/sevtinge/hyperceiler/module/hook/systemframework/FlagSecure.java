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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceControl;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    private static final Method deoptimizeMethod;
    private static Field captureSecureLayersField;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethods(Class<?> clazz, String... names) throws InvocationTargetException, IllegalAccessException {
        var list = Arrays.asList(names);
        for (Method method : clazz.getDeclaredMethods()) {
            if (deoptimizeMethod != null && list.contains(method.getName())) {
                deoptimizeMethod.invoke(null, method);
                Log.d("FlagSecure", "Deoptimized " + method);
            }
        }
    }

    static void deoptimizeConstructors(Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (deoptimizeMethod != null) {
                deoptimizeMethod.invoke(null, constructor);
                Log.d("DisableFlagSecure", "Deoptimized constructor " + constructor);
            }
        }
    }

    @Override
    public void init() {
        if (lpparam.packageName.equals("android")) {
            try {
                deoptimizeMethods(XposedHelpers.findClass("com.android.server.wm.WindowStateAnimator", lpparam.classLoader), "createSurfaceLocked");

                deoptimizeMethods(XposedHelpers.findClass("com.android.server.display.DisplayManagerService", lpparam.classLoader),
                        "setUserPreferredModeForDisplayLocked", "setUserPreferredDisplayModeInternal");

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    Class<?> insetsPolicyListenerClass = XposedHelpers.findClass("com.android.server.wm.InsetsPolicy$InsetsPolicyAnimationControlListener", lpparam.classLoader);
                    deoptimizeConstructors(insetsPolicyListenerClass);
                }

                deoptimizeMethods(XposedHelpers.findClass("com.android.server.wm.InsetsPolicy", lpparam.classLoader),
                        "startAnimation", "controlAnimationUnchecked");

                for (int i = 0; i < 20; i++) {
                    Class<?> clazz = XposedHelpers.findClassIfExists("com.android.server.wm.DisplayContent$$ExternalSyntheticLambda" + i, lpparam.classLoader);
                    if (clazz != null && BiPredicate.class.isAssignableFrom(clazz)) {
                        deoptimizeMethods(clazz, "test");
                    }
                }

                deoptimizeMethods(XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader), "relayoutWindow");

                for (int i = 0; i < 20; i++) {
                    Class<?> clazz = XposedHelpers.findClassIfExists("com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda" + i, lpparam.classLoader);
                    if (clazz != null && BiConsumer.class.isAssignableFrom(clazz)) {
                        deoptimizeMethods(clazz, "accept");
                    }
                }
            } catch (Throwable t) {
                logE(TAG, this.lpparam.packageName, "deoptimize system server failed", t);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // Screen record detection (V)
                try {
                    var windowManagerServiceClazz = XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
                    var iScreenRecordingCallbackClazz = XposedHelpers.findClass("android.window.IScreenRecordingCallback", lpparam.classLoader);
                    var method = windowManagerServiceClazz.getDeclaredMethod("registerScreenRecordingCallback", iScreenRecordingCallbackClazz);
                    hookMethod(method, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.setResult(false);
                        }
                    });
                } catch (Throwable t) {
                    logE(TAG, this.lpparam.packageName, "hook WindowManagerService failed", t);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Screenshot detection (U~V)
                try {
                    XposedHelpers.findAndHookMethod(
                            "com.android.server.wm.ActivityTaskManagerService",
                            lpparam.classLoader,
                            "registerScreenCaptureObserver",
                            "android.os.IBinder",
                            "android.app.IScreenCaptureObserver",
                            XC_MethodReplacement.DO_NOTHING);
                } catch (Throwable t) {
                    logE(TAG, this.lpparam.packageName, "hook ActivityTaskManagerService failed", t);
                }

                // Xiaomi HyperOS (U)
                try {
                    var windowManagerServiceImplClazz = XposedHelpers.findClass("com.android.server.wm.WindowManagerServiceImpl", lpparam.classLoader);
                    XposedHelpers.findAndHookMethod(
                            windowManagerServiceImplClazz,
                            "notAllowCaptureDisplay",
                            XposedHelpers.findClass("com.android.server.wm.RootWindowContainer", lpparam.classLoader), int.class,
                            XC_MethodReplacement.returnConstant(false));
                } catch (Throwable t) {
                    logE(TAG, this.lpparam.packageName, "hook HyperOS failed", t);
                }
            }

            // ScreenCapture in WindowManagerService (S~V)
            try {
                var screenCaptureClazz = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
                        XposedHelpers.findClass("android.window.ScreenCapture", lpparam.classLoader) :
                        SurfaceControl.class;
                var captureArgsClazz = XposedHelpers.findClass(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
                        "android.window.ScreenCapture$CaptureArgs" :
                        "android.view.SurfaceControl$CaptureArgs", lpparam.classLoader);
                var displayCaptureArgsClazz = XposedHelpers.findClass("android.window.ScreenCapture$DisplayCaptureArgs", lpparam.classLoader);
                var layerCaptureArgsClazz = XposedHelpers.findClass("android.window.ScreenCapture$LayerCaptureArgs", lpparam.classLoader);
                captureSecureLayersField = captureArgsClazz.getDeclaredField("mCaptureSecureLayers");
                captureSecureLayersField.setAccessible(true);
                XposedHelpers.findAndHookMethod(screenCaptureClazz, "nativeCaptureDisplay",
                        displayCaptureArgsClazz, long.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                try {
                                    captureSecureLayersField.set(param.args[0], true);
                                } catch (IllegalAccessException t) {
                                    logE(TAG, "android", "ScreenCaptureHooker failed", t);
                                }
                            }
                        });
                XposedHelpers.findAndHookMethod(screenCaptureClazz, "nativeCaptureLayers", layerCaptureArgsClazz, long.class, boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Blackout permission check (S~T)
                try {
                    var activityTaskManagerServiceClazz = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader);
                    var method = activityTaskManagerServiceClazz.getDeclaredMethod("checkPermission", String.class, int.class, int.class);
                    hookMethod(method, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            var permission = param.args[0];
                            if ("android.permission.CAPTURE_BLACKOUT_CONTENT".equals(permission)) {
                                param.args[0] = "android.permission.READ_FRAME_BUFFER";
                            }
                        }
                    });
                } catch (Throwable t) {
                    logE(TAG, this.lpparam.packageName, "hook ActivityManagerService failed", t);
                }
            }

            // WifiDisplay (S~V) / OverlayDisplay (S~V) / VirtualDisplay (U~V)
            try {
                var displayControlClazz = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
                        XposedHelpers.findClass("com.android.server.display.DisplayControl", lpparam.classLoader) :
                        SurfaceControl.class;
                var method = displayControlClazz.getDeclaredMethod(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM ?
                                "createVirtualDisplay" :
                                "createDisplay", String.class, boolean.class);
                hookMethod(method, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            String stack = Log.getStackTraceString(new Throwable());
                            if (stack.contains("createVirtualDisplayLocked")) {
                                return;
                            }
                        }
                        param.args[1] = true;
                    }
                });
            } catch (Throwable t) {
                logE(TAG, this.lpparam.packageName, "hook DisplayControl failed", t);
            }

            // VirtualDisplay with MediaProjection (S~V)
            try {
                var displayControlClazz = XposedHelpers.findClass("com.android.server.display.VirtualDisplayAdapter", lpparam.classLoader);
                var iVirtualDisplayCallback = XposedHelpers.findClass("android.hardware.display.IVirtualDisplayCallback", lpparam.classLoader);
                var iMediaProjection = XposedHelpers.findClass("android.media.projection.IMediaProjection", lpparam.classLoader);
                var surface = XposedHelpers.findClass("android.view.Surface", lpparam.classLoader);
                var virtualDisplayConfig = XposedHelpers.findClass("android.hardware.display.VirtualDisplayConfig", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(displayControlClazz, "createVirtualDisplayLocked",
                        iVirtualDisplayCallback, iMediaProjection, int.class, String.class, String.class, surface, int.class, virtualDisplayConfig,
                        new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        var caller = (int) param.args[2];
                        if (caller != 1000 && param.args[1] == null) {
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
        } else if (lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.miui.screenshot")) {
            try {
                var screenCaptureClazz = SurfaceControl.class;
                var captureArgsClazz = XposedHelpers.findClass(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
                        "android.window.ScreenCapture$CaptureArgs" :
                        "android.view.SurfaceControl$CaptureArgs", lpparam.classLoader);
                var displayCaptureArgsClazz = XposedHelpers.findClass("android.window.ScreenCapture$DisplayCaptureArgs", lpparam.classLoader);
                var layerCaptureArgsClazz = XposedHelpers.findClass("android.window.ScreenCapture$LayerCaptureArgs", lpparam.classLoader);
                captureSecureLayersField = captureArgsClazz.getDeclaredField("mCaptureSecureLayers");
                captureSecureLayersField.setAccessible(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    XposedHelpers.findAndHookMethod("android.window.ScreenCapture", lpparam.classLoader, "nativeCaptureDisplay",
                            displayCaptureArgsClazz, long.class,
                            new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    try {
                                        captureSecureLayersField.set(param.args[0], true);
                                    } catch (IllegalAccessException t) {
                                        logE(TAG, "other", "ScreenCaptureHooker failed", t);
                                    }
                                }
                            });
                    XposedHelpers.findAndHookMethod("android.window.ScreenCapture", lpparam.classLoader, "nativeCaptureLayers",
                            layerCaptureArgsClazz, long.class, boolean.class,
                            new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            try {
                                captureSecureLayersField.set(param.args[0], true);
                            } catch (IllegalAccessException t) {
                                logE(TAG, "other", "ScreenCaptureHooker failed", t);
                            }
                        }
                    });
                } else {
                    XposedHelpers.findAndHookMethod(screenCaptureClazz, "nativeCaptureDisplay",
                            new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) throws Throwable {
                                    try {
                                        captureSecureLayersField.set(param.args[0], true);
                                    } catch (IllegalAccessException t) {
                                        logE(TAG, "other", "ScreenCaptureHooker failed", t);
                                    }
                                }
                            });
                    XposedHelpers.findAndHookMethod(screenCaptureClazz, "nativeCaptureLayers", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            try {
                                captureSecureLayersField.set(param.args[0], true);
                            } catch (IllegalAccessException t) {
                                logE(TAG, "other", "ScreenCaptureHooker failed", t);
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                logE(TAG, this.lpparam.packageName, "hook ScreenCapture failed", t);
            }
        }
    }
}
