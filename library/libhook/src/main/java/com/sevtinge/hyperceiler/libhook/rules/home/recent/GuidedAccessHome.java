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
package com.sevtinge.hyperceiler.libhook.rules.home.recent;

import android.content.Context;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

public class GuidedAccessHome extends BaseHook {
    private static final String SETTING_KEY_LOCK_APP = "key_lock_app";

    @Override
    public void init() {
        hookPointerEvent();
        hookIsMistakeTouch();
        hookScreenPinTouchResolution();
        hookLandscapeOverviewGestureView();
        hookScreenPinnedHelperStartDirectly();
        hookHomeStartScreenPinningDirectly();
    }

    private void hookPointerEvent() {
        findAndChainMethod("com.miui.home.recents.NavStubView",
            "onPointerEvent",
            chain -> {
                Context context = resolveContext(chain.getThisObject());
                if (getLockApp(context) == -1) return chain.proceed();
                return false;
            },
            MotionEvent.class
        );
    }

    private void hookIsMistakeTouch() {
        findAndChainMethod("com.miui.home.recents.NavStubView", "isMistakeTouch", chain -> {
            Context context = resolveContext(chain.getThisObject());
            if (getLockApp(context) == -1) return chain.proceed();
            return true;
        });
    }

    private void hookScreenPinTouchResolution() {
        try {
            findAndChainMethod("com.miui.home.recents.NavStubView",
                "screenPinTouchResolution",
                chain -> {
                    Context context = resolveContext(chain.getThisObject());
                    if (getLockApp(context) == -1) return chain.proceed();
                    return null;
                },
                MotionEvent.class
            );
        } catch (Throwable e) {
            // Pad variant may not include this method; keep other hooks alive.
        }
    }

    private void hookLandscapeOverviewGestureView() {
        findAndChainMethod("com.miui.home.recents.views.RecentsContainer",
            "showLandscapeOverviewGestureView",
            chain -> {
                Context context = resolveContext(chain.getThisObject());
                if (getLockApp(context) == -1) return chain.proceed();
                return null;
            },
            boolean.class
        );
    }

    private void hookHomeStartScreenPinningDirectly() {
        findAndChainMethod("com.miui.home.recents.SystemUiProxyWrapper",
            "startScreenPinning",
            chain -> {
                if (!PrefsBridge.getBoolean("system_framework_guided_access", false)) {
                    return chain.proceed();
                }
                int taskId = (int) chain.getArg(0);
                if (taskId < 0) return chain.proceed();
                try {
                    Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
                    if (activityTaskManager == null) return chain.proceed();
                    Object service = callStaticMethod(activityTaskManager, "getService");
                    callMethod(service, "startSystemLockTaskMode", taskId);
                    return true;
                } catch (Throwable e) {
                    XposedLog.w(TAG, "home direct startSystemLockTaskMode E: " + e);
                    return chain.proceed();
                }
            },
            int.class
        );
    }

    private void hookScreenPinnedHelperStartDirectly() {
        findAndChainMethod("com.miui.home.recents.ScreenPinnedHelper",
            "startScreenPinning",
            chain -> {
                if (!PrefsBridge.getBoolean("system_framework_guided_access", false)) {
                    return chain.proceed();
                }
                int taskId = (int) chain.getArg(0);
                if (taskId < 0) return chain.proceed();
                try {
                    Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
                    if (activityTaskManager == null) return chain.proceed();
                    Object service = callStaticMethod(activityTaskManager, "getService");
                    callMethod(service, "startSystemLockTaskMode", taskId);
                    XposedLog.d(TAG, "home helper direct startSystemLockTaskMode taskId=" + taskId);
                    return null;
                } catch (Throwable e) {
                    XposedLog.w(TAG, "home helper direct startSystemLockTaskMode E: " + e);
                    return chain.proceed();
                }
            },
            int.class
        );
    }

    private Context resolveContext(Object target) {
        if (target instanceof Context) return (Context) target;
        if (target instanceof View) return ((View) target).getContext();
        if (target == null) return null;
        try {
            Object context = getObjectField(target, "mContext");
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int getLockApp(Context context) {
        if (context == null) return -1;
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP);
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
