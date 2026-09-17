/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.input;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.github.libxposed.api.XposedInterface;

/** Evidence: pandora OS4.0.0.32.XBLCNXM, miui-services.jar; see docs/rear-alipay-gestures.md. */
public final class RearAlipayGestures extends BaseHook {
    public static final String POWER_PREF = "securitycore_power_rear_code_enable";
    public static final String POWER_CODE_PREF = "securitycore_power_rear_code_action";
    public static final String BACK_PREF = "securitycore_back_tap_rear_alipay_enable";
    private static final String DISPATCHER = "com.miui.server.input.util.ShortCutActionsUtils";
    private static final String POWER_RULE = "com.android.server.input.shortcut.singlekeyrule.PowerKeyRule";
    private static boolean sDispatcherInstalled;
    private static boolean sPowerInstalled;
    private final AtomicBoolean mCallbackErrorLogged = new AtomicBoolean();
    private final AtomicLong mLastLog = new AtomicLong(-5000L);

    public static boolean isEnabled() {
        return RearAlipayGesturePolicy.hasEnabledFeature(
            PrefsBridge.getBoolean(POWER_PREF), PrefsBridge.getBoolean(BACK_PREF));
    }

    private static String selectedPowerCode() {
        return PrefsBridge.getString(POWER_CODE_PREF, RearAlipayGesturePolicy.ALIPAY);
    }

    @Override
    public void init() {
        // Do not even resolve OEM classes until the user enables a feature.
        if (!isEnabled()) return;
        if (!BaseLoad.isSystemServer()) {
            XposedLog.w(TAG, "Entry skipped: not system_server");
            return;
        }
        // Only the API 37 implementation has been checked against DEX/smali.
        if (Build.VERSION.SDK_INT != 37) {
            XposedLog.w(TAG, "Target version mismatch: SDK " + Build.VERSION.SDK_INT);
            return;
        }
        synchronized (RearAlipayGestures.class) {
            try {
                Class<?> displayInfo = findClass("miui.util.MiuiMultiDisplayTypeInfo");
                Method rearDevice = displayInfo.getDeclaredMethod("isIndependentRearDevice");
                if (!Boolean.TRUE.equals(rearDevice.invoke(null))) {
                    XposedLog.w(TAG, "Target version/device mismatch: no independent rear display");
                    return;
                }
                Class<?> dispatcher = findClass(DISPATCHER);
                Method trigger = dispatcher.getDeclaredMethod("triggerFunction", String.class, String.class,
                    Bundle.class, boolean.class, String.class);
                requireBoolean(trigger);
                Method currentUser = findClass("miui.securityspace.CrossUserUtils").getDeclaredMethod("getCurrentUserId");
                if (currentUser.getReturnType() != int.class) throw new NoSuchMethodException("getCurrentUserId return type");
                if (!sDispatcherInstalled) {
                    if (chain(trigger, chain -> dispatch(chain, currentUser)) == null) {
                        throw new IllegalStateException("triggerFunction hook returned no handle");
                    }
                    sDispatcherInstalled = true;
                    XposedLog.i(TAG, "Installed triggerFunction(String,String,Bundle,boolean,String); 4-argument delegate left intact");
                }
                if (PrefsBridge.getBoolean(POWER_PREF) && !sPowerInstalled) installPowerHook(currentUser);
            } catch (ReflectiveOperationException error) {
                XposedLog.w(TAG, "Target member not found / version mismatch", error);
            } catch (Throwable error) {
                XposedLog.w(TAG, "Hook installation failed", error);
            }
        }
    }

