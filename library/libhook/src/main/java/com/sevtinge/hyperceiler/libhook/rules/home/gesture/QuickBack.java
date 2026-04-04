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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 启用桌面快捷返回功能
 *
 * @noinspection DataFlowIssue
 */
public class QuickBack extends BaseHook {

    private static final String CLASS_GESTURE_STUB_VIEW = "com.miui.home.recents.GestureStubView";
    private static final String CLASS_GESTURE_STUB_CALLBACK = "com.miui.home.recents.GestureStubView$3";
    private static final String CLASS_GESTURE_BACK_ARROW_VIEW = "com.miui.home.recents.GestureBackArrowView";
    private static final String CLASS_READY_STATE = "com.miui.home.recents.GestureBackArrowView$ReadyState";
    private static final String CLASS_RECENTS_MODEL = "com.miui.home.recents.RecentsModel";
    private static final String CLASS_ACTIVITY_MANAGER_WRAPPER = "com.android.systemui.shared.recents.system.ActivityManagerWrapper";
    private static final String CLASS_BACK_GESTURE_UTILS = "com.android.systemui.fsgesture.BackGestureUtils";

    private static final Map<String, Integer> sAnimResCache = new HashMap<>();
    private static int[] sReadyStateValues = null;
    private static final int STATE_BACK = 1;
    private static final int STATE_RECENT = 2;
    private static final int STATE_NONE = 3;

    private Class<?> mRecentsModelClass;
    private Class<?> mActivityManagerWrapperClass;
    private Method mStartTaskFromRecentsByIdMethod;
    private Method mStartTaskFromRecentsByKeyMethod;

    @Override
    public void init() {
        initReadyStateValues();
        hookDisableQuickSwitch();
        hookLoadRecentTaskIcon();
        hookOnSwipeStop();
    }

