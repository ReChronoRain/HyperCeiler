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
package com.sevtinge.hyperceiler.module.hook.systemui;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.ToastHelper;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author 焕晨HChen
 */
public class UiLockApp extends BaseHook {
    public Context mContext;
    public static Handler mHandler = new LockAppHandler();
    public final static int WILL_LOCK_APP = 0;
    public final static int LOCK_APP = 1;
    public final static int UNLOCK_APP = 2;
    public final static int WILL_UNLOCK_APP = 3;
    public final static int UNKNOWN_ERROR = 4;
    public final static int RESTORE = 5;
    boolean isListen = false;
    public int taskId = -1;

    public int count = 0;
    public int eCount = 0;
    boolean isLock = false;

    boolean isObserver = false;

    @Override
    public void init() throws NoSuchMethodException {
        hookAllConstructors("com.android.systemui.statusbar.phone.AutoHideController",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        if (!isListen) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    isLock = getLockApp(context) != -1;
                                    if (getLockApp(context) != -1) {
                                        try {
                                            XposedHelpers.callMethod(param.thisObject, "scheduleAutoHide");
                                        } catch (Throwable e) {

                                        }
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

        if (mPrefsMap.getBoolean("system_framework_guided_access_status")) {
            hookAllConstructors("com.android.systemui.statusbar.window.StatusBarWindowController",
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            try {
                                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                if (context == null) return;

                                View view = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindowView");

                                if (!isObserver) {
                                    ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                                        @Override
                                        public void onChange(boolean selfChange) {
                                            isLock = getLockApp(context) != -1;
                                            // logE(TAG, "hide SUCCESS");
                                            view.setVisibility(isLock ? View.GONE : View.VISIBLE);
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
        }

        findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView",
                "onTouchEvent", MotionEvent.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        MotionEvent motionEvent = (MotionEvent) param.args[0];
                        // logE(TAG, "mo: " + motionEvent.getActionMasked());
                        mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                        int action = motionEvent.getActionMasked();
                        int lockId = getLockApp(mContext);
                        setSystemLockApp(mContext);
                        setSystemLockScreen(mContext);
                        if (mPrefsMap.getBoolean("system_framework_guided_access_screen")) {
                            setMyLockScreen(mContext, 1);
                        } else {
                            setMyLockScreen(mContext, 0);
                        }
                        if (action == 2) { // 移动手指判定失效
                            count = count + 1;
                            if (count > 6) {
                                remoAllMes();
                                count = 0;
                                return;
                            }
                        }
                        if (action == 0) {
                            Class<?> ActivityManagerWrapper = findClassIfExists("com.android.systemui.shared.system.ActivityManagerWrapper");
                            ActivityManager.RunningTaskInfo runningTaskInfo;
                            if (ActivityManagerWrapper != null) {
                                try {
                                    ActivityManagerWrapper.getDeclaredMethod("getInstance");
                                    Object getInstance = XposedHelpers.callStaticMethod(
                                            ActivityManagerWrapper,
                                            "getInstance");
                                    runningTaskInfo = (ActivityManager.RunningTaskInfo) XposedHelpers.callMethod(
                                            getInstance, "getRunningTask");
                                } catch (NoSuchMethodException e) {
                                    Object sInstance = XposedHelpers.getStaticObjectField(ActivityManagerWrapper, "sInstance");
                                    runningTaskInfo = (ActivityManager.RunningTaskInfo) XposedHelpers.callMethod(
                                            sInstance, "getRunningTask");
                                }
                            } else {
                                logE(TAG, "ActivityManagerWrapper is null");
                                return;
                            }
                            if (runningTaskInfo == null) {
                                logE(TAG, "runningTaskInfo is null");
                                return;
                            }
                            // ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) XposedHelpers.callMethod(
                            //     XposedHelpers.callStaticMethod(findClassIfExists("com.miui.home.recents.RecentsModel"), "getInstance",
                            //         mContext), "getRunningTaskContainHome");
                            taskId = runningTaskInfo.taskId;
                            ComponentName topActivity = runningTaskInfo.topActivity;
                            String pkg = topActivity.getPackageName();
                            if ("com.miui.home".equals(pkg)) {
                                return;
                            }
                            // logE(TAG, "task id: " + taskId + " a: " + pkg);
                            remoAllMes();
                            if (lockId == -1) {
                                mHandler.sendMessageDelayed(mHandler.obtainMessage(WILL_LOCK_APP), 1000);
                                mHandler.sendMessageDelayed(mHandler.obtainMessage(LOCK_APP, taskId), 1500);
                                // XposedHelpers.callMethod(param.thisObject, "updateLayoutForCutout");
                            } else {
                                if (lockId == taskId) {
                                    mHandler.sendMessageDelayed(mHandler.obtainMessage(WILL_UNLOCK_APP), 1000);
                                    mHandler.sendMessageDelayed(mHandler.obtainMessage(UNLOCK_APP), 1500);
                                } else {
                                    if (lockId != -1) {
                                        if (eCount < 2) {
                                            mHandler.sendMessage(mHandler.obtainMessage(UNKNOWN_ERROR));
                                            eCount = eCount + 1;
                                        } else {
                                            mHandler.sendMessage(mHandler.obtainMessage(RESTORE));
                                            eCount = 0;
                                        }
                                    }
                                }
                            }
                        }
                        if (action == 1) {
                            remoAllMes();
                        }
                        if (getLockApp(mContext) == taskId && lockId != -1) {
                            param.setResult(true);
                        }
                    }
                }
        );

        safeFindAndHookMethod("com.android.wm.shell.miuimultiwinswitch.miuiwindowdecor.MiuiBaseWindowDecoration",
            "shouldHideCaption",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    isLock = getLockApp(context) != -1;
                    param.setResult(isLock);
                }
            }
        );

        safeFindAndHookMethod("com.android.systemui.shared.system.ActivityManagerWrapper",
                "isLockTaskKioskModeActive", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
        );

        safeFindAndHookMethod("com.android.systemui.shared.system.ActivityManagerWrapper",
                "isScreenPinningActive", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
        );

        Class<?> ScreenPinningNotify = findClassIfExists("com.android.systemui.navigationbar.ScreenPinningNotify");
        if (ScreenPinningNotify != null) {
            Method[] methods = ScreenPinningNotify.getDeclaredMethods();
            for (Method method : methods) {
                switch (method.getName()) {
                    case "showPinningStartToast", "showPinningExitToast", "showEscapeToast" -> {
                        if (method.getReturnType().equals(void.class)) hookToast(method);
                    }
                }
            }
        }
    }

    public void hookToast(Method method) {
        safeHookMethod(method,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
        );
    }

    public void remoAllMes() {
        mHandler.removeMessages(WILL_LOCK_APP);
        mHandler.removeMessages(LOCK_APP);
        mHandler.removeMessages(WILL_UNLOCK_APP);
        mHandler.removeMessages(UNLOCK_APP);
    }

    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");
        } catch (Settings.SettingNotFoundException e) {
            logE("LockApp", "getInt hyceiler_lock_app e: " + e);
            setLockApp(context, -1);
        }
        return -1;
    }

    public static void setLockApp(Context context, int id) {
        Settings.Global.putInt(context.getContentResolver(), "key_lock_app", id);
    }

    public static void setSystemLockApp(Context context) {
        Settings.System.putInt(context.getContentResolver(), "lock_to_app_enabled", 0);
    }

    public static void setSystemLockScreen(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "lock_to_app_exit_locked", 0);
    }

    public static void setMyLockScreen(Context context, int value) {
        Settings.Global.putInt(context.getContentResolver(), "exit_lock_app_screen", value);
    }

    /**
     * @noinspection deprecation
     */
    public static class LockAppHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Context context = findContext(FLAG_CURRENT_APP);
            if (context == null) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(msg.what), 500);
                return;
            }
            switch (msg.what) {
                case WILL_LOCK_APP -> {
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_will_lock),
                            false);
                }
                case LOCK_APP -> {
                    int taskId = (int) msg.obj;
                    setLockApp(context, taskId);
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_lock),
                            false);
                }
                case WILL_UNLOCK_APP -> {
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_will_unlock),
                            false);
                }
                case UNLOCK_APP -> {
                    setLockApp(context, -1);
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_unlock),
                            false);
                }
                case UNKNOWN_ERROR -> {
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_e),
                            false);
                }
                case RESTORE -> {
                    setLockApp(context, -1);
                    ToastHelper.makeText(context,
                            context.getResources().getString(
                                    R.string.system_framework_guided_access_r),
                            false);
                }
            }
        }
    }
}
