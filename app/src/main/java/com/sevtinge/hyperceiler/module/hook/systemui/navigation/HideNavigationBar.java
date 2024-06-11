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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseTool;

public class HideNavigationBar extends BaseTool {
    boolean run = false;

    @Override
    public void doHook() {
        hcHook.findClass("nbc", "com.android.systemui.navigationbar.NavigationBarController")
                .getAnyMethod("createNavigationBar")
                .hook(new IAction() {
                    @Override
                    public void after(ParamTool param) {
                        if (param.size() >= 3) {
                            Display display = param.first();
                            int id = display.getDisplayId();
                            param.callMethod("removeNavigationBar", id);
                            Context mContext = param.getField("mContext");
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
                })
                .findClass("mdis", "com.android.systemui.statusbar.phone.MiuiDockIndicatorService")
                .getMethod("onNavigationModeChanged", int.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        param.setField("mNavMode", param.first());
                        if (param.getField("mNavigationBarView") != null) {
                            param.callMethod("setNavigationBarView", null);
                        } else {
                            param.callMethod("checkAndApplyNavigationMode");
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
