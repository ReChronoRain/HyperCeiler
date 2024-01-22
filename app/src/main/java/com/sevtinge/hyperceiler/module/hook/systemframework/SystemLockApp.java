package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SystemLockApp extends BaseHook {
    private int taskId;
    private boolean isObserver = false;

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
                                    if (getLockApp(context) != -1) {
                                        taskId = getLockApp(context);
                                        XposedHelpers.callMethod(param.thisObject, "startSystemLockTaskMode", taskId);
                                    } else {
                                        XposedHelpers.callMethod(param.thisObject, "stopSystemLockTaskMode");
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

    }

    public int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");

        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, "getInt hyceiler_lock_app E: " + e);
        }
        return -1;
    }
}
