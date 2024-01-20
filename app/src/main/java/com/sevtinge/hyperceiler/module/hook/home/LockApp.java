package com.sevtinge.hyperceiler.module.hook.home;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.ToastHelper;

import de.robv.android.xposed.XposedHelpers;

public class LockApp extends BaseHook {
    public Context mContext;
    public static Handler mHandler = new LockAppHandler();
    public final static int WILL_LOCK_APP = 0;
    public final static int LOCK_APP = 1;
    public final static int UNLOCK_APP = 2;
    public final static int WILL_UNLOCK_APP = 3;
    public final static int UNKNOWN_ERROR = 4;
    public final static int RESTORE = 5;
    public int taskId = -1;

    public int count = 0;
    public int eCount = 0;


    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.recents.NavStubView",
            "onTouchEvent", MotionEvent.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    MotionEvent motionEvent = (MotionEvent) param.args[0];
                    mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                    int action = motionEvent.getActionMasked();
                    int lockId = getLockApp(mContext);
                    if (getSystemLockEnable(mContext)) {
                        setSystemLockApp(mContext);
                    }
                    if (action == 2) {
                        count = count + 1;
                        if (count > 10) {
                            remoAllMes();
                            count = 0;
                            return;
                        }
                    }
                    if (action == 0) {
                        ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) XposedHelpers.callMethod(
                            XposedHelpers.callStaticMethod(findClassIfExists("com.miui.home.recents.RecentsModel"), "getInstance",
                                mContext), "getRunningTaskContainHome");
                        taskId = runningTaskInfo.taskId;
                        remoAllMes();
                        if (lockId == -1) {
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(WILL_LOCK_APP), 1000);
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(LOCK_APP, taskId), 2000);
                        } else {
                            if (lockId == taskId) {
                                mHandler.sendMessageDelayed(mHandler.obtainMessage(WILL_UNLOCK_APP), 1000);
                                mHandler.sendMessageDelayed(mHandler.obtainMessage(UNLOCK_APP), 2000);
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
                        param.setResult(false);
                    }
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
            logE("LockApp", "getInt hyceiler_lock_app will set E: " + e);
            setLockApp(context, -1);
        }
        return -1;
    }

    public boolean getSystemLockEnable(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "lock_to_app_enabled") == 1;
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, "getSystemLock E will set " + e);
            Settings.System.putInt(context.getContentResolver(), "lock_to_app_enabled", 0);
        }
        return false;
    }

    public static void setLockApp(Context context, int id) {
        Settings.Global.putInt(context.getContentResolver(), "key_lock_app", id);
    }

    public static void setSystemLockApp(Context context) {
        Settings.System.putInt(context.getContentResolver(), "lock_to_app_enabled", 0);
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
                            mResHook.addResource("will_lock_app",
                                R.string.home_other_lock_app_will_lock)),
                        false);
                }
                case LOCK_APP -> {
                    int taskId = (int) msg.obj;
                    setLockApp(context, taskId);
                    ToastHelper.makeText(context,
                        context.getResources().getString(
                            mResHook.addResource("lock_app",
                                R.string.home_other_lock_app_lock)),
                        false);
                }
                case WILL_UNLOCK_APP -> {
                    ToastHelper.makeText(context,
                        context.getResources().getString(
                            mResHook.addResource("will_unlock_app",
                                R.string.home_other_lock_app_will_unlock)),
                        false);
                }
                case UNLOCK_APP -> {
                    setLockApp(context, -1);
                    ToastHelper.makeText(context,
                        context.getResources().getString(
                            mResHook.addResource("unlock_app",
                                R.string.home_other_lock_app_unlock)),
                        false);
                }
                case UNKNOWN_ERROR -> {
                    ToastHelper.makeText(context,
                        context.getResources().getString(
                            mResHook.addResource("lock_app_e",
                                R.string.home_other_lock_app_e)),
                        false);
                }
                case RESTORE -> {
                    setLockApp(context, -1);
                    ToastHelper.makeText(context,
                        context.getResources().getString(
                            mResHook.addResource("lock_app_r",
                                R.string.home_other_lock_app_r)),
                        false);
                }
            }
        }
    }
}
