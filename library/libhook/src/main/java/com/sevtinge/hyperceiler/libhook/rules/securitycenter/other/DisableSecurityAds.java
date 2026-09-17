/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other;

import android.content.Context;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DisableSecurityAds extends BaseHook {
    private Class<?> globalLoader;

    @Override protected boolean useDexKit() { return true; }

    @Override protected boolean initDexKit() {
        // This loader does not exist on CN. The common card parser is independent.
        globalLoader = optionalMember("SecurityGlobalAdLoader", bridge -> bridge.findClass(FindClass.create()
            .matcher(ClassMatcher.create().usingStrings("GlobalAdLoader", "getNativeAd placeId = "))).singleOrNull());
        return true;
    }

    @Override public void init() {
        IMethodHook block = new IMethodHook() {
            private boolean logged;
            @Override public void before(HookParam param) {
                param.setResult(null);
                if (!logged) {
                    logged = true;
                    XposedLog.d(TAG, getPackageName(), "Blocked Security Center advertisement work");
                }
            }
        };
        Class<?> card = findClassIfExists("com.miui.common.card.models.AdvCardModel");
        if (card != null) {
            for (Method method : card.getDeclaredMethods()) {
                if (method.getName().equals("parse") && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType().getName().equals("com.miui.common.card.models.BaseCardModel")) {
                    // The host parser already uses null to represent an unsupported ad.
                    hookMethod(method, block);
                }
            }
        }
        if (globalLoader == null) return;
        for (Method method : globalLoader.getDeclaredMethods()) {
            Class<?>[] args = method.getParameterTypes();
            boolean initializer = method.getReturnType() == void.class && args.length == 2
                && args[0] == Context.class && args[1] == String.class;
            boolean request = method.getReturnType() == void.class && args.length == 2
                && args[0] == String.class && args[1] == boolean.class;
            boolean lookup = args.length == 1 && args[0] == String.class
                && method.getReturnType().getName().equals("com.xiaomi.miglobaladsdk.nativead.api.INativeAd");
            if (Modifier.isPublic(method.getModifiers()) && (initializer || request || lookup)) {
                // Stop before the initializer starts its thread or a request is queued.
                hookMethod(method, block);
            }
        }
    }
}
