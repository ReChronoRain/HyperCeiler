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
package com.sevtinge.hyperceiler.module.hook.home;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class StickyFloatingWindowsForHome extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer", "onAttachedToWindow", new MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        try {
                            String pkgName = intent.getStringExtra("package");
                            if (pkgName != null) {
                                XposedHelpers.callMethod(param.thisObject, "dismissRecentsToLaunchTargetTaskOrHome", pkgName, true);
                            }
                        } catch (Throwable t) {
                            logW(TAG, StickyFloatingWindowsForHome.this.lpparam.packageName, t);
                        }
                    }
                }, new IntentFilter(ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen"));
            }
        });
    }
}
