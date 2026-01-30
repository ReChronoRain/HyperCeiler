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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import static com.sevtinge.hyperceiler.libhook.utils.api.InvokeUtils.getStaticField;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 启用桌面快捷返回功能
 *
 * @noinspection DataFlowIssue
 */
public class QuickBack extends BaseHook {

    private static final Map<String, Integer> sAnimResCache = new HashMap<>();
    private static int[] sReadyStateValues = null;

    private static final int STATE_BACK = 1;
    private static final int STATE_RECENT = 2;
    private static final int STATE_NONE = 3;

    @Override
    public void init() {
        initReadyStateValues();hookDisableQuickSwitch();
        hookOnSwipeStop();
        hookGetNextTask();
    }

    private void initReadyStateValues() {
        try {
            Class<?> readyStateClass = findClass("com.miui.home.recents.GestureBackArrowView$ReadyState");
            Object[] enumValues = (Object[]) callStaticMethod(readyStateClass, "values");

            sReadyStateValues = new int[enumValues.length];
            sReadyStateValues[getEnumOrdinal(readyStateClass, "READY_STATE_BACK")] = STATE_BACK;
            sReadyStateValues[getEnumOrdinal(readyStateClass, "READY_STATE_RECENT")] = STATE_RECENT;
            sReadyStateValues[getEnumOrdinal(readyStateClass, "READY_STATE_NONE")] = STATE_NONE;
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "initReadyStateValues failed", e);
        }
    }

    private int getEnumOrdinal(Class<?> enumClass, String name) {
        Enum<?> enumValue = getStaticField(enumClass, name);
        return enumValue.ordinal();
    }

    private void hookDisableQuickSwitch() {
        findAndHookMethod("com.miui.home.recents.GestureStubView","isDisableQuickSwitch",
            returnConstant(false));
    }

    private void hookOnSwipeStop() {
        findAndHookMethod("com.miui.home.recents.GestureStubView$3",
            "onSwipeStop", boolean.class, float.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    boolean isFinish = (boolean) param.getArgs()[0];
                    if (!isFinish) return;

                    int stateOrdinal = getCurrentStateOrdinal(param.getThisObject());
                    int state = mapOrdinalToState(stateOrdinal);

                    if (state == STATE_RECENT) {
                        Object gestureStubView = getObjectField(param.getThisObject(), "this$0");
                        callMethod(gestureStubView, "onBackCancelled");
                        XposedLog.i(TAG, getPackageName(), "call onBackCancelled");
                    }
                }
            });
    }

    private int getCurrentStateOrdinal(Object swipeCallback) {
        Object gestureStubView = getObjectField(swipeCallback, "this$0");
        Object arrowView = getObjectField(gestureStubView, "mGestureBackArrowView");
        Object currentState = callMethod(arrowView, "getCurrentState");
        return (int) callMethod(currentState, "ordinal");
    }

    private int mapOrdinalToState(int ordinal) {
        if (sReadyStateValues != null) {
            return sReadyStateValues[ordinal];
        }

        // Fallback:从 switch map 获取
        Class<?> switchMapClass = findSwitchMapClass();
        if (switchMapClass != null) {
            int[] switchMap = getStaticField(switchMapClass,
                "$SwitchMap$com$miui$home$recents$GestureBackArrowView$ReadyState");
            return switchMap[ordinal];
        }
        return STATE_NONE;
    }

    private Class<?> findSwitchMapClass() {
        try {
            return findClass("com.miui.home.recents.GestureStubView$4");
        } catch (Throwable e) {
            try {
                return findClass("com.miui.home.recents.GestureStubView$5");
            } catch (Throwable e2) {
                return null;
            }
        }
    }

    private void hookGetNextTask() {
        findAndHookMethod("com.miui.home.recents.GestureStubView",
            "getNextTask", Context.class, boolean.class, int.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    boolean shouldStart = (boolean) param.getArgs()[1];
                    int gestureStubPos = (int) param.getArgs()[2];

                    Object task = findNextTask(context);
                    if (task == null) {
                        param.setResult(null);
                        return;
                    }

                    loadTaskIconIfNeeded(context, task);

                    if (!shouldStart) {
                        param.setResult(task);
                        return;
                    }

                    startTaskFromRecents(context, task, gestureStubPos);
                    param.setResult(task);
                }
            });
    }

    private Object findNextTask(Context context) {
        Object recentsModel = callStaticMethod(
            findClass("com.miui.home.recents.RecentsModel"), "getInstance", context);
        Object taskLoader = callMethod(recentsModel, "getTaskLoader");
        Object loadPlan = callMethod(taskLoader, "createLoadPlan", context);
        callMethod(taskLoader, "preloadTasks", loadPlan, -1);
        Object taskStack = callMethod(loadPlan, "getTaskStack");

        if (taskStack == null || (int) callMethod(taskStack, "getTaskCount") == 0) {
            return null;
        }

        ActivityManager.RunningTaskInfo runningTask =
            (ActivityManager.RunningTaskInfo) callMethod(recentsModel, "getRunningTask");
        if (runningTask == null) {
            return null;
        }

        return getNextTaskFromStack(taskStack, runningTask);
    }

    private Object getNextTaskFromStack(Object taskStack, ActivityManager.RunningTaskInfo runningTask) {
        ArrayList<?> stackTasks = (ArrayList<?>) callMethod(taskStack, "getStackTasks");
        //查找当前任务的下一个任务
        for (int i = 0; i < stackTasks.size() - 1; i++) {
            Object task = stackTasks.get(i);
            int taskId = (int) getObjectField(getObjectField(task, "key"), "id");
            if (taskId == runningTask.id) {
                return stackTasks.get(i + 1);
            }
        }

        // 如果在桌面，返回第一个任务
        if (!stackTasks.isEmpty() &&
            "com.miui.home".equals(runningTask.baseActivity.getPackageName())) {
            return stackTasks.get(0);
        }

        return null;
    }

    private void loadTaskIconIfNeeded(Context context, Object task) {
        if (getObjectField(task, "icon") != null) return;

        Object recentsModel = callStaticMethod(
            findClass("com.miui.home.recents.RecentsModel"), "getInstance", context);
        Object taskLoader = callMethod(recentsModel, "getTaskLoader");

        Object icon = callMethod(taskLoader, "getAndUpdateActivityIcon",
            getObjectField(task, "key"),
            getObjectField(task, "taskDescription"),
            context.getResources(),
            true);
        setObjectField(task, "icon", icon);
    }

    private void startTaskFromRecents(Context context, Object task, int gestureStubPos) {
        ActivityOptions options = createActivityOptions(context, task, gestureStubPos);

        Object activityManager = callStaticMethod(
            findClass("android.app.ActivityManagerNative"), "getDefault");
        if (activityManager == null) return;

        int taskId = (int) getObjectField(getObjectField(task, "key"), "id");
        callMethod(activityManager, "startActivityFromRecents",
            taskId,
            options != null ? options.toBundle() : null);
    }

    private ActivityOptions createActivityOptions(Context context, Object task, int gestureStubPos) {
        ActivityOptions options = null;

        // 创建切换动画
        if (gestureStubPos == 0) {
            options = ActivityOptions.makeCustomAnimation(context,
                getAnimResId(context, "recents_quick_switch_left_enter"),
                getAnimResId(context, "recents_quick_switch_left_exit"));
        } else if (gestureStubPos == 1) {
            options = ActivityOptions.makeCustomAnimation(context,
                getAnimResId(context, "recents_quick_switch_right_enter"),
                getAnimResId(context, "recents_quick_switch_right_exit"));
        }

        // 处理分屏模式
        int windowingMode = (int) getObjectField(getObjectField(task, "key"), "windowingMode");
        if (windowingMode == 3) {
            if (options == null) {
                options = ActivityOptions.makeBasic();
            }
            callMethod(options, "setLaunchWindowingMode", 4);
        }

        return options;
    }

    private static int getAnimResId(Context context, String animName) {
        Integer cached = sAnimResCache.get(animName);
        if (cached != null) {
            return cached;
        }

        int resId = context.getResources().getIdentifier(animName, "anim", context.getPackageName());
        sAnimResCache.put(animName, resId);
        return resId;
    }
}

