/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class RedirectBgmRecognition extends BaseHook {
    public static final int ROUTE_SECURITY_CENTER_INSTRUMENTATION = 0;
    public static final int ROUTE_SECURITY_CENTER_BINDER = 1;

    public static final String PREF_ENABLED =
        "security_center_sidebar_bgm_enable";
    private static final String PREF_TARGET_ACTIVITY =
        "security_center_sidebar_bgm_target_activity";
    public static final String PREF_HOOK_ROUTE =
        "security_center_sidebar_bgm_hook_route";
    private final boolean hookInstrumentation;
    private final boolean hookActivityTaskManager;

    public RedirectBgmRecognition(
        boolean hookInstrumentation,
        boolean hookActivityTaskManager
    ) {
        this.hookInstrumentation = hookInstrumentation;
        this.hookActivityTaskManager = hookActivityTaskManager;
    }

    @Override
    public void init() {
        IMethodHook redirectHook = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                redirectIntentArguments(param);
            }
        };

        int instrumentationHooks = 0;
        int binderHooks = 0;
        if (hookInstrumentation) {
            List<?> hooks = hookAllMethods(
                Instrumentation.class, "execStartActivity", redirectHook);
            instrumentationHooks = hooks.size();
        }
        if (hookActivityTaskManager) {
            binderHooks = hookActivityTaskManagerBinder(redirectHook);
        }

        XposedLog.i(TAG, getPackageName(),
            "BGM redirect initialized: instrumentationHooks=" + instrumentationHooks
                + ", activityTaskManagerHooks=" + binderHooks
                + ", target=" + PrefsBridge.getString(PREF_TARGET_ACTIVITY, ""));
    }

    private int hookActivityTaskManagerBinder(IMethodHook redirectHook) {
        Class<?> proxyClass = findClassIfExists(
            "android.app.IActivityTaskManager$Stub$Proxy", getClassLoader());
        if (proxyClass == null) {
            XposedLog.w(TAG, getPackageName(),
                "IActivityTaskManager proxy not found; direct Binder activity starts will not be intercepted");
            return 0;
        }

        int hooks = 0;
        for (Method method : proxyClass.getDeclaredMethods()) {
            if (method.getName().startsWith("startActivity")) {
                hookMethod(method, redirectHook);
                hooks++;
            }
        }
        if (hooks == 0) {
            XposedLog.w(TAG, getPackageName(),
                "IActivityTaskManager proxy has no startActivity methods");
        }
        return hooks;
    }

    private void redirectIntentArguments(HookParam param) {
        Object[] args = param.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof Intent intent) || !isRadarRecognitionRequest(intent)) {
                continue;
            }

            XposedLog.i(TAG, getPackageName(),
                "Observed BGM radar request: data=" + intent.getData());
            String target = PrefsBridge.getString(PREF_TARGET_ACTIVITY, "");
            if (target == null || TextUtils.isEmpty(target.trim())) {
                return;
            }

            ComponentName targetComponent = ComponentName.unflattenFromString(target.trim());
            if (targetComponent == null) {
                XposedLog.w(TAG, getPackageName(),
                    "Invalid BGM recognition target component: " + target);
                return;
            }

            // 替换待处理的参数后继续原 Instrumentation/Binder 启动流程，不额外发起第二次启动。
            // 仅保留抓取到的 NEW_TASK 标志；丢弃小米音乐 URI 和其它原始数据。
            Intent redirectedIntent = new Intent().setComponent(targetComponent);
            redirectedIntent.addFlags(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
            args[i] = redirectedIntent;
            XposedLog.i(TAG, getPackageName(),
                "Redirect BGM recognition request to "
                    + targetComponent.flattenToShortString());
            return;
        }
    }

    private static boolean isRadarRecognitionRequest(Intent intent) {
        // 抓取到的调用链：安全中心发送 ACTION_VIEW miui-music://radar/...，
        // 系统将其交给 MusicDispatchActivity，再由小米音乐进入内部识曲流程。
        if (!Intent.ACTION_VIEW.equals(intent.getAction())) {
            return false;
        }

        Uri data = intent.getData();
        return data != null
            && "miui-music".equalsIgnoreCase(data.getScheme())
            && "radar".equalsIgnoreCase(data.getHost());
    }
}
