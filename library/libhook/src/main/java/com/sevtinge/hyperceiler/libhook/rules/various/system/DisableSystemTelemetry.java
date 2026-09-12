/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.various.system;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Method;

public class DisableSystemTelemetry extends BaseHook {
    public static boolean supports(String name) {
        return DisableSystemAds.supports(name) || "com.miui.weather2".equals(name);
    }

    @Override public void init() {
        Class<?> track = findClassIfExists("com.xiaomi.onetrack.OneTrack");
        if (track == null) return;
        IMethodHook stop = new IMethodHook() {
            private boolean logged;
            @Override public void before(HookParam param) {
                param.setResult(null);
                if (!logged) {
                    logged = true;
                    XposedLog.d(TAG, getPackageName(), "Blocked system telemetry event");
                }
            }
        };
        for (Method method : track.getDeclaredMethods()) {
            String name = method.getName();
            if (method.getReturnType() == void.class && (name.startsWith("track") || name.equals("adTrack"))) {
                // Stop before event serialization/queueing. Keep configuration and object contracts.
                hookMethod(method, stop);
            }
        }
    }
}
