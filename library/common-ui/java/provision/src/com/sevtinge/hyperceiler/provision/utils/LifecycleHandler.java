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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.utils;

import android.app.Activity;
import android.util.Log;

import com.sevtinge.hyperceiler.provision.activity.BaseActivity;

import java.util.Deque;
import java.util.LinkedList;

public class LifecycleHandler {

    private Deque<Activity> mActivitieStack = new LinkedList();

    private static volatile LifecycleHandler singleton;

    public static LifecycleHandler getInstance() {
        if (singleton == null) {
            synchronized (LifecycleHandler.class) {
                try {
                    if (singleton == null) {
                        singleton = new LifecycleHandler();
                    }
                } finally {}
            }
        }
        return singleton;
    }

    public Activity getActivity(Class<?> clazz) {
        for (Activity activity : mActivitieStack) {
            Log.d("MyLifecycleHandler", "class is:" + activity.getClass());
            if (activity.getClass().equals(clazz)) {
                Log.d("MyLifecycleHandler", "class is:" + activity.getClass() + " finish");
                return activity;
            }
        }
        return null;
    }

    public void finishActivity(Class<?> clazz) {
        for (Activity activity : mActivitieStack) {
            Log.d("MyLifecycleHandler", "class is:" + activity.getClass());
            if (activity.getClass().equals(clazz)) {
                Log.d("MyLifecycleHandler", "class is:" + activity.getClass() + " finish");
                activity.finish();
                boolean z = activity instanceof BaseActivity;
                return;
            }
        }
    }
}