    private void installPowerHook(Method currentUser) throws ReflectiveOperationException {
        Class<?> rule = findClass(POWER_RULE);
        Method doubleClick = rule.getDeclaredMethod("triggerDoubleClick");
        requireBoolean(doubleClick);
        Method function = rule.getMethod("getFunction", String.class);
        Field quickCode = rule.getDeclaredField("mQuickShowCodeFunction");
        quickCode.setAccessible(true);
        Method execute = quickCode.getType().getDeclaredMethod("executeQuickShowCodeFunction", String.class, String.class);
        requireBoolean(execute);
        execute.setAccessible(true);
        if (chain(doubleClick, chain -> {
            String configuredFunction = null;
            int requestingUser = -1;
            try {
                configuredFunction = (String) function.invoke(chain.getThisObject(), RearAlipayGesturePolicy.POWER);
                requestingUser = (int) currentUser.invoke(null);
            } catch (Throwable error) {
                callbackFailure(error);
            }
            final String originalFunction = configuredFunction;
            final int userId = requestingUser;
            return RearAlipayGesturePolicy.afterOriginal(chain::proceed, result -> {
                if (!RearAlipayGesturePolicy.shouldAppend(PrefsBridge.getBoolean(POWER_PREF),
                    originalFunction, Boolean.TRUE.equals(result)) || userId < 0) return;
                if ((int) currentUser.invoke(null) != userId) return;
                String code = selectedPowerCode();
                if (!RearAlipayGesturePolicy.isPowerCode(code)) return;
                // The native executor retains its lock, unlock listener, handler and pending-action lifetime.
                // Carry the requesting user across the asynchronous unlock boundary; normalize before dispatch.
                boolean queued = Boolean.TRUE.equals(execute.invoke(quickCode.get(chain.getThisObject()),
                    RearAlipayGesturePolicy.powerRequest(userId), code));
                logLimited("Wallet gesture accepted; native rear code " + code + " queued=" + queued);
            }, this::callbackFailure);
        }) == null) throw new IllegalStateException("triggerDoubleClick hook returned no handle");
        sPowerInstalled = true;
        XposedLog.i(TAG, "Installed PowerKeyRule.triggerDoubleClick; native unlock executor retained");
    }

    private Object dispatch(XposedInterface.Chain chain, Method currentUser) throws Throwable {
        String function = (String) chain.getArg(0);
        String action = (String) chain.getArg(1);
        boolean powerRequest = RearAlipayGesturePolicy.isPowerRequest(action);
        Object[] args = null;
        try {
            if (powerRequest && !RearAlipayGesturePolicy.allowPowerDispatch(PrefsBridge.getBoolean(POWER_PREF),
                function, selectedPowerCode(), action, (int) currentUser.invoke(null))) {
                logLimited("Discarded disabled or stale rear payment request");
                return false;
            }
            boolean rearBackTap = RearAlipayGesturePolicy.useRearForBackTap(PrefsBridge.getBoolean(BACK_PREF), function, action);
            if (powerRequest || rearBackTap) {
                // Never mutate the caller's bundle (it may belong to an OEM pending runnable).
                Bundle original = (Bundle) chain.getArg(2);
                Bundle extras = original == null ? new Bundle() : new Bundle(original);
                extras.putInt("show_code_display", 1); // OEM selector, NOT a physical Android display ID.
                args = chain.getArgs().toArray();
                args[0] = powerRequest ? function : RearAlipayGesturePolicy.backTapFunction(function);
                args[2] = extras;
                if (powerRequest) {
                    args[1] = RearAlipayGesturePolicy.POWER;
                    args[3] = false; // Wallet already supplied gesture feedback.
                }
                logLimited("Native rear payment dispatch: " + args[1]);
            }
        } catch (Throwable error) {
            callbackFailure(error);
            if (powerRequest) return false;
            args = null;
        }
        // Preserve native return values and exceptions; do not retry a native call after failure.
        return args == null ? chain.proceed() : chain.proceed(args);
    }

    private static void requireBoolean(Method method) throws NoSuchMethodException {
        if (method.getReturnType() != boolean.class) throw new NoSuchMethodException(method + " return type");
    }

    private void callbackFailure(Throwable error) {
        if (mCallbackErrorLogged.compareAndSet(false, true)) XposedLog.w(TAG, "Hook callback failed (further errors suppressed)", error);
    }

    private void logLimited(String message) {
        long now = SystemClock.uptimeMillis();
        long last = mLastLog.get();
        if (now - last >= 5000L && mLastLog.compareAndSet(last, now)) XposedLog.i(TAG, message);
    }
}
