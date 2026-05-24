/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.powerkeeper;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.deoptimize;

import android.os.Bundle;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * Source:
 * https://github.com/Howard20181/HyperOS_FCM_Live/blob/main/HyperFCMLive/src/main/java/io/github/howard20181/hyperos/fcmlive/Hooker.java
 */
public class GmsDozeFix extends BaseHook {
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";

    private static final String[] GMS_OBSERVER_METHODS = new String[]{
        "updateGmsNetWork",
        "updateGmsAlarm",
        "updateGoogleReletivesWakelock"
    };

    private static final XposedInterface.Hooker FORCE_FALSE_HOOKER = chain -> {
        Object[] args = chain.getArgs().toArray();
        if (args.length > 0) {
            args[0] = false;
        }
        return chain.proceed(args);
    };

    @Override
    public void init() {
        hookGmsObserver();
        hookGlobalFeatureConfigureHelper();
    }

    private void hookGmsObserver() {
        Class<?> netdExecutorClass = findClassIfExists("com.miui.powerkeeper.utils.NetdExecutor");
        if (netdExecutorClass != null) {
            try {
                Method initGmsChainMethod = netdExecutorClass.getDeclaredMethod(
                    "initGmsChain", String.class, int.class, String.class);
                chain(initGmsChainMethod, chain -> {
                    Object[] args = chain.getArgs().toArray();
                    args[2] = "ACCEPT";
                    return chain.proceed(args);
                });
                deoptimize(initGmsChainMethod);
            } catch (NoSuchMethodException e) {
                XposedLog.w(TAG, getPackageName(), "Skip missing method: NetdExecutor#initGmsChain", e);
            } catch (Throwable t) {
                XposedLog.e(TAG, getPackageName(), "Hook failed in initGmsChain", t);
            }
        }

        Class<?> gmsObserverClass = findClassIfExists("com.miui.powerkeeper.utils.GmsObserver");
        if (gmsObserverClass == null) return;

        for (String methodName : GMS_OBSERVER_METHODS) {
            hookObserverMethod(gmsObserverClass, methodName);
        }
    }

    @SuppressWarnings("unchecked")
    private void hookGlobalFeatureConfigureHelper() {
        Class<?> helperClass = findClassIfExists(
            "com.miui.powerkeeper.provider.GlobalFeatureConfigureHelper");
        if (helperClass == null) return;

        try {
            Method getDozeWhiteListAppsMethod = helperClass.getDeclaredMethod(
                "getDozeWhiteListApps", Bundle.class);
            chain(getDozeWhiteListAppsMethod, chain -> {
                Object result = chain.proceed();
                if (result instanceof List<?>) {
                    List<String> whiteList = (List<String>) result;
                    if (!whiteList.contains(GMS_PACKAGE_NAME)) {
                        whiteList.add(GMS_PACKAGE_NAME);
                    }
                }
                return result;
            });
        } catch (NoSuchMethodException e) {
            XposedLog.w(TAG, getPackageName(),
                "Skip missing method: GlobalFeatureConfigureHelper#getDozeWhiteListApps", e);
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Hook failed in getDozeWhiteListApps", t);
        }
    }

    private void hookObserverMethod(Class<?> observerClass, String methodName) {
        try {
            Method method = observerClass.getDeclaredMethod(methodName, boolean.class);
            chain(method, FORCE_FALSE_HOOKER);
            deoptimize(method);
        } catch (NoSuchMethodException e) {
            XposedLog.w(TAG, getPackageName(), "Skip missing method in GmsObserver: " + methodName, e);
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Hook failed in " + methodName, t);
        }
    }
}
