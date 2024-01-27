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
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

/**
 * @author 焕晨HChen
 */
public class LockApp extends BaseHook {
    boolean isListen = false;
    boolean isLock = false;

    @Override
    public void init() throws NoSuchMethodException {
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


    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");
        } catch (Settings.SettingNotFoundException e) {
            logE("LockApp", "getInt hyceiler_lock_app will set E: " + e);
        }
        return -1;
    }
}
