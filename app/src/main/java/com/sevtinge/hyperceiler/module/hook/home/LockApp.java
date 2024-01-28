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

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.InputEvent;
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author 焕晨HChen
 */
public class LockApp extends BaseHook {
    boolean isListen = false;
    boolean isListen2 = false;
    boolean isListen3 = false;
    boolean isLock = false;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.miui.home.recents.GestureStubView",
            Context.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context context = (Context) param.args[0];
                    AndroidLogUtils.LogI(TAG, "com.miui.home.recents.GestureStubView");
                    if (!isListen) {
                        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange) {
                                AndroidLogUtils.LogI(TAG, "change");
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
                    AndroidLogUtils.LogI(TAG, "touch Nav");
                    if (isLock) param.setResult(false);
                }
            }
        );

        if (isPad()) {
            findAndHookMethod("com.miui.home.recents.GestureInputHelper",
                "onInputEvent", InputEvent.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        // AndroidLogUtils.LogI(TAG, "onInputEvent");
                        if (isLock) param.setResult(null);
                    }
                }
            );

            findAndHookConstructor("com.miui.home.launcher.dock.DockControllerImpl",
                "com.miui.home.launcher.hotseats.HotSeats", "com.miui.home.launcher.Launcher",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        AndroidLogUtils.LogI(TAG, "com.miui.home.launcher.dock.DockControllerImpl");
                        if (!isListen2) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    // AndroidLogUtils.LogI(TAG, "change dock");
                                    Object getMDockStateMachine = XposedHelpers.callMethod(param.thisObject, "getMDockStateMachine");
                                    if (getLockApp(context) != -1) {
                                        XposedHelpers.callMethod(getMDockStateMachine, "notifyPinnedStateChanged", false);
                                        // XposedHelpers.callMethod(param.thisObject, "onPinnedStateChanged", true);
                                        // AndroidLogUtils.LogI(TAG, "onPinnedStateChanged remove: " + getMDockStateMachine);
                                    } else {
                                        XposedHelpers.callMethod(getMDockStateMachine, "notifyPinnedStateChanged", true);
                                        // AndroidLogUtils.LogI(TAG, "onPinnedStateChanged show: " + getMDockStateMachine);
                                    }
                                    // isLock = getLockApp(context) != -1;
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

            findAndHookMethod("com.miui.home.recents.GestureModeAssistant", "onTouchEvent", android.view.MotionEvent.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        if (isLock) {
                            param.setResult(false);
                            AndroidLogUtils.LogI(TAG, "onTouchEvent G");
                        }
                        AndroidLogUtils.LogI(TAG, "onTouchEvent M");
                    }
                }
            );
/*
            findAndHookConstructor("com.miui.home.recents.GestureInputHelper", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        AndroidLogUtils.LogI(TAG, "com.miui.home.recents.GestureInputHelper " + context);
                        if (!isListen3) {
                            AndroidLogUtils.LogI(TAG, "!isListen3");
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    AndroidLogUtils.LogI(TAG, "change 1");
                                    if (getLockApp(context) != -1) {
                                        XposedHelpers.callMethod(param.thisObject, "setHideGestureLine", true);
                                        AndroidLogUtils.LogI(TAG, "setHideGestureLine true");
                                        // XposedHelpers.callMethod(param.thisObject, "onPinnedStateChanged", true);
                                        // AndroidLogUtils.LogI(TAG, "onPinnedStateChanged remove: " + getMDockStateMachine);
                                    } else {
                                        XposedHelpers.callMethod(param.thisObject, "setHideGestureLine", false);
                                        AndroidLogUtils.LogI(TAG, "setHideGestureLine false");
                                        // AndroidLogUtils.LogI(TAG, "onPinnedStateChanged show: " + getMDockStateMachine);
                                    }
                                    // isLock = getLockApp(context) != -1;
                                }
                            };
                            context.getContentResolver().registerContentObserver(
                                Settings.Global.getUriFor("key_hide"),
                                false, contentObserver);
                            isListen3 = true;
                        }
                    }
                }
            );*/


        /*ClassDataList classData = DexKit.INSTANCE.getDexKitBridge().findClass(
            FindClass.create()
                .matcher(ClassMatcher.create()
                    .addMethod(MethodMatcher.create()
                        .usingStrings("onTouchEvent")
                        .returnType(void.class)
                    )
                )
        );
        Class<?> g = findClassIfExists("com.miui.home.recents.GestureMode");
        AndroidLogUtils.LogI(TAG, "class: " + Arrays.toString(classData.toArray()));
        for (ClassData classData1 : classData) {
            try {
                Class<?> clzz = classData1.getInstance(lpparam.classLoader);
                // Class<?> s = clzz.getSuperclass();

                // if (s == null) continue;
                // if (s.equals(g)) {
                Method method = clzz.getDeclaredMethod("onTouchEvent", MotionEvent.class);
                AndroidLogUtils.LogI(TAG, "hook c: " + clzz + " me: " + method);
                if (method == null) continue;
                hookMethod(method, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        AndroidLogUtils.LogI(TAG, "hook: " + method + " touch");
                        if (isLock) param.setResult(null);
                    }
                });
                // }
            } catch (ClassNotFoundException e) {
                logE(TAG, "class");
            }
        }*/
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
}
