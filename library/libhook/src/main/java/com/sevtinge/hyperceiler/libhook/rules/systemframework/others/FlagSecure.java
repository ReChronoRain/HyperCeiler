package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimizeMethods;

import android.hardware.display.DisplayManager;
import android.os.Build;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

public class FlagSecure {

    private static final String TAG = "FlagSecure";

    private static Field captureSecureLayersField;

    @FunctionalInterface
    private interface ChainHooker {
        Object intercept(XposedInterface.Chain chain) throws Throwable;
    }

    public void onLoad(XposedModuleInterface.SystemServerStartingParam lpparam) {
        var classLoader = lpparam.getClassLoader();

        try {
            deoptimizeSystemServer(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "deoptimize system server failed", t);
        }

        // Screen record detection (V~Baklava)
        try {
            hookWindowManagerService(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook WindowManagerService failed", t);
        }

        // Screenshot detection (U~Baklava)
        try {
            hookActivityTaskManagerService(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook ActivityTaskManagerService failed", t);
        }

        // Xiaomi HyperOS (U~Baklava)
        try {
            hookHyperOS(classLoader);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook HyperOS failed", t);
        }

        // ScreenCapture in WindowManagerService (S~Baklava)
        try {
            hookScreenCapture(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook ScreenCapture failed", t);
        }

        // WifiDisplay (S~Baklava) / OverlayDisplay (S~Baklava) / VirtualDisplay (U~Baklava)
        try {
            hookDisplayControl(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook DisplayControl failed", t);
        }

        // VirtualDisplay with MediaProjection (S~Baklava)
        try {
            hookVirtualDisplayAdapter(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook VirtualDisplayAdapter failed", t);
        }

        // secureLocked flag
        try {
            hookWindowState(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook WindowState failed", t);
        }
    }

    private final ChainHooker createDisplayHooker = chain -> {
        Object[] args = chain.getArgs().toArray();
        args[1] = true;
        return chain.proceed(args);
    };

    private final ChainHooker screenCaptureHooker = chain -> {
        var captureArgs = chain.getArg(0);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && isAtLeastBaklava1()) {
                captureSecureLayersField.set(captureArgs, 1);
            } else {
                captureSecureLayersField.set(captureArgs, true);
            }
        } catch (IllegalAccessException t) {
            XposedLog.e(TAG, "system", "ScreenCaptureHooker failed", t);
        }
        return chain.proceed();
    };

    private final ChainHooker createVirtualDisplayLockedHooker = chain -> {
        var caller = (int) chain.getArg(2);
        if (caller >= 10000 && chain.getArg(1) == null) {
            // not os and not media projection
            return chain.proceed();
        }
        for (int i = 3; i < chain.getArgs().size(); i++) {
            var arg = chain.getArg(i);
            if (arg instanceof Integer) {
                var flags = (int) arg;
                flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
                Object[] args = chain.getArgs().toArray();
                args[i] = flags;
                return chain.proceed(args);
            }
        }
        XposedLog.e(TAG, "system", "flag not found in CreateVirtualDisplayLockedHooker");
        return chain.proceed();
    };

    private final ChainHooker secureLockedHooker = chain -> {
        var walker = StackWalker.getInstance();
        var match = walker.walk(frames -> frames
            .map(StackWalker.StackFrame::getMethodName)
            .limit(10)
            .skip(6)
            .anyMatch(s -> s.equals("setInitialSurfaceControlProperties") || s.equals("createSurfaceLocked")));
        if (match) return chain.proceed();
        return false;
    };

    private final ChainHooker returnFalseHooker = chain -> false;

    private final ChainHooker returnNullHooker = chain -> null;

    private void deoptimizeSystemServer(ClassLoader classLoader) throws ClassNotFoundException {
        deoptimizeMethods(
            classLoader.loadClass("com.android.server.wm.WindowStateAnimator"),
            "createSurfaceLocked");

        deoptimizeMethods(
            classLoader.loadClass("com.android.server.wm.WindowManagerService"),
            "relayoutWindow");

        for (int i = 0; i < 20; i++) {
            try {
                var clazz = classLoader.loadClass("com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda" + i);
                if (BiConsumer.class.isAssignableFrom(clazz)) {
                    deoptimizeMethods(clazz, "accept");
                }
            } catch (ClassNotFoundException ignored) {
            }
            try {
                var clazz = classLoader.loadClass("com.android.server.wm.DisplayContent$" + i);
                if (BiPredicate.class.isAssignableFrom(clazz)) {
                    deoptimizeMethods(clazz, "test");
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private void hookWindowState(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var windowStateClazz = classLoader.loadClass("com.android.server.wm.WindowState");
        var isSecureLockedMethod = windowStateClazz.getDeclaredMethod("isSecureLocked");
        hook(isSecureLockedMethod, secureLockedHooker);
    }

    private void hookScreenCapture(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> screenCaptureClazz;
        Class<?> captureArgsClazz;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && isAtLeastBaklava1()) {
            screenCaptureClazz = classLoader.loadClass("android.window.ScreenCaptureInternal");
            captureArgsClazz = classLoader.loadClass("android.window.ScreenCaptureInternal$CaptureArgs");
        } else {
            screenCaptureClazz = classLoader.loadClass("android.window.ScreenCapture");
            captureArgsClazz = classLoader.loadClass("android.window.ScreenCapture$CaptureArgs");
        }
        captureSecureLayersField = captureArgsClazz.getDeclaredField(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            isAtLeastBaklava1() ? "mSecureContentPolicy" : "mCaptureSecureLayers");
        captureSecureLayersField.setAccessible(true);
        hookMethods(screenCaptureClazz, screenCaptureHooker, "nativeCaptureDisplay");
        hookMethods(screenCaptureClazz, screenCaptureHooker, "nativeCaptureLayers");
    }

    private void hookDisplayControl(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var displayControlClazz = classLoader.loadClass("com.android.server.display.DisplayControl");
        var method = displayControlClazz.getDeclaredMethod(
            "createVirtualDisplay", String.class, boolean.class);
        hook(method, createDisplayHooker);
    }

    private void hookVirtualDisplayAdapter(ClassLoader classLoader) throws ClassNotFoundException {
        var displayControlClazz = classLoader.loadClass("com.android.server.display.VirtualDisplayAdapter");
        hookMethods(displayControlClazz, createVirtualDisplayLockedHooker, "createVirtualDisplayLocked");
    }

    private void hookActivityTaskManagerService(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var activityTaskManagerServiceClazz = classLoader.loadClass("com.android.server.wm.ActivityTaskManagerService");
        var iBinderClazz = classLoader.loadClass("android.os.IBinder");
        var iScreenCaptureObserverClazz = classLoader.loadClass("android.app.IScreenCaptureObserver");
        var method = activityTaskManagerServiceClazz.getDeclaredMethod("registerScreenCaptureObserver", iBinderClazz, iScreenCaptureObserverClazz);
        hook(method, returnNullHooker);
    }

    private void hookWindowManagerService(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var windowManagerServiceClazz = classLoader.loadClass("com.android.server.wm.WindowManagerService");
        var iScreenRecordingCallbackClazz = classLoader.loadClass("android.window.IScreenRecordingCallback");
        var method = windowManagerServiceClazz.getDeclaredMethod("registerScreenRecordingCallback", iScreenRecordingCallbackClazz);
        hook(method, returnFalseHooker);
    }

    private void hookHyperOS(ClassLoader classLoader) throws ClassNotFoundException {
        var windowManagerServiceImplClazz = classLoader.loadClass("com.android.server.wm.WindowManagerServiceImpl");
        hookMethods(windowManagerServiceImplClazz, returnFalseHooker, "notAllowCaptureDisplay");
    }

    private void hookMethods(Class<?> clazz, ChainHooker hooker, String... names) {
        List<String> list = Arrays.asList(names);
        Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> list.contains(method.getName()))
            .forEach(method -> hook(method, hooker));
    }

    private void hook(Method method, ChainHooker hooker) {
        EzxHelpUtils.chain(method, hooker::intercept);
    }

    private static boolean isAtLeastBaklava1() {
        try {
            int sdkFull = -1;
            try {
                Field f = Build.VERSION.class.getDeclaredField("SDK_INT_FULL");
                f.setAccessible(true);
                sdkFull = f.getInt(null);
            } catch (Throwable ignored) {
            }

            int baklava1 = Integer.MAX_VALUE;
            try {
                Class<?> codesFull = Class.forName("android.os.Build$VERSION_CODES_FULL");
                Field f2 = codesFull.getDeclaredField("BAKLAVA_1");
                f2.setAccessible(true);
                baklava1 = f2.getInt(null);
            } catch (Throwable ignored) {
            }

            return sdkFull >= 0 && baklava1 != Integer.MAX_VALUE && sdkFull >= baklava1;
        } catch (Throwable t) {
            return false;
        }
    }
}
