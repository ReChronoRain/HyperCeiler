/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.downloads;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

/** Keep ordinary HTTP downloads on the platform worker without starting
 * Download Provider's optional Xunlei engine. */
public final class DisableXunleiEngine extends BaseHook {
    private static final String PROVIDER = "com.android.providers.downloads";
    private static final String UI = "com.android.providers.downloads.ui";

    @Override
    public void init() {
        int installed;
        if (PROVIDER.equals(getPackageName())) {
            installed = hookProvider(getClassLoader());
        } else if (UI.equals(getPackageName())) {
            installed = hookUi(getClassLoader());
        } else {
            return;
        }
        XposedLog.i("DisableXunleiEngine", getPackageName(),
                "Installed " + installed + " Xunlei hooks");
    }

    private int hookProvider(ClassLoader loader) {
        int installed = 0;
        Class<?> engine = lookupClass("com.android.providers.downloads.XLDownloadApplication", loader);
        if (engine != null) {
            for (Method method : engine.getDeclaredMethods()) {
                if ("initXunleiEngine".equals(method.getName())
                        && method.getParameterCount() == 0
                        && method.getReturnType() == Void.TYPE) {
                    BaseHook.chain(method, chain -> null);
                    installed++;
                }
            }
        }

        Class<?> privacy = lookupClass(
                "com.android.providers.downloads.setting.PrivacySettingHelper", loader);
        installed += forceNoArgBoolean(privacy, "isXunleiUsageOpen", false);

        Class<?> info = lookupClass("com.android.providers.downloads.service.DownloadInfo", loader);
        if (info != null) {
            for (Method method : info.getDeclaredMethods()) {
                if (!"getDownloadThread".equals(method.getName())
                        || method.getParameterCount() != 0
                        || method.getReturnType() != Runnable.class) {
                    continue;
                }
                BaseHook.chain(method, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object downloadInfo = chain.getThisObject();
                        Object uri = getField(downloadInfo, "mUri");
                        if (uri instanceof String url
                                && (url.startsWith("http://") || url.startsWith("https://"))) {
                            Method aosp = findMethod(downloadInfo.getClass(),
                                    "getAospDownloadThread");
                            if (aosp != null && aosp.getReturnType() == Runnable.class) {
                                Runnable task = (Runnable) aosp.invoke(downloadInfo);
                                if (task != null) {
                                    setField(downloadInfo, "mTask", task);
                                    return task;
                                }
                            }
                        }
                        return chain.proceed();
                    }
                });
                installed++;
            }
        }
        return installed;
    }

    private int hookUi(ClassLoader loader) {
        int installed = 0;
        Class<?> preferences = lookupClass(
                "com.android.thunderfoundation.component.utils.SharePreferenceHelper", loader);
        installed += forceNoArgBoolean(preferences, "isDoubleEngineSupport", false);
        if (preferences != null) {
            for (Method method : preferences.getDeclaredMethods()) {
                if ("saveDoubleEngineSupport".equals(method.getName())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == Boolean.TYPE
                        && method.getReturnType() == Void.TYPE) {
                    BaseHook.chain(method, chain -> chain.proceed(new Object[]{Boolean.FALSE}));
                    installed++;
                }
            }
        }

        Class<?> settings = lookupClass(
                "com.android.providers.downloads.remote.service.DownloadSettingsProviderProxy", loader);
        if (settings != null) {
            for (Method method : settings.getDeclaredMethods()) {
                if ("getXunleiUsagePermission".equals(method.getName())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == Boolean.TYPE
                        && method.getReturnType() == Boolean.TYPE) {
                    BaseHook.chain(method, chain -> Boolean.FALSE);
                    installed++;
                }
            }
        }
        return installed;
    }

    private int forceNoArgBoolean(Class<?> type, String name, boolean value) {
        if (type == null) return 0;
        int installed = 0;
        for (Method method : type.getDeclaredMethods()) {
            if (name.equals(method.getName())
                    && method.getParameterCount() == 0
                    && method.getReturnType() == Boolean.TYPE) {
                BaseHook.chain(method, chain -> value);
                installed++;
            }
        }
        return installed;
    }

    private Class<?> lookupClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Method findMethod(Class<?> type, String name) {
        try {
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Object getField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
