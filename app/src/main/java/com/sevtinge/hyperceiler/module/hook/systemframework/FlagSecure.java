package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.os.Build;
import android.util.Log;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

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
            } catch (Throwable t) {
                XposedBridge.log(t);
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
                XposedLogUtils.logE(TAG, this.lpparam.packageName, t);
            }
        }
    }
}
