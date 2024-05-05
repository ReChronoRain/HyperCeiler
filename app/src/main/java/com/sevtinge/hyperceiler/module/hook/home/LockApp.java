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
package com.sevtinge.hyperceiler.module.hook.home;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.getIS_TABLET;

/**
 * @author 焕晨HChen
 */
public class LockApp extends BaseHook {
    boolean isListen = false;
    boolean isListen2 = false;
    boolean isLock = false;

    @Override
    public void init() throws NoSuchMethodException {
        if (getIS_TABLET()) {
            // 平板
            findAndHookConstructor("com.miui.home.recents.GestureStubView",
                Context.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        if (!isListen) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    isLock = getLockApp(context) != -1;
                                    if (isLock) {
                                        setGestureLine(context, 1);
                                    } else {
                                        setGestureLine(context, 0);
                                    }
                                }
                            };
                            context.getContentResolver().registerContentObserver(
                                Settings.Global.getUriFor("key_lock_app"),
                                false, contentObserver);
                            isListen = true;
                        }
                    }
                }
            );

            findAndHookMethod("com.miui.home.recents.GestureInputHelper",
                "onInputEvent", InputEvent.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isLock) param.setResult(null);
                    }
                }
            );

            findAndHookConstructor("com.miui.home.launcher.dock.DockControllerImpl",
                "com.miui.home.launcher.hotseats.HotSeats", "com.miui.home.launcher.Launcher",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        if (!isListen2) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    Object getMDockStateMachine = XposedHelpers.callMethod(param.thisObject, "getMDockStateMachine");
                                    Object getMDockWindowManager = XposedHelpers.callMethod(param.thisObject, "getMDockWindowManager");
                                    View mDockRootView = (View) XposedHelpers.getObjectField(getMDockWindowManager, "mDockRootView");
                                    if (context == null) {
                                        logE(TAG, "DockControllerImpl context must not null");
                                        return;
                                    }
                                    if (getLockApp(context) != -1) {
                                        XposedHelpers.callMethod(getMDockStateMachine, "notifyPinnedStateChanged", false);
                                        mDockRootView.setVisibility(View.GONE);
                                    } else {
                                        XposedHelpers.callMethod(getMDockStateMachine, "notifyPinnedStateChanged", true);
                                        mDockRootView.setVisibility(View.VISIBLE);
                                    }
                                }
                            };
                            context.getContentResolver().registerContentObserver(
                                Settings.Global.getUriFor("key_lock_app"),
                                false, contentObserver);
                            isListen2 = true;
                        }
                    }
                }
            );
        } else {
            // 手机
            findAndHookConstructor("com.miui.home.recents.NavStubView",
                Context.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        if (!isListen) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    isLock = getLockApp(context) != -1;
                                }
                            };
                            context.getContentResolver().registerContentObserver(
                                Settings.Global.getUriFor("key_lock_app"),
                                false, contentObserver);
                            isListen = true;
                        }
                    }
                }
            );

            findAndHookMethod("com.miui.home.recents.NavStubView",
                "onTouchEvent", MotionEvent.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isLock) param.setResult(false);
                    }
                }
            );
        }
    }

    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");
        } catch (Settings.SettingNotFoundException e) {
            logE("LockApp", "getInt hyceiler_lock_app will set E: " + e);
        }
        return -1;
    }

    public static void setGestureLine(Context context, int type) {
        Settings.Global.putInt(context.getContentResolver(), "hide_gesture_line", type);
    }
}
