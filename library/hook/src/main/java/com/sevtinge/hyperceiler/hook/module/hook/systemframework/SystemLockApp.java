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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author 焕晨HChen
 */
public class SystemLockApp extends BaseHook {
    private int taskId;
    private boolean isObserver = false;
    boolean isLock = false;
    boolean needLockScreen = false;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService",
                "onSystemReady",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            if (context == null) return;
                            if (!isObserver) {
                                ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                    @Override
                                    public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                        isLock = getLockApp(context) != -1;
                                        if (isLock) {
                                            taskId = getLockApp(context);
                                            XposedHelpers.callMethod(param.thisObject, "startSystemLockTaskMode", taskId);
                                            needLockScreen = getMyLockScreen(context) == 1;
                                        } else {
                                            new Handler(context.getMainLooper()).postDelayed(() -> XposedHelpers.callMethod(param.thisObject, "stopSystemLockTaskMode"),300);
                                        }
                                    }
                                };
                                context.getContentResolver().registerContentObserver(
                                        Settings.Global.getUriFor("key_lock_app"),
                                        false, contentObserver);
                                isObserver = true;
                            }
                        } catch (Throwable e) {
                            logE(TAG, "E: " + e);
                        }
                    }
                }
        );

        findAndHookMethod("com.miui.server.input.util.ShortCutActionsUtils",
                "triggerHapticFeedback", boolean.class, String.class,
                String.class, boolean.class, String.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (!mPrefsMap.getBoolean("system_framework_guided_access_status"))
                            return; // 不知道为什么还是需要重启才生效
                        String shortcut = (String) param.args[1];
                        if ("imperceptible_press_power_key".equals(shortcut) || "long_press_power_key".equals(shortcut)) {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            isLock = getLockApp(context) != -1;
                            if (isLock) {
                                setLockApp(context, -1);
                            }
                        }
                    }
                }
        );

        findAndHookMethod("com.android.server.wm.LockTaskController",
                "shouldLockKeyguard", int.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (needLockScreen) {
                            param.setResult(true);
                        } else {
                            param.setResult(false);
                        }
                    }
                }
        );

        if (isPad()) {
            findAndHookMethod("com.android.server.wm.MiuiCvwGestureController$GesturePointerEventListener",
                    "onPointerEvent", MotionEvent.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (isLock) {
                                param.setResult(null);
                            }
                        }
                    }
            );
        }
    }

    public static void setLockApp(Context context, int id) {
        Settings.Global.putInt(context.getContentResolver(), "key_lock_app", id);
    }

    public int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, "getInt hyceiler_lock_app E: " + e);
        }
        return -1;
    }

    public int getMyLockScreen(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "exit_lock_app_screen");
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, "getMyLockScreen E will set " + e);
        }
        return 0;
    }
}
