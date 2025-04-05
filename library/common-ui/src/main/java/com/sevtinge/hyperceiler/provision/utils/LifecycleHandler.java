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
