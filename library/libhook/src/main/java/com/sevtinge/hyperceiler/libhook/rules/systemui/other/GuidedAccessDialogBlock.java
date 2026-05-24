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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.libxposed.api.XposedInterface;

public class GuidedAccessDialogBlock extends BaseHook {

    @Override
    public void init() {
        Class<?> callbacksClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.CentralSurfacesCommandQueueCallbacks");
        if (callbacksClass == null) return;

        chainAllMethods(callbacksClass, "showScreenPinningRequest", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                int taskId = readIntArg(chain.getArgs().toArray(), 0, -1);
                XposedLog.d(TAG, "block showScreenPinningRequest taskId=" + taskId);
                startSystemLockTaskModeByTaskId(chain.getThisObject(), taskId);
                return null;
            }
        });
    }

    private void startSystemLockTaskModeByTaskId(Object callbacks, int taskId) {
        if (taskId < 0) return;
        Context context = resolveContext(callbacks);
        if (context != null && UiLockApp.getLockApp(context) != -1) {
            return;
        }
        try {
            Class<?> activityTaskManagerClass = findClassIfExists("android.app.ActivityTaskManager");
            if (activityTaskManagerClass == null) return;
            Object service = callStaticMethod(activityTaskManagerClass, "getService");
            callMethod(service, "startSystemLockTaskMode", taskId);
        } catch (Throwable e) {
            XposedLog.w(TAG, "startSystemLockTaskMode from dialog block E: " + e);
        }
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
        try {
            Object context = getObjectField(target, "context");
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        try {
            Object outer = getObjectField(target, "this$0");
            if (outer != null && outer != target) {
                return resolveContext(outer);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int readIntArg(Object[] args, int index, int defValue) {
        if (args == null || index < 0 || index >= args.length) return defValue;
        Object value = args[index];
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        return defValue;
    }
}
