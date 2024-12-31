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
package com.sevtinge.hyperceiler.module.hook.systemui.navigation;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HideNavigationBar extends BaseHook {
    boolean run = false;

    @Override
    public void init() {
        hookAllMethods(isMoreAndroidVersion(35) ? "com.android.systemui.navigationbar.NavigationBarControllerImpl" : "com.android.systemui.navigationbar.NavigationBarController", "createNavigationBar", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Display display = (Display) param.args[0];
                int id = display.getDisplayId();
                callMethod(param.thisObject, "removeNavigationBar", id);
                Context mContext = (Context) getObjectField(param.thisObject, "mContext");
                ContentObserver(mContext);
                try {
                    int state = Settings.Global.getInt(mContext.getContentResolver(), "hide_gesture_line");
                    if (state == 1) {
                        Settings.Global.putInt(mContext.getContentResolver(), "hide_gesture_line", 0);
                    }
                } catch (Settings.SettingNotFoundException e) {
                    logW(TAG, lpparam.packageName, "Don't Have hide_gesture_line");
                }
            }
        });

        findAndHookMethod(isMoreAndroidVersion(35) ? "com.android.systemui.navigationbar.NavigationBarControllerImpl" : "com.android.systemui.statusbar.phone.MiuiDockIndicatorService", "onNavigationModeChanged", int.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "mNavMode", param.args[0]);
                        if (getObjectField(param.thisObject, "mNavigationBarView") != null) {
                            callMethod(param.thisObject, "setNavigationBarView");
                        } else {
                            callMethod(param.thisObject, "checkAndApplyNavigationMode");
                        }
                        param.setResult(null);
                    }
                });
    }

    /*防呆专用*/
    public void ContentObserver(Context context) {
        if (!run) {
            run = true;
            ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (selfChange) return;
                    if (Settings.Global.getUriFor("hide_gesture_line").toString().equals(uri.toString())) {
                        Settings.Global.putInt(context.getContentResolver(), "hide_gesture_line", 0);
                        Toast.makeText(context, R.string.system_ui_hide_navigation_bar_toast_2, Toast.LENGTH_SHORT).show();
                        logW(TAG, lpparam.packageName, "Please don't hide gesture lines!");
                    } else if (Settings.Global.getUriFor("force_fsg_nav_bar").toString().equals(uri.toString())) {
                        Settings.Global.putInt(context.getContentResolver(), "force_fsg_nav_bar", 1);
                        Toast.makeText(context, R.string.system_ui_hide_navigation_bar_toast, Toast.LENGTH_SHORT).show();
                        logW(TAG, lpparam.packageName, "Please don't switch classic navigation keys!");
                    }
                }
            };
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, contentObserver);
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("hide_gesture_line"), false, contentObserver);
        }
    }
}
