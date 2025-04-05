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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HideStatusBarBeforeScreenshot extends BaseHook {
    public static final String collapsedStatusBar = "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment";

    @Override
    public void init() {
        try {
            findClass(collapsedStatusBar).getDeclaredMethod("initMiuiViewsOnViewCreated", View.class);
            findAndHookMethod(collapsedStatusBar,
                "initMiuiViewsOnViewCreated", View.class, new MethodHook() {

                    @Override
                    protected void after(MethodHookParam param) {
                        View view = (View) param.args[0];
                        // logE(TAG, "1: " + param.args[0]);
                        setCollapsedStatusBar(view);
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            try {
                findClass(collapsedStatusBar).getDeclaredMethod("onViewCreated", View.class, Bundle.class);
                findAndHookMethod(collapsedStatusBar,
                    "onViewCreated", View.class, Bundle.class,
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            View view = (View) param.args[0];
                            // logE(TAG, "2: " + param.args[0]);
                            setCollapsedStatusBar(view);
                        }
                    }
                );
            } catch (NoSuchMethodException f) {
                logE(TAG, "No such: " + collapsedStatusBar + " method initMiuiViewsOnViewCreated and onViewCreated");
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void setCollapsedStatusBar(View view) {
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("miui.intent.TAKE_SCREENSHOT".equals(intent.getAction())) {
                    boolean finished = intent.getBooleanExtra("IsFinished", true);
                    // logE(TAG, "1: " + finished);
                    view.setVisibility(finished ? View.VISIBLE : View.INVISIBLE);
                }
            }
        };
        view.getContext().registerReceiver(br, new IntentFilter("miui.intent.TAKE_SCREENSHOT"));
    }
}
