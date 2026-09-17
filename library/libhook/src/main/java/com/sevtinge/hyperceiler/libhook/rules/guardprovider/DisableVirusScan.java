/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */
package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Prevents the MIUI package installer scan from reaching MiEngine/Tencent engines.
 * The public service is obfuscated, but the Binder implementation keeps stable
 * signatures in OS3: SecurityService$a.S(...) and SecurityService$a.k(...).
 */
public class DisableVirusScan extends BaseHook {
    private static final String SERVICE_IMPL =
        "com.miui.guardprovider.manager.SecurityService$a";
    private static final String SCAN_MANAGER = "C1.g";

    @Override
    public void init() {
        XposedLog.i(TAG, getPackageName(), "DisableVirusScan init");
        Class<?> callback = findClassIfExists("h1.c");
        if (callback == null) {
            XposedLog.w(TAG, getPackageName(), "Virus scan callback class not found");
            return;
        }

        IMethodHook bypass = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                XposedLog.i(TAG, getPackageName(), "DisableVirusScan bypass scan");
                Object observer = param.getArgs().length > 1 ? param.getArgs()[1] : null;
                if (observer != null) {
                    try {
                        Object empty = Array.newInstance(findClass("h1.f"), 0);
                        Method finish = callback.getMethod("b0", int.class, empty.getClass());
                        // AntivirusManager uses -1 when no engine list is
                        // available; status 0 is interpreted as an active
                        // scan and leaves the installer's spinner running.
                        finish.invoke(observer, -1, empty);
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), "Failed to complete skipped virus scan", t);
                    }
                }
                param.setResult(-1);
            }
        };

        Class<?> impl = findClassIfExists(SERVICE_IMPL);
        if (impl != null) {
            findAndHookMethod(impl, "S", String[].class, callback, boolean.class, bypass);
            findAndHookMethod(impl, "k", String[].class, callback, boolean.class, int.class, bypass);
        } else {
            XposedLog.w(TAG, getPackageName(), "SecurityService implementation not found");
        }

        // SecurityService delegates the real work to C1.g.K.  Hooking this
        // concrete manager as well covers builds where the Binder stub is
        // created before the module class is resolved.
        Class<?> manager = findClassIfExists(SCAN_MANAGER);
        if (manager != null) {
            findAndHookMethod(manager, "K", String[].class, callback, boolean.class,
                int.class, String.class, bypass);
        }
    }
}
