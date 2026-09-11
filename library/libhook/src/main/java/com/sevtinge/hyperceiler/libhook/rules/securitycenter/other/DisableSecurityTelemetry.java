/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Method;

public class DisableSecurityTelemetry extends BaseHook {
    @Override public void init() {
        IMethodHook block = new IMethodHook() {
            private boolean logged;
            @Override public void before(HookParam param) {
                param.setResult(null);
                if (!logged) {
                    logged = true;
                    XposedLog.d(TAG, getPackageName(), "Blocked Security Center usage telemetry");
                }
            }
        };
        for (String className : new String[]{"com.miui.analytics.AnalyticsUtil", "com.miui.analytics.StatManager"}) {
            Class<?> type = findClassIfExists(className);
            if (type == null) continue;
            for (Method method : type.getDeclaredMethods()) {
                if (method.getReturnType() != void.class) continue;
                String name = method.getName();
                if (name.startsWith("track") || name.startsWith("record") || name.equals("adTrack")
                    || name.equals("initMiStats") || name.equals("initSdkInternal")
                    || name.equals("initOneTrackGlobalSdk") || name.equals("initNetworkAssistantOneTrackGl")
                    || name.equals("setAccessNetworkEnable") || name.equals("setDataUploadingEnabled")
                    || name.equals("triggerUpload")) {
                    hookMethod(method, block);
                }
            }
        }
        // StatManager's Global remote-config getters remain available: their lazy
        // initSdk path is also used for functional configuration, not only reports.
    }
}
