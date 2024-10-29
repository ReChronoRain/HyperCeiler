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
package com.sevtinge.hyperceiler.module.hook.systemui.navigation;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;
import com.sevtinge.hyperceiler.R;

public class HideNavigationBar extends BaseHC {
    boolean run = false;

    @Override
    public void init() {
        hookAll("com.android.systemui.navigationbar.NavigationBarController", "createNavigationBar", new IAction() {
            @Override
            public void after() throws Throwable {
                if (size() >= 3) {
                    Display display = first();
                    int id = display.getDisplayId();
                    callThisMethod("removeNavigationBar", id);
                    Context mContext = getThisField("mContext");
                    ContentObserver(mContext);
                    try {
                        int state = Settings.Global.getInt(mContext.getContentResolver(), "hide_gesture_line");
                        if (state == 1) {
                            Settings.Global.putInt(mContext.getContentResolver(), "hide_gesture_line", 0);
                        }
                    } catch (Settings.SettingNotFoundException e) {
                        logW(TAG, lpparam.packageName, "Don‘t Have hide_gesture_line");
                    }
                }
            }
        });

        hook("com.android.systemui.statusbar.phone.MiuiDockIndicatorService", "onNavigationModeChanged", int.class,
                new IAction() {
                    @Override
                    public void before() throws Throwable {
                        setThisField("mNavMode", first());
                        if (getThisField("mNavigationBarView") != null) {
                            callThisMethod("setNavigationBarView");
                        } else {
                            callThisMethod("checkAndApplyNavigationMode");
                        }
                        returnNull();
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
