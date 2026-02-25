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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * @author 焕晨HChen
 */
public class SystemLockApp extends BaseHook {
    private int taskId;
    private boolean isObserver = false;
    boolean isLock = false;
    boolean needLockScreen = false;

    @Override
    public void init() {
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerService",
                "onSystemReady",
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        try {
                            Context context = (Context) getObjectField(param.getThisObject(), "mContext");
                            if (context == null) return;
                            if (!isObserver) {
                                ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                    @Override
                                    public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
                                        isLock = getLockApp(context) != -1;
                                        if (isLock) {
                                            taskId = getLockApp(context);
                                            callMethod(param.getThisObject(), "startSystemLockTaskMode", taskId);
                                            needLockScreen = getMyLockScreen(context) == 1;
                                        } else {
                                            new Handler(context.getMainLooper()).postDelayed(() -> callMethod(param.getThisObject(), "stopSystemLockTaskMode"),300);
                                        }
                                    }
                                };
                                context.getContentResolver().registerContentObserver(
                                        Settings.Global.getUriFor("key_lock_app"),
                                        false, contentObserver);
                                isObserver = true;
                            }
                        } catch (Throwable e) {
                            XposedLog.e(TAG, "E: " + e);
                        }
                    }
                }
        );

        findAndHookMethod("com.miui.server.input.util.ShortCutActionsUtils",
                "triggerHapticFeedback", boolean.class, String.class,
                String.class, boolean.class, String.class,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (!PrefsBridge.getBoolean("system_framework_guided_access_status"))
                            return; // 不知道为什么还是需要重启才生效
                        String shortcut = (String) param.getArgs()[1];
                        if ("imperceptible_press_power_key".equals(shortcut) || "long_press_power_key".equals(shortcut)) {
                            Context context = (Context) getObjectField(param.getThisObject(), "mContext");
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
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
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
                    new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
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
            XposedLog.e(TAG, getPackageName(), "getInt hyceiler_lock_app E: " + e);
        }
        return -1;
    }

    public int getMyLockScreen(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "exit_lock_app_screen");
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.e(TAG, getPackageName(), "getMyLockScreen E will set " + e);
        }
        return 0;
    }
}
