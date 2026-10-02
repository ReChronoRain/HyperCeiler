/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.Application.isModuleActivated;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public final class XposedActivateHelper {
    // A missing asynchronous Binder callback at two seconds is not proof of inactivity.
    private static final long SERVICE_BIND_TIMEOUT_MS = 10_000L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static WeakReference<Activity> warnedActivityRef = new WeakReference<>(null);
    private static Runnable pendingCheck;
    private static fan.appcompat.app.AlertDialog activationDialog;

    private XposedActivateHelper() {}

    public static void init(Context context) {
        if (!(context instanceof Activity activity)) return;
        onMain(() -> {
            if (activityRef.get() != activity) cancelCheckAndDialog();
            activityRef = new WeakReference<>(activity);
            updateActivateState();
        });
    }

    public static void clear(Activity activity) {
        onMain(() -> {
            if (activityRef.get() != activity) return;
            cancelCheckAndDialog();
            activityRef.clear();
        });
    }

    // Called for real service bind/death events, never from a persisted status cache.
    public static void onActivationChanged() {
        onMain(XposedActivateHelper::updateActivateState);
    }

    public static boolean isActive() {
        return isModuleActivated;
    }

    private static void updateActivateState() {
        if (isActive()) {
            cancelCheckAndDialog();
            warnedActivityRef.clear();
            return;
        }
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()
            || warnedActivityRef.get() == activity || pendingCheck != null) return;
        WeakReference<Activity> target = new WeakReference<>(activity);
        pendingCheck = () -> {
            pendingCheck = null;
            Activity current = target.get();
            if (current == null || current != activityRef.get()
                || current.isFinishing() || current.isDestroyed() || isActive()) return;
            warnedActivityRef = target;
            activationDialog = DialogHelper.showXposedActivateDialog(current);
        };
        MAIN_HANDLER.postDelayed(pendingCheck, SERVICE_BIND_TIMEOUT_MS);
    }

    private static void cancelCheckAndDialog() {
        if (pendingCheck != null) {
            MAIN_HANDLER.removeCallbacks(pendingCheck);
            pendingCheck = null;
        }
        if (activationDialog != null) {
            activationDialog.dismiss();
            activationDialog = null;
        }
    }

    private static void onMain(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) action.run();
        else MAIN_HANDLER.post(action);
    }
}
