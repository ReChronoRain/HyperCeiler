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

public class XposedActivateHelper {
    private static final long SERVICE_BIND_TIMEOUT_MS = 2000L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());


    public static void init(Context context) {
        if (isActive()) return;

        if (context instanceof Activity activity) {
            WeakReference<Activity> activityRef = new WeakReference<>(activity);
            MAIN_HANDLER.postDelayed(() -> {
                Activity current = activityRef.get();
                if (current == null || current.isFinishing() || current.isDestroyed()) return;
                checkActivateState(current);
            }, SERVICE_BIND_TIMEOUT_MS);
            return;
        }

        checkActivateState(context);
    }

    public static boolean isActive() {
        return isModuleActivated;
    }

    private static void checkActivateState(Context context) {
        if (!isActive()) DialogHelper.showXposedActivateDialog(context);
    }
}