    private void initReadyStateValues() {
        try {
            Class<?> readyStateClass = findClass(CLASS_READY_STATE);
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
        findAndHookMethod(CLASS_GESTURE_STUB_VIEW, "isDisableQuickSwitch",
            returnConstant(false));
    }

    private void hookOnSwipeStop() {
        findAndHookMethod(CLASS_GESTURE_STUB_CALLBACK,
            "onSwipeStop", boolean.class, float.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    boolean isFinish = (boolean) param.getArgs()[0];
                    if (!isFinish) return;

                    int stateOrdinal = getCurrentStateOrdinal(param.getThisObject());
                    int state = mapOrdinalToState(stateOrdinal);

                    if (state == STATE_RECENT) {
                        try {
                            handleRecentSwipeStop(param);
                        } catch (Throwable e) {
                            XposedLog.e(TAG, getPackageName(), "handleRecentSwipeStop failed", e);
                        }
                    }
                }
            });
    }

    private void hookLoadRecentTaskIcon() {
        findAndHookMethod(CLASS_GESTURE_BACK_ARROW_VIEW, "loadRecentTaskIcon", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                try {
                    Object icon = loadRecentTaskIcon(param.getThisObject());
                    if (icon != null) {
                        param.setResult(icon);
                    }
                } catch (Throwable e) {
                    XposedLog.e(TAG, getPackageName(), "loadRecentTaskIcon hook failed", e);
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
        if (sReadyStateValues != null && ordinal >= 0 && ordinal < sReadyStateValues.length) {
            return sReadyStateValues[ordinal];
        }

        // Fallback:从 switch map 获取
        Class<?> switchMapClass = findSwitchMapClass();
        if (switchMapClass != null) {
            int[] switchMap = getStaticField(switchMapClass,
                "$SwitchMap$com$miui$home$recents$GestureBackArrowView$ReadyState");
            if (ordinal >= 0 && ordinal < switchMap.length) {
                return switchMap[ordinal];
            }
        }
        return STATE_NONE;
    }

    private Object findNextTask(Context context) {
        Object recentsModel = callStaticMethod(getRecentsModelClass(), "getInstance", context);
        ActivityManager.RunningTaskInfo runningTask = getRunningTaskForQuickBack(recentsModel);
        if (runningTask == null) {
            XposedLog.w(TAG, getPackageName(), "findNextTask: runningTask is null");
            return null;
        }

        int runningTaskId = getRunningTaskId(runningTask);
        Object loadPlan = callMethod(recentsModel, "getSmartRecentsTaskLoadPlan", context, runningTaskId);
        Object taskStack = loadPlan != null ? callMethod(loadPlan, "getTaskStack") : null;

        if (taskStack == null || (int) callMethod(taskStack, "getTaskCount") == 0) {
            XposedLog.w(TAG, getPackageName(), "findNextTask: taskStack is empty, runningTask=" + describeRunningTask(runningTask, runningTaskId));
            return null;
        }

        return getNextTaskFromStack(taskStack, runningTask, runningTaskId);
    }

    private ActivityManager.RunningTaskInfo getRunningTaskForQuickBack(Object recentsModel) {
        try {
            return (ActivityManager.RunningTaskInfo) callMethod(recentsModel, "getRunningTaskForGesture", true);
        } catch (Throwable ignored) {
            return (ActivityManager.RunningTaskInfo) callMethod(recentsModel, "getRunningTask");
        }
    }

    private Object getNextTaskFromStack(Object taskStack, ActivityManager.RunningTaskInfo runningTask, int runningTaskId) {
        ArrayList<?> stackTasks = (ArrayList<?>) callMethod(taskStack, "getStackTasks");
        if (stackTasks == null || stackTasks.isEmpty()) {
            return null;
        }

        Object runningTaskInStack = callMethod(taskStack, "findTaskWithId", runningTaskId);
        if (runningTaskInStack != null) {
            int runningTaskIndex = (int) callMethod(taskStack, "indexOfStackTask", runningTaskInStack);
            if (runningTaskIndex >= 0 && runningTaskIndex + 1 < stackTasks.size()) {
                return stackTasks.get(runningTaskIndex + 1);
            }
            XposedLog.w(TAG, getPackageName(), "getNextTaskFromStack: running task has no next task, runningTask=" + describeRunningTask(runningTask, runningTaskId) + ", stackSize=" + stackTasks.size());
        } else {
            XposedLog.w(TAG, getPackageName(), "getNextTaskFromStack: running task not found in stack, runningTask=" + describeRunningTask(runningTask, runningTaskId) + ", stackSize=" + stackTasks.size());
        }

        // 如果在桌面，返回第一个任务
        if (runningTask.baseActivity != null &&
            "com.miui.home".equals(runningTask.baseActivity.getPackageName())) {
            return stackTasks.get(0);
        }

        return null;
    }

    private void loadTaskIconIfNeeded(Context context, Object task) {
        if (getObjectField(task, "icon") != null) return;

        Object recentsModel = callStaticMethod(
            getRecentsModelClass(), "getInstance", context);
        Object taskLoader = callMethod(recentsModel, "getTaskLoader");

        Object icon = callMethod(taskLoader, "getAndUpdateActivityIcon",
            getObjectField(task, "key"),
            getObjectField(task, "taskDescription"),
            context.getResources(),
            true);
        setObjectField(task, "icon", icon);
    }

    private boolean startTaskFromRecents(Context context, Object task, int gestureStubPos) {
        ActivityOptions options = createActivityOptions(context, task, gestureStubPos);
        Object taskKey = getObjectField(task, "key");
        int taskId = (int) getObjectField(taskKey, "id");
        Object activityManagerWrapper = callStaticMethod(getActivityManagerWrapperClass(), "getInstance");
        if (activityManagerWrapper == null) {
            XposedLog.w(TAG, getPackageName(), "startTaskFromRecents: activityManagerWrapper is null, task=" + describeTask(task));
            return false;
        }

        try {
            Object started = getStartTaskFromRecentsByIdMethod(activityManagerWrapper.getClass())
                .invoke(activityManagerWrapper, taskId, options);
            return !(started instanceof Boolean) || (Boolean) started;
        } catch (Throwable e) {
            try {
                getStartTaskFromRecentsByKeyMethod(activityManagerWrapper.getClass(), taskKey.getClass())
                    .invoke(activityManagerWrapper, taskKey, options);
                return true;
            } catch (Throwable fallbackError) {
                XposedLog.e(TAG, getPackageName(), "startTaskFromRecents failed, task=" + describeTask(task), fallbackError);
                return false;
            }
        }
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

    private int getRunningTaskId(ActivityManager.RunningTaskInfo runningTask) {
        try {
            Object activityManagerWrapper = callStaticMethod(getActivityManagerWrapperClass(), "getInstance");
            if (activityManagerWrapper != null) {
                return (int) callMethod(activityManagerWrapper, "getTaskId", runningTask);
            }
        } catch (Throwable e) {
            XposedLog.w(TAG, getPackageName(), "getTaskId via ActivityManagerWrapper failed");
        }

        try {
            return (int) getObjectField(runningTask, "taskId");
        } catch (Throwable ignored) {
            return runningTask.id;
        }
    }

    private Class<?> getRecentsModelClass() {
        if (mRecentsModelClass == null) {
            mRecentsModelClass = findClass(CLASS_RECENTS_MODEL);
        }
        return mRecentsModelClass;
    }

    private Class<?> getActivityManagerWrapperClass() {
        if (mActivityManagerWrapperClass == null) {
            mActivityManagerWrapperClass = findClass(CLASS_ACTIVITY_MANAGER_WRAPPER);
        }
        return mActivityManagerWrapperClass;
    }

    private Method getStartTaskFromRecentsByIdMethod(Class<?> wrapperClass) throws NoSuchMethodException {
        if (mStartTaskFromRecentsByIdMethod == null) {
            mStartTaskFromRecentsByIdMethod = wrapperClass.getMethod("startActivityFromRecents", Integer.TYPE, ActivityOptions.class);
            mStartTaskFromRecentsByIdMethod.setAccessible(true);
        }
        return mStartTaskFromRecentsByIdMethod;
    }

    private Method getStartTaskFromRecentsByKeyMethod(Class<?> wrapperClass, Class<?> taskKeyClass) throws NoSuchMethodException {
        if (mStartTaskFromRecentsByKeyMethod == null) {
            mStartTaskFromRecentsByKeyMethod = wrapperClass.getMethod("startActivityFromRecents", taskKeyClass, ActivityOptions.class);
            mStartTaskFromRecentsByKeyMethod.setAccessible(true);
        }
        return mStartTaskFromRecentsByKeyMethod;
    }

    private String describeRunningTask(ActivityManager.RunningTaskInfo runningTask, int runningTaskId) {
        String packageName = runningTask.baseActivity != null ? runningTask.baseActivity.getPackageName() : "null";
        return "id=" + runningTaskId + ", pkg=" + packageName;
    }

    private String describeTask(Object task) {
        if (task == null) {
            return "null";
        }

        try {
            Object key = getObjectField(task, "key");
            int taskId = (int) getObjectField(key, "id");
            Intent baseIntent = (Intent) getObjectField(key, "baseIntent");
            ComponentName component = baseIntent != null ? baseIntent.getComponent() : null;
            String packageName = component != null ? component.getPackageName() : "null";
            return "id=" + taskId + ", pkg=" + packageName;
        } catch (Throwable e) {
            return String.valueOf(task);
        }
    }

    private void handleRecentSwipeStop(HookParam param) {
        Object swipeCallback = param.getThisObject();
        Object gestureStubView = getObjectField(swipeCallback, "this$0");
        Object arrowView = getObjectField(gestureStubView, "mGestureBackArrowView");
        Context context = (Context) getObjectField(gestureStubView, "mContext");
        int gestureStubPos = (int) getObjectField(gestureStubView, "mGestureStubPos");

        callMethod(gestureStubView, "onBackCancelled");

        if (isNextTaskSupported(gestureStubView)) {
            Object task = findNextTask(context);
            if (task != null && startTaskFromRecents(context, task, gestureStubPos)) {
                finishSwipeStop(gestureStubView, arrowView, (float) param.getArgs()[1]);
                param.setResult(null);
                return;
            }

            if (task == null) {
                XposedLog.w(TAG, getPackageName(), "onSwipeStop: next task is null");
            }
        }

        vibrateQuickBackFail(gestureStubView);
        finishSwipeStop(gestureStubView, arrowView, (float) param.getArgs()[1]);
        param.setResult(null);
    }

    private Object loadRecentTaskIcon(Object arrowView) {
        if (!isNextTaskSupportedFromArrowView(arrowView)) {
            return getObjectField(arrowView, "mNoneTaskIcon");
        }

        Context context = (Context) callMethod(arrowView, "getContext");
        Object task = findNextTask(context);
        if (task == null) {
            return getObjectField(arrowView, "mNoneTaskIcon");
        }

        loadTaskIconIfNeeded(context, task);

        Object icon = getObjectField(task, "icon");
        return icon != null ? icon : getObjectField(arrowView, "mNoneTaskIcon");
    }

    private boolean isNextTaskSupported(Object gestureStubView) {
        Object contentResolver = getObjectField(gestureStubView, "mContentResolver");
        return (boolean) callStaticMethod(findClass(CLASS_GESTURE_STUB_VIEW), "supportNextTask", contentResolver);
    }

    private boolean isNextTaskSupportedFromArrowView(Object arrowView) {
        Object contentResolver = getObjectField(arrowView, "mContentResolver");
        return (boolean) callStaticMethod(findClass(CLASS_GESTURE_STUB_VIEW), "supportNextTask", contentResolver);
    }

    private void vibrateQuickBackFail(Object gestureStubView) {
        Object vibrator = getObjectField(gestureStubView, "mVibrator");
        if (vibrator != null) {
            callMethod(vibrator, "vibrate", 100L);
        }
    }

    private void finishSwipeStop(Object gestureStubView, Object arrowView, float offset) {
        setObjectField(gestureStubView, "mIsGestureStarted", false);

        Object handler = getObjectField(gestureStubView, "mHandler");
        Object resetMessage = callMethod(handler, "obtainMessage", 258);
        callMethod(handler, "sendMessageDelayed", resetMessage, 500L);
        callMethod(handler, "removeMessages", 261);

        Object animatorListener = getObjectField(gestureStubView, "mAnimatorListener");
        Object backGestureUtils = getStaticObjectField(findClass(CLASS_BACK_GESTURE_UTILS), "INSTANCE");
        Object convertedOffset = callMethod(backGestureUtils, "convertOffset", offset);
        try {
            callMethod(arrowView, "onSwipeStop", convertedOffset, animatorListener);
        } catch (Throwable ignored) {
            callMethod(arrowView, "onActionUp", convertedOffset, animatorListener);
        }
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
}
