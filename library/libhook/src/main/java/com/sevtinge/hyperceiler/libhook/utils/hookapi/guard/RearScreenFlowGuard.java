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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.guard;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RearScreenFlowGuard {
    private static final String REAR_SCREEN_DETAIL_ACTIVITY = "com.rearScreen.RearScreenDetailActivity";
    private static final AtomicBoolean sLifecycleRegistered = new AtomicBoolean(false);
    private static final Set<String> sActiveActivityClassNames =
        Collections.synchronizedSet(new HashSet<>());

    private RearScreenFlowGuard() {
    }

    public static void ensureActivityTrackerRegistered(Context context) {
        if (context == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        if (!(applicationContext instanceof Application application)) {
            return;
        }
        refreshCurrentActivity(applicationContext);
        if (!sLifecycleRegistered.compareAndSet(false, true)) {
            return;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // No-op.
            }

            @Override
            public void onActivityStarted(Activity activity) {
                sActiveActivityClassNames.add(activity.getClass().getName());
            }

            @Override
            public void onActivityResumed(Activity activity) {
                sActiveActivityClassNames.add(activity.getClass().getName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // No-op.
            }

            @Override
            public void onActivityStopped(Activity activity) {
                sActiveActivityClassNames.remove(activity.getClass().getName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // No-op.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                sActiveActivityClassNames.remove(activity.getClass().getName());
            }
        });
    }

    public static boolean isRearScreenActivityActive() {
        return sActiveActivityClassNames.contains(REAR_SCREEN_DETAIL_ACTIVITY);
    }

    public static boolean isRearScreenActivityActive(Context context) {
        if (isRearScreenActivityActive()) {
            return true;
        }
        if (context == null) {
            return false;
        }
        try {
            ActivityManager activityManager = context.getSystemService(ActivityManager.class);
            if (activityManager == null) {
                return false;
            }
            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
            if (runningTasks == null || runningTasks.isEmpty()) {
                return false;
            }
            ComponentName topActivity = runningTasks.get(0).topActivity;
            return topActivity != null && REAR_SCREEN_DETAIL_ACTIVITY.equals(topActivity.getClassName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void refreshCurrentActivity(Context context) {
        try {
            ActivityManager activityManager = context.getSystemService(ActivityManager.class);
            if (activityManager == null) {
                return;
            }
            List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
            if (runningTasks == null || runningTasks.isEmpty()) {
                return;
            }
            ComponentName topActivity = runningTasks.get(0).topActivity;
            if (topActivity != null) {
                sActiveActivityClassNames.add(topActivity.getClassName());
            }
        } catch (Throwable ignored) {
        }
    }
}
