package com.sevtinge.hyperceiler.home.task;

import android.content.Context;

import com.sevtinge.hyperceiler.ui.HomePageActivity;

/**
 * 初始化调度中心
 */
public class AppInitializer {

    public static void attach(Context context) {
        AppTaskManager.attach(context);
        TaskRunner.getInstance().start();
    }

    public static void initOnAppCreate(Context context) {
        AppTaskManager.setupAppTasks(context);
        TaskRunner.getInstance().start();
    }

    public static void initOnActivityCreate(HomePageActivity activity) {
        AppTaskManager.setupActivityTasks(activity);
        TaskRunner.getInstance().start();
    }
}
