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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author 焕晨HChen
 */
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
