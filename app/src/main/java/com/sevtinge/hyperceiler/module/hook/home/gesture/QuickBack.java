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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.home.gesture;

import static com.hchen.hooktool.log.XposedLog.logE;
import static com.hchen.hooktool.log.XposedLog.logI;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 启用桌面快捷返回功能
 *
 * @noinspection DataFlowIssue
 */
public class QuickBack extends BaseHC {
    private static final HashMap<String, Integer> mResMap = new HashMap<>();
    private static int[] values = null;

    @Override
    protected void init() {
        try {
            Class<?> mReadyStateClass = findClass("com.miui.home.recents.GestureBackArrowView$ReadyState").get();
            Enum<?> READY_STATE_BACK = (Enum<?>) getStaticField(mReadyStateClass, "READY_STATE_BACK");
            Enum<?> READY_STATE_RECENT = (Enum<?>) getStaticField(mReadyStateClass, "READY_STATE_RECENT");
            Enum<?> READY_STATE_NONE = (Enum<?>) getStaticField(mReadyStateClass, "READY_STATE_NONE");
            values = new int[((Object[]) callStaticMethod(mReadyStateClass, "values")).length];
            values[READY_STATE_BACK.ordinal()] = 1;
            values[READY_STATE_RECENT.ordinal()] = 2;
            values[READY_STATE_NONE.ordinal()] = 3;
        } catch (Throwable e) {
            logE(TAG, e);
        }

        hookMethod("com.miui.home.recents.GestureStubView",
                "isDisableQuickSwitch",
                returnResult(false)
        );

        hookMethod("com.miui.home.recents.GestureStubView$3",
                "onSwipeStop",
                boolean.class, float.class, boolean.class,
                new IHook() {

                    @Override
                    public void before() {
                        boolean isFinish = (boolean) getArgs(0);
                        if (isFinish) {
                            Object mGestureStubView = getThisField("this$0");
                            Object mGestureBackArrowView = getField(mGestureStubView, "mGestureBackArrowView");
                            Object getCurrentState = callMethod(mGestureBackArrowView, "getCurrentState");

                            int ordinal = (int) callMethod(getCurrentState, "ordinal");
                            if (values == null) {
                                Class<?> mGestureStubView$X = existsClass("com.miui.home.recents.GestureStubView$4") ?
                                        findClass("com.miui.home.recents.GestureStubView$4").get()
                                        : findClass("com.miui.home.recents.GestureStubView$5").get();
                                int[] mState = (int[]) getStaticField(mGestureStubView$X, "$SwitchMap$com$miui$home$recents$GestureBackArrowView$ReadyState");
                                ordinal = mState[ordinal];

                            } else
                                ordinal = values[ordinal];

                            if (ordinal == 2) {
                                callMethod(mGestureStubView, "onBackCancelled");
                                logI(TAG, "call onBackCancelled");
                            }
                        }
                    }
                }
        );

        hookMethod("com.miui.home.recents.GestureStubView",
                "getNextTask",
                Context.class, boolean.class, int.class,
                new IHook() {
                    @Override
                    public void before() {
                        Context context = (Context) getArgs(0);
                        ActivityManager.RunningTaskInfo runningTask;
                        Object recentsModel = callStaticMethod("com.miui.home.recents.RecentsModel", "getInstance", context);
                        Object taskLoader = callMethod(recentsModel, "getTaskLoader");
                        Object createLoadPlan = callMethod(taskLoader, "createLoadPlan", context);
                        callMethod(taskLoader, "preloadTasks", createLoadPlan, -1);
                        Object taskStack = callMethod(createLoadPlan, "getTaskStack");

                        if (taskStack == null || (int) callMethod(taskStack, "getTaskCount") == 0 ||
                                (runningTask = (ActivityManager.RunningTaskInfo) callMethod(recentsModel, "getRunningTask")) == null) {
                            setResult(null); // 后台无其他任务
                            return;
                        }

                        ArrayList<?> stackTasks = (ArrayList<?>) callMethod(taskStack, "getStackTasks");
                        Object task = null;
                        for (int i = 0; i < stackTasks.size() - 1; i++) {
                            Object t = stackTasks.get(i);
                            if ((int) getField(getField(t, "key"), "id") == runningTask.id) {
                                task = stackTasks.get(i + 1);
                                break;
                            }
                        }
                        if (task == null && !stackTasks.isEmpty() && "com.miui.home".equals(runningTask.baseActivity.getPackageName())) {
                            task = stackTasks.get(0);
                        }

                        if (task != null && getField(task, "icon") == null) {
                            setField(task, "icon",
                                    callMethod(taskLoader, "getAndUpdateActivityIcon",
                                            getField(task, "key"),
                                            getField(task, "taskDescription"),
                                            context.getResources(),
                                            true
                                    )
                            );
                        }
                        if (!(boolean) getArgs(1) || task == null) {
                            setResult(task);
                            return;
                        }

                        ActivityOptions activityOptions = null;
                        int mGestureStubPos = (int) getArgs(2);
                        if (mGestureStubPos == 0) {
                            activityOptions = ActivityOptions.makeCustomAnimation(context,
                                    getAnimId(context, "recents_quick_switch_left_enter"),
                                    getAnimId(context, "recents_quick_switch_left_exit"));
                        } else if (mGestureStubPos == 1) {
                            activityOptions = ActivityOptions.makeCustomAnimation(context,
                                    getAnimId(context, "recents_quick_switch_right_enter"),
                                    getAnimId(context, "recents_quick_switch_right_exit"));
                        }
                        Object iActivityManager = callStaticMethod("android.app.ActivityManagerNative", "getDefault");
                        if (iActivityManager != null) {
                            if ((int) getField(getField(task, "key"), "windowingMode") == 3) {
                                if (activityOptions == null) {
                                    activityOptions = ActivityOptions.makeBasic();
                                }
                                callMethod(activityOptions, "setLaunchWindowingMode", 4);
                            }
                            callMethod(iActivityManager, "startActivityFromRecents",
                                    getField(getField(task, "key"), "id"),
                                    activityOptions == null ? null : activityOptions.toBundle());
                            setResult(task);
                            return;
                        }
                        setResult(task);
                    }
                }
        );
    }

    public static int getAnimId(Context context, String str) {
        if (mResMap.get(str) == null) {
            int id = context.getResources().getIdentifier(str, "anim", context.getPackageName());
            mResMap.put(str, id);
            return id;
        } else
            return mResMap.get(str);
    }
}
