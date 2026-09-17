/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.various.system;

import android.app.Application;
import android.content.Context;
import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/** Host entry points, before ad executors, listeners and SDK requests are created. */
public class DisableSystemAds extends BaseHook {
    private static final Set<String> PACKAGES = Set.of(
        "com.mi.android.globalFileexplorer", "com.android.providers.downloads.ui",
        "com.xiaomi.mipicks", "com.miui.global.packageinstaller",
        "com.android.thememanager", "com.miui.videoplayer");
    private Class<?> loader;
    private Method videoInit;

    public static boolean supports(String name) { return PACKAGES.contains(name); }

    @Override protected boolean useDexKit() {
        return switch (getPackageName()) {
            case "com.miui.global.packageinstaller", "com.android.thememanager", "com.miui.videoplayer" -> true;
            default -> false;
        };
    }

    @Override protected boolean initDexKit() {
        switch (getPackageName()) {
            case "com.miui.global.packageinstaller" -> loader = optionalMember("GlobalInstallerAdLoader",
                bridge -> bridge.findClass(FindClass.create().matcher(ClassMatcher.create()
                    .usingStrings("GlobalAdLoader", "load native AD, isInit: "))).singleOrNull());
            case "com.android.thememanager" -> loader = optionalMember("GlobalThemeAdManager",
                bridge -> bridge.findClass(FindClass.create().matcher(ClassMatcher.create()
                    .usingStrings("close ad in setting", "miglobaladsdk_thememanager"))).singleOrNull());
            case "com.miui.videoplayer" -> videoInit = optionalMember("GlobalVideoAdInit",
                bridge -> bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings("VideoMediationConfig", "block ad init")
                    .paramCount(0).returnType("void"))).singleOrNull());
            default -> { return true; }
        }
        return true;
    }

    private IMethodHook result(Object value, String kind) {
        return new IMethodHook() {
            private boolean logged;
            @Override public void before(HookParam param) {
                param.setResult(value);
                if (!logged) {
                    logged = true;
                    XposedLog.d(TAG, getPackageName(), "Blocked system ad " + kind);
                }
            }
        };
    }

    private void named(String name, Class<?> returnType, IMethodHook callback, String... methods) {
        Class<?> type = findClassIfExists(name);
        if (type == null) return;
        Set<String> names = Set.of(methods);
        for (Method method : type.getDeclaredMethods()) {
            if (method.getReturnType() == returnType && names.contains(method.getName())) {
                hookMethod(method, callback);
            }
        }
    }

    @Override public void init() {
        IMethodHook stop = result(null, "work");
        IMethodHook disabled = result(false, "eligibility");
        switch (getPackageName()) {
            case "com.mi.android.globalFileexplorer" -> {
                // Missing/frequency-limited ads already return without success callbacks.
                // Do not retain listeners for ads that will never load. Cleanup remains safe.
                named("com.fileexplorer.advert.AdManagerController", void.class, stop,
                    "initAd", "loadAd", "getAdView", "showAd", "reportPV",
                    "addLoadAdListener", "removeLoadAdListener", "release");
            }
            case "com.android.providers.downloads.ui" -> {
                named("com.android.providers.downloads.ui.recommend.mediation.MediationAdManager",
                    void.class, stop, "init", "applicationInit", "loadDetailAd", "loadDownloadedAd",
                    "loadHomepageAd", "registerInitListener", "washPv", "washBindView", "washAdClick");
            }
            case "com.xiaomi.mipicks" -> {
                named("com.xiaomi.market.model.AdSwitch", boolean.class, disabled,
                    "isDownloadRecommendSupported", "isUpdateHistoryGridRecommendSupported",
                    "isUpdateHistoryListRecommendSupported", "isUpdateListRecommendSupported");
                named("com.xiaomi.market.ui.UpdateAppsFragmentPhone$AdmobAdManager", void.class, stop, "preload");
            }
            case "com.miui.global.packageinstaller" -> {
                if (loader == null) return;
                for (Method method : loader.getDeclaredMethods()) {
                    if (!Modifier.isPublic(method.getModifiers())) continue;
                    Class<?>[] args = method.getParameterTypes();
                    boolean initializer = method.getReturnType() == void.class && args.length == 2
                        && args[0] == Context.class && args[1] == String.class;
                    boolean ad = args.length == 0 && method.getReturnType().getName()
                        .equals("com.xiaomi.miglobaladsdk.nativead.api.INativeAd");
                    if (initializer || ad) hookMethod(method, stop);
                }
                // With no initialization, this host's load method returns before allocation.
            }
            case "com.android.thememanager" -> {
                if (loader == null) return;
                for (Method method : loader.getDeclaredMethods()) {
                    Class<?>[] args = method.getParameterTypes();
                    if (args.length != 1 || Modifier.isStatic(method.getModifiers())) continue;
                    boolean initializer = method.getReturnType() == void.class
                        && (args[0] == Context.class || args[0] == Application.class);
                    boolean permission = Modifier.isPublic(method.getModifiers())
                        && method.getReturnType() == boolean.class && args[0] == String.class;
                    if (initializer) hookMethod(method, stop);
                    if (permission) hookMethod(method, disabled);
                }
                // These are ad-load gates; no reward or resource-entitlement result is forged.
            }
            case "com.miui.videoplayer" -> {
                if (videoInit != null) hookMethod(videoInit, stop);
                String pool = "com.miui.video.biz.videoplus.app.adapter.LocalAdMediationPool";
                named(pool, boolean.class, disabled, "getSwitch");
                named(pool, void.class, stop, "loadAd");
            }
            default -> { return; }
        }
    }
}
