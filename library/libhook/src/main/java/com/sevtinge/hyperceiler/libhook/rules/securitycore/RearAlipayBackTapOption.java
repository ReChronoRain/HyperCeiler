/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycore;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.input.RearAlipayGestures;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.input.RearAlipayGesturePolicy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** SecurityCore 4.4.3.3-4-260814 (40004433): f2.t.e supplies both labels and persisted action values. */
public final class RearAlipayBackTapOption extends BaseHook {
    private static final String PACKAGE = "com.miui.securitycore";
    private static boolean sInstalled;
    private final AtomicBoolean mFailureLogged = new AtomicBoolean();
    private Boolean mVersionSupported;

    @Override
    public void init() {
        if (!PrefsBridge.getBoolean(RearAlipayGestures.BACK_PREF)) return;
        if (!PACKAGE.equals(getPackageName()) || !PACKAGE.equals(Application.getProcessName())) {
            XposedLog.i(TAG, "Entry skipped: not the SecurityCore main process");
            return;
        }
        if (Build.VERSION.SDK_INT != 37) {
            XposedLog.w(TAG, "Target version mismatch: requires SDK 37");
            return;
        }
        synchronized (RearAlipayBackTapOption.class) {
            if (sInstalled) return;
            try {
                if (!Boolean.TRUE.equals(findClass("miui.util.MiuiMultiDisplayTypeInfo")
                    .getDeclaredMethod("isIndependentRearDevice").invoke(null))) return;
                Class<?> feature = findClass("f2.t");
                Method choices = feature.getDeclaredMethod("e", String[].class, int.class);
                if (choices.getReturnType() != LinkedHashMap.class) throw new NoSuchMethodException("f2.t.e return type");
                Field contextField = feature.getDeclaredField("a");
                contextField.setAccessible(true);
                if (chain(choices, chain -> {
                    Object original = chain.proceed();
                    if (!PrefsBridge.getBoolean(RearAlipayGestures.BACK_PREF)) return original;
                    try {
                        String[] actions = (String[]) chain.getArg(0);
                        if ((int) chain.getArg(1) != 2 || actions == null || actions.length != 1
                            || !RearAlipayGesturePolicy.isBackTap(actions[0])
                            || !(original instanceof LinkedHashMap<?, ?> map)
                            || !map.containsKey(RearAlipayGesturePolicy.ALIPAY)) return original;
                        Context context = (Context) contextField.get(chain.getThisObject());
                        if (mVersionSupported == null) {
                            mVersionSupported = context.getPackageManager().getPackageInfo(PACKAGE, 0).getLongVersionCode() == 40004433L;
                            if (!mVersionSupported) XposedLog.w(TAG, "Target version mismatch: SecurityCore is not 40004433");
                        }
                        if (!mVersionSupported) return original;
                        String label = getModuleRes(context).getString(R.string.rear_alipay_back_native_entry);
                        String transitLabel = getModuleRes(context).getString(R.string.rear_alipay_back_native_transit_entry);
                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, String> nativeChoices = (LinkedHashMap<String, String>) map;
                        return RearAlipayGesturePolicy.addRearChoices(nativeChoices, actions, 2, label, transitLabel);
                    } catch (Throwable error) {
                        if (mFailureLogged.compareAndSet(false, true)) XposedLog.w(TAG, "Native option callback failed", error);
                        return original;
                    }
                }) == null) throw new IllegalStateException("Native choices hook returned no handle");
                sInstalled = true;
                XposedLog.i(TAG, "Installed f2.t.e(String[],int): native back-tap rear payment and transit options");
            } catch (ReflectiveOperationException error) {
                XposedLog.w(TAG, "Target member not found / version mismatch", error);
            } catch (Throwable error) {
                XposedLog.w(TAG, "Native option installation failed", error);
            }
        }
    }
}
