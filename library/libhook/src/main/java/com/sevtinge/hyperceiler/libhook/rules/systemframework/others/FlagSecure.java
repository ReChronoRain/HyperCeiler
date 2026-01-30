package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimizeMethods;

import android.hardware.display.DisplayManager;
import android.os.Build;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import io.github.libxposed.api.XposedInterface.BeforeHookCallback;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class FlagSecure {

    private static final String TAG = "FlagSecure";
    public void onLoad(XposedModuleInterface.SystemServerLoadedParam lpparam) {
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
        // OS2.0.300.1.WOCCNXM
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
            // Screenshot
            hookWindowState(classLoader);
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "hook WindowState failed", t);
        }
    }

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
        hook(isSecureLockedMethod, SecureLockedHooker.class);
    }

    private static Field captureSecureLayersField;

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
        hookMethods(screenCaptureClazz, ScreenCaptureHooker.class, "nativeCaptureDisplay");
        hookMethods(screenCaptureClazz, ScreenCaptureHooker.class, "nativeCaptureLayers");
    }

    private void hookDisplayControl(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var displayControlClazz = classLoader.loadClass("com.android.server.display.DisplayControl");
        var method = displayControlClazz.getDeclaredMethod(
            "createVirtualDisplay", String.class, boolean.class);
        hook(method, CreateDisplayHooker.class);
    }

    private void hookVirtualDisplayAdapter(ClassLoader classLoader) throws ClassNotFoundException {
        var displayControlClazz = classLoader.loadClass("com.android.server.display.VirtualDisplayAdapter");
        hookMethods(displayControlClazz, CreateVirtualDisplayLockedHooker.class, "createVirtualDisplayLocked");
    }

    private void hookActivityTaskManagerService(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var activityTaskManagerServiceClazz = classLoader.loadClass("com.android.server.wm.ActivityTaskManagerService");
        var iBinderClazz = classLoader.loadClass("android.os.IBinder");
        var iScreenCaptureObserverClazz = classLoader.loadClass("android.app.IScreenCaptureObserver");
        var method = activityTaskManagerServiceClazz.getDeclaredMethod("registerScreenCaptureObserver", iBinderClazz, iScreenCaptureObserverClazz);
        hook(method, ReturnNullHooker.class);
    }

    private void hookWindowManagerService(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var windowManagerServiceClazz = classLoader.loadClass("com.android.server.wm.WindowManagerService");
        var iScreenRecordingCallbackClazz = classLoader.loadClass("android.window.IScreenRecordingCallback");
        var method = windowManagerServiceClazz.getDeclaredMethod("registerScreenRecordingCallback", iScreenRecordingCallbackClazz);
        hook(method, ReturnFalseHooker.class);
    }

    private void hookActivityManagerService(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var activityTaskManagerServiceClazz = classLoader.loadClass("com.android.server.am.ActivityManagerService");
        var method = activityTaskManagerServiceClazz.getDeclaredMethod("checkPermission", String.class, int.class, int.class);
        hook(method, CheckPermissionHooker.class);
    }

    private void hookHyperOS(ClassLoader classLoader) throws ClassNotFoundException {
        var windowManagerServiceImplClazz = classLoader.loadClass("com.android.server.wm.WindowManagerServiceImpl");
        hookMethods(windowManagerServiceImplClazz, ReturnFalseHooker.class, "notAllowCaptureDisplay");
    }

    private void hookScreenshotHardwareBuffer(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        var screenshotHardwareBufferClazz = classLoader.loadClass(
            "android.window.ScreenCapture$ScreenshotHardwareBuffer");
        var method = screenshotHardwareBufferClazz.getDeclaredMethod("containsSecureLayers");
        hook(method, ReturnFalseHooker.class);
    }

    private void hookMethods(Class<?> clazz, Class<? extends Hooker> hooker, String... names) {
        var list = Arrays.asList(names);
        Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> list.contains(method.getName()))
            .forEach(method -> hook(method, hooker));
    }

    private void hook(Method method, Class<? extends Hooker> hooker) {
        EzxHelpUtils.libHook(method, hooker);
    }

    @XposedHooker
    private static class CreateDisplayHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var stackTrace = new Throwable().getStackTrace();
                for (int i = 4; i < stackTrace.length && i < 8; i++) {
                    var name = stackTrace[i].getMethodName();
                    if (name.equals("createVirtualDisplayLocked")) {
                        return;
                    }
                }
            }
            callback.getArgs()[1] = true;
        }
    }

    @XposedHooker
    private static class CheckPermissionHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            var permission = callback.getArgs()[0];
            if ("android.permission.CAPTURE_BLACKOUT_CONTENT".equals(permission)) {
                callback.getArgs()[0] = "android.permission.READ_FRAME_BUFFER";
            }
        }
    }

    @XposedHooker
    private static class OplusScreenCaptureHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            callback.getArgs()[0] = -1;
        }
    }

    @XposedHooker
    private static class ScreenCaptureHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            var captureArgs = callback.getArgs()[0];
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && isAtLeastBaklava1()) {
                    captureSecureLayersField.set(captureArgs, 1);
                } else {
                    captureSecureLayersField.set(captureArgs, true);
                }
            } catch (IllegalAccessException t) {
                XposedLog.e(TAG, "system", "ScreenCaptureHooker failed", t);
            }
        }
    }

    @XposedHooker
    private static class CreateVirtualDisplayLockedHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            var caller = (int) callback.getArgs()[2];
            if (caller >= 10000 && callback.getArgs()[1] == null) {
                // not os and not media projection
                return;
            }
            for (int i = 3; i < callback.getArgs().length; i++) {
                var arg = callback.getArgs()[i];
                if (arg instanceof Integer) {
                    var flags = (int) arg;
                    flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
                    callback.getArgs()[i] = flags;
                    return;
                }
            }
            XposedLog.e(TAG, "system", "flag not found in CreateVirtualDisplayLockedHooker");
        }
    }

    @XposedHooker
    private static class SecureLockedHooker implements Hooker {

        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            var walker = StackWalker.getInstance();
            var match = walker.walk(frames -> frames
                .map(StackWalker.StackFrame::getMethodName)
                .limit(6)
                .skip(2)
                .anyMatch(s -> s.equals("setInitialSurfaceControlProperties") || s.equals("createSurfaceLocked")));
            if (match) return;
            callback.returnAndSkip(false);
        }
    }

    @XposedHooker
    private static class ReturnTrueHooker implements Hooker {
        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            callback.returnAndSkip(true);
        }
    }

    @XposedHooker
    private static class ReturnFalseHooker implements Hooker {
        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            callback.returnAndSkip(false);
        }
    }

    @XposedHooker
    private static class ReturnNullHooker implements Hooker {
        @BeforeInvocation
        public static void before(@NonNull BeforeHookCallback callback) {
            callback.returnAndSkip(null);
        }
    }

    private static boolean isAtLeastBaklava1() {
        try {
            int sdkFull = -1;
            try {
                java.lang.reflect.Field f = Build.VERSION.class.getDeclaredField("SDK_INT_FULL");
                f.setAccessible(true);
                sdkFull = f.getInt(null);
            } catch (Throwable ignored) {}

            int baklava1 = Integer.MAX_VALUE;
            try {
                Class<?> codesFull = Class.forName("android.os.Build$VERSION_CODES_FULL");
                java.lang.reflect.Field f2 = codesFull.getDeclaredField("BAKLAVA_1");
                f2.setAccessible(true);
                baklava1 = f2.getInt(null);
            } catch (Throwable ignored) {}

            return sdkFull >= 0 && baklava1 != Integer.MAX_VALUE && sdkFull >= baklava1;
        } catch (Throwable t) {
            return false;
        }
    }
}
