package com.sevtinge.hyperceiler.module.hook.home.gesture;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class QuickBack extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.GestureStubView",
            "isDisableQuickSwitch", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.GestureStubView",
            "getNextTask", Context.class, boolean.class, int.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context context = (Context) param.args[0];
                    ActivityManager.RunningTaskInfo runningTask;
                    Object recentsModel = XposedHelpers.callStaticMethod(findClass("com.miui.home.recents.RecentsModel"), "getInstance", context);
                    Object taskLoader = XposedHelpers.callMethod(recentsModel, "getTaskLoader");
                    Object createLoadPlan = XposedHelpers.callMethod(taskLoader, "createLoadPlan", context);
                    XposedHelpers.callMethod(taskLoader, "preloadTasks", createLoadPlan, -1);
                    Object taskStack = XposedHelpers.callMethod(createLoadPlan, "getTaskStack");
                    ActivityOptions activityOptions = null;
                    if (taskStack == null || (int) XposedHelpers.callMethod(taskStack, "getTaskCount") == 0 || (runningTask = (ActivityManager.RunningTaskInfo) XposedHelpers.callMethod(recentsModel, "getRunningTask")) == null) {
                        param.setResult(null);
                        return;
                    }
                    ArrayList<?> stackTasks = (ArrayList<?>) XposedHelpers.callMethod(taskStack, "getStackTasks");
                    int size = stackTasks.size();
                    Object task = null;
                    int i2 = 0;
                    while (true) {
                        if (i2 >= size - 1) {
                            break;
                        } else if ((int) XposedHelpers.getObjectField(XposedHelpers.getObjectField(stackTasks.get(i2), "key"), "id") == runningTask.id) {
                            task = stackTasks.get(i2 + 1);
                            break;
                        } else {
                            i2++;
                        }
                    }
                    if (task == null && size >= 1 && "com.miui.home".equals(runningTask.baseActivity.getPackageName())) {
                        task = stackTasks.get(0);
                    }
                    if (task != null && XposedHelpers.getObjectField(task, "icon") == null) {
                        XposedHelpers.setObjectField(task, "icon", XposedHelpers.callMethod(taskLoader, "getAndUpdateActivityIcon",
                            XposedHelpers.getObjectField(task, "key"),
                            XposedHelpers.getObjectField(task, "taskDescription"),
                            context.getResources(), true
                        ));
                    }
                    if (!(boolean) param.args[1] || task == null) {
                        param.setResult(task);
                        return;
                    }
                    int i = (int) param.args[2];
                    if (i == 0) {
                        activityOptions = ActivityOptions.makeCustomAnimation(context,
                            getAnimId(context, "recents_quick_switch_left_enter"),
                            getAnimId(context, "recents_quick_switch_left_exit"));
                    } else if (i == 1) {
                        activityOptions = ActivityOptions.makeCustomAnimation(context,
                            getAnimId(context, "recents_quick_switch_right_enter"),
                            getAnimId(context, "recents_quick_switch_right_exit"));
                    }
                    Object iActivityManager = XposedHelpers.callStaticMethod(findClass("android.app.ActivityManagerNative"), "getDefault");
                    if (iActivityManager != null) {
                        try {
                            if ((int) XposedHelpers.getObjectField(XposedHelpers.getObjectField(task, "key"), "windowingMode") == 3) {
                                if (activityOptions == null) {
                                    activityOptions = ActivityOptions.makeBasic();
                                }
                                activityOptions.getClass().getMethod("setLaunchWindowingMode", Integer.class).invoke(activityOptions, 4);
                            }
                            XposedHelpers.callMethod(iActivityManager, "startActivityFromRecents",
                                XposedHelpers.getObjectField(
                                    XposedHelpers.getObjectField(task, "key"),
                                    "id"),
                                activityOptions == null ? null : activityOptions.toBundle());
                            param.setResult(task);
                            return;
                        } catch (Exception e) {
                            logE(TAG, "Error: " + e);
                            param.setResult(task);
                            return;
                        }
                    }
                    param.setResult(task);
                }
            }
        );

    }

    public static int getAnimId(Context context, String str) {
        return context.getResources().getIdentifier(str, "anim", context.getPackageName());
    }
}
