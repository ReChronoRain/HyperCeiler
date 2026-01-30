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
package com.sevtinge.hyperceiler.libhook.rules.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * @author 焕晨HChen
 */
public class LockApp extends BaseHook {

    private static final String SETTING_KEY_LOCK_APP = "key_lock_app";
    private static final String SETTING_HIDE_GESTURE_LINE = "hide_gesture_line";

    private boolean isLockObserverRegistered = false;
    private boolean isDockObserverRegistered = false;
    private boolean isAppLocked = false;

    @Override
    public void init() {
        if (isPad()) {
            hookPadGesture();
            hookPadDock();
        } else {
            hookPhoneNavigation();
        }
    }

    private void hookPadGesture() {
        findAndHookConstructor("com.miui.home.recents.GestureStubView",
            Context.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    registerLockObserver(context);
                }
            });

        findAndHookMethod("com.miui.home.recents.GestureInputHelper",
            "onInputEvent", InputEvent.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (isAppLocked) {
                        param.setResult(null);
                    }
                }
            });
    }

    private void hookPadDock() {
        findAndHookConstructor("com.miui.home.launcher.dock.DockControllerImpl",
            "com.miui.home.launcher.hotseats.HotSeats",
            "com.miui.home.launcher.Launcher",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Context context = (Context) getObjectField(param.getThisObject(), "mContext");
                    if (context == null) {
                        XposedLog.w(TAG, getPackageName(), "DockControllerImpl context is null");
                        return;
                    }
                    registerDockObserver(context, param.getThisObject());
                }
            });
    }

    private void hookPhoneNavigation() {
        findAndHookConstructor("com.miui.home.recents.NavStubView",
            Context.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    registerLockObserver(context);
                }
            });

        findAndHookMethod("com.miui.home.recents.NavStubView",
            "onTouchEvent", MotionEvent.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (isAppLocked) {
                        param.setResult(false);
                    }
                }
            });
    }

    private void registerLockObserver(Context context) {
        if (isLockObserverRegistered) return;

        ContentObserver observer = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateLockState(context);
            }
        };

        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY_LOCK_APP),
            false,
            observer
        );

        isLockObserverRegistered = true;updateLockState(context);
    }

    private void registerDockObserver(Context context, Object dockController) {
        if (isDockObserverRegistered) return;

        ContentObserver observer = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateDockVisibility(context, dockController);
            }
        };

        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY_LOCK_APP),
            false,
            observer
        );

        isDockObserverRegistered = true;
        updateDockVisibility(context, dockController);
    }

    private void updateLockState(Context context) {
        boolean wasLocked = isAppLocked;
        isAppLocked = isAppLocked(context);

        if (isPad() && wasLocked != isAppLocked) {
            setGestureLine(context, isAppLocked ? 1 : 0);
        }
    }

    private void updateDockVisibility(Context context, Object dockController) {
        try {
            Object stateMachine = callMethod(dockController, "getMDockStateMachine");
            Object windowManager = callMethod(dockController, "getMDockWindowManager");
            View dockRootView = (View) getObjectField(windowManager, "mDockRootView");

            boolean locked = isAppLocked(context);
            callMethod(stateMachine, "notifyPinnedStateChanged", !locked);
            dockRootView.setVisibility(locked ? View.GONE : View.VISIBLE);
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "updateDockVisibility failed", t);
        }
    }

    private static boolean isAppLocked(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP) != -1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private static void setGestureLine(Context context, int type) {
        try {
            Settings.Global.putInt(context.getContentResolver(), SETTING_HIDE_GESTURE_LINE, type);
        } catch (Throwable t) {
            XposedLog.e("LockApp", "system", "setGestureLine failed", t);
        }
    }
}

